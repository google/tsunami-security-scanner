"""HTTP client using requests."""

import asyncio
import concurrent.futures
import functools
from typing import Optional

from absl import logging
import requests

from common.data.network_service_utils import NetworkService
from common.net.http.host_resolver_http_adapter import HostResolverHttpAdapter
from common.net.http.http_client import Builder
from common.net.http.http_client import HttpClient
from common.net.http.http_header_fields import HttpHeaderFields
from common.net.http.http_headers import HttpHeaders
from common.net.http.http_request import HttpRequest
from common.net.http.http_response import HttpResponse
from common.net.http.http_status import HttpStatus


_DEFAULT_ALLOW_REDIRECT = True
_DEFAULT_POOL_CONNECTIONS = 5
_DEFAULT_POOL_MAXSIZE = 10
_DEFAULT_MAX_WORKERS = 64
_TIMEOUT_SEC = 10
_VERIFY_SSL = True
# Default ceiling on response body bytes a single HTTP call may buffer in memory. Targets
# probed by the scanner are by definition adversarial, so an unbounded read is a remote DoS
# primitive (CWE-770 / CWE-400). 100 MB comfortably accommodates any HTML/JSON payload a
# detector should ever need while bounding the worst-case allocation per call.
_DEFAULT_MAX_RESPONSE_BODY_BYTES = 100 * 1024 * 1024
_READ_CHUNK_BYTES = 8192


class RequestsHttpClient(HttpClient):
  """Requests HTTP client library used for communicating with remote servers.

  Attributes:
    allow_redirects: Optional boolean to determine whether requests may be
      redirected. True by default.
    log_id: the log id.
    max_workers: Maximum number of threads to execute concurrently.
    session: Requests session object to manage and persist settings across
      requests.
    timeout_sec: Optional float. How long to wait for the server to send
      data before giving up.
    verify_ssl: Optional boolean to verify SSL certification. True by default.
  """
  TSUNAMI_USER_AGENT = 'TsunamiSecurityScanner'

  def __init__(
      self,
      session: requests.Session,
      allow_redirects: Optional[bool],
      log_id: Optional[str],
      max_workers: Optional[int],
      timeout_sec: Optional[float],
      verify_ssl: Optional[bool],
      max_response_body_bytes: int = _DEFAULT_MAX_RESPONSE_BODY_BYTES,
  ):
    self.session = session
    self.allow_redirects = allow_redirects
    self.log_id = log_id
    self.max_workers = max_workers
    self.timeout_sec = timeout_sec
    self.verify_ssl = verify_ssl
    self.max_response_body_bytes = max_response_body_bytes

  def get_log_id(self) -> str:
    return self.log_id

  def send(self,
           http_request: HttpRequest,
           network_service: Optional[NetworkService] = None) -> HttpResponse:
    """Send the HTTP request using this client."""
    logging.info("%sSending HTTP '%s' request to '%s'.", self.log_id,
                 http_request.method, http_request.url)
    req = self._prepare_request(http_request)
    resp = self.session.send(
        request=req,
        ip=self._get_ip(network_service),
        verify=self.verify_ssl,
        timeout=self.timeout_sec,
        allow_redirects=self.allow_redirects,
        # stream=True keeps the body off the response object so we can drain it
        # ourselves with a hard byte ceiling. Without it, requests buffers the
        # entire body inside session.send() and a malicious chunked peer can OOM
        # the process before we ever return.
        stream=True,
    )
    return self._parse_response(resp)

  def send_async(
      self,
      http_request: HttpRequest,
      network_service: Optional[NetworkService] = None) -> HttpResponse:
    """Send the HTTP request asynchronously."""
    logging.info("%sSending HTTP '%s' request to '%s'.", self.log_id,
                 http_request.method, http_request.url)
    req = self._prepare_request(http_request)
    loop = asyncio.get_event_loop()
    future = asyncio.ensure_future(self._prepare_future(req, network_service))
    loop.run_until_complete(future)
    res = future.result()
    return self._parse_response(res)

  @classmethod
  def modify(cls):
    """Allows client code to modify the configurations of the HTTP client."""
    return RequestsHttpClientBuilder()

  def _build_response_headers(self, headers: dict[str, str]) -> HttpHeaders:
    headers_builder = HttpHeaders.builder()
    for field in headers:
      headers_builder.add_header(field, headers[field])
    return headers_builder.build()

  def _parse_response(self, res: requests.Response) -> HttpResponse:
    body = self._read_body_with_cap(res)
    response_header = self._build_response_headers(res.headers)
    status = HttpStatus.from_code(res.status_code)
    return (
        HttpResponse.builder()
        .set_url(res.url)
        .set_status(status)
        .set_headers(response_header)
        .set_response_body(body)
        .build()
    )

  def _read_body_with_cap(self, res: requests.Response) -> bytes:
    """Drain a streamed response body with a hard byte ceiling.

    Targets probed by Tsunami are adversarial by definition; a malicious peer
    can serve an unbounded chunked body and force the process OOM. This method
    reads in 8 KiB chunks and aborts the moment the cap is exceeded, closing
    the underlying connection so the peer cannot keep the socket alive.
    """
    advertised = res.headers.get('Content-Length')
    if advertised is not None:
      try:
        if int(advertised) > self.max_response_body_bytes:
          raise IOError(
              'Response body for %s advertised %s bytes, exceeds cap of %d'
              ' bytes.'
              % (res.url, advertised, self.max_response_body_bytes)
          )
      except ValueError:
        pass

    buf = bytearray()
    try:
      for chunk in res.iter_content(
          chunk_size=_READ_CHUNK_BYTES, decode_unicode=False
      ):
        if not chunk:
          continue
        buf.extend(chunk)
        if len(buf) > self.max_response_body_bytes:
          raise IOError(
              'Response body for %s exceeds cap of %d bytes (read so far: %d).'
              % (res.url, self.max_response_body_bytes, len(buf))
          )
    finally:
      res.close()
    return bytes(buf)

  async def _prepare_future(
      self,
      req: requests.PreparedRequest,
      network_service: Optional[NetworkService],
  ):
    """Prepare async request to include configuration."""
    loop = asyncio.get_event_loop()
    future = loop.run_in_executor(
        concurrent.futures.ThreadPoolExecutor(max_workers=self.max_workers),
        functools.partial(
            self.session.send,
            request=req,
            ip=self._get_ip(network_service),
            verify=self.verify_ssl,
            timeout=self.timeout_sec,
            allow_redirects=self.allow_redirects,
            stream=True,
        ),
    )
    return await future

  def _prepare_request(
      self, http_request: HttpRequest
  ) -> requests.PreparedRequest:
    """Prepare request to bypass Requests library's canonicalization.

    Client can accept requests with any URL paths such as paths with "../.." and
    special characters.

    Args:
      http_request: HTTP request to prep for.

    Returns:
      PreparedRequest: Request containing the exact bytes that is ready to be
        sent.
    """
    req = requests.Request(
        method=http_request.method,
        url=http_request.url,
        data=http_request.body,
        headers=self._serialize_request_headers(http_request.headers)
        )
    prepped = req.prepare()
    # URL reassignment to bypass URL canonicalization
    prepped.url = http_request.url
    return prepped

  def _serialize_request_headers(self, headers: HttpHeaders) -> dict[str, str]:
    """Put headers in a dictionary and add Tsunami user agent."""
    serialized_headers = {}
    for field, values in headers.raw_headers.items():
      serialized_headers[field] = ', '.join(values)
    serialized_headers[
        HttpHeaderFields.USER_AGENT.value] = self.TSUNAMI_USER_AGENT
    return serialized_headers

  def _get_ip(self, network_service: Optional[NetworkService]) -> Optional[str]:
    if not network_service:
      return None
    return network_service.network_endpoint.ip_address.address


class RequestsHttpClientBuilder(Builder):
  """Base builder for implementations of RequestsHttpClient."""

  def __init__(self):
    self.log_id = None
    # SSL certification verification.
    self.verify_ssl = _VERIFY_SSL
    # How long to wait for the server to send data before giving up.
    self.timeout_sec = _TIMEOUT_SEC
    # Whether requests may be redirected.
    self.allow_redirects = _DEFAULT_ALLOW_REDIRECT
    # Maximum number of threads to execute concurrently.
    self.max_workers = _DEFAULT_MAX_WORKERS
    # Number of urllib3 connection pools to cache.
    self.pool_connections = _DEFAULT_POOL_CONNECTIONS
    # Maximum number of connections to save in the pool.
    self.pool_maxsize = _DEFAULT_POOL_MAXSIZE
    # Maximum response body bytes the client will buffer in memory.
    self.max_response_body_bytes = _DEFAULT_MAX_RESPONSE_BODY_BYTES

  def set_allow_redirects(
      self, allow_redirects: bool
  ) -> 'RequestsHttpClientBuilder':
    self.allow_redirects = allow_redirects
    return self

  def set_log_id(self, log_id: str) -> 'RequestsHttpClientBuilder':
    self.log_id = log_id
    return self

  def set_max_workers(self, max_workers: int) -> 'RequestsHttpClientBuilder':
    self.max_workers = max_workers
    return self

  def set_pool_connections(
      self, pool_connections: int
  ) -> 'RequestsHttpClientBuilder':
    self.pool_connections = pool_connections
    return self

  def set_pool_maxsize(self, pool_maxsize: int) -> 'RequestsHttpClientBuilder':
    self.pool_maxsize = pool_maxsize
    return self

  def set_timeout_sec(self, timeout_sec: float) -> 'RequestsHttpClientBuilder':
    self.timeout_sec = timeout_sec
    return self

  def set_verify_ssl(self, verify_ssl: bool) -> 'RequestsHttpClientBuilder':
    self.verify_ssl = verify_ssl
    return self

  def set_max_response_body_bytes(
      self, max_response_body_bytes: int
  ) -> 'RequestsHttpClientBuilder':
    self.max_response_body_bytes = max_response_body_bytes
    return self

  def build(self) -> RequestsHttpClient:
    session = requests.Session()
    adapter = HostResolverHttpAdapter(
        pool_maxsize=self.pool_maxsize,
        pool_connections=self.pool_connections,
    )
    session.mount('http://', adapter)
    session.mount('https://', adapter)
    return RequestsHttpClient(
        session=session,
        allow_redirects=self.allow_redirects,
        log_id=self.log_id,
        max_workers=self.max_workers,
        timeout_sec=self.timeout_sec,
        verify_ssl=self.verify_ssl,
        max_response_body_bytes=self.max_response_body_bytes,
    )
