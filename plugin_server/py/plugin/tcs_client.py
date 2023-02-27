"""Tsunami Callback Server client."""
import hashlib
from typing import Optional
from absl import logging
from net.proto2.python.public import json_format
from tsunami.plugin_server.py.common.data import network_endpoint_utils
from tsunami.plugin_server.py.common.net.http.http_client import HttpClient
from tsunami.plugin_server.py.common.net.http.http_headers import HttpHeaders
from tsunami.plugin_server.py.common.net.http.http_request import HttpRequest
from tsunami.proto import network_pb2
from tsunami_callbackserver.proto import polling_pb2


class TcsClient:
  """Client use for communicating with Tsunami Callback Server."""

  def __init__(
      self,
      callback_address: str,
      callback_port: int,
      polling_base_url: str,
      http_client: HttpClient,
  ):
    """Initialize Tsunami Callback Server client.

    Args:
      callback_address: IP address or the hostname of the callback server.
      callback_port: The port that the callback server is running on.
      polling_base_url: Base url of the callback server.
      http_client: Client used to send HTTP requests and process responses.
    """
    self.callback_endpoint = self._create_callback_address(
        callback_address, callback_port
    )
    self.polling_base_url = self._remove_trailing_slashes(polling_base_url)
    self.http_client = http_client

  def is_callback_server_enabled(self) -> bool:
    """Check if callback server is enabled.

    Returns:
      Whether callback server is reachable.
    """
    return bool(
        self.callback_endpoint.ip_address.address
        or self.callback_endpoint.hostname.name
    ) and bool(self.polling_base_url)

  def get_callback_uri(self, secret_string: str) -> str:
    """Assemble the URI to reach the callback server.

    Args:
      secret_string: Callback unique ID that is bonded to the scan target. Used
        to include in the request URI.

    Returns:
      The server URI.

      Examples with provided hostname:
        04041e8898e739ca33.google.com
        04041e8898e739ca33.google.com:8080

      Examples with provided IP address:
        http://127.0.0.1:8080/04041e8898e739ca33
        http://[2001:db8:3333:4444:5555:6666:7777:8888]/04041e8898e739ca33
    """
    # Generate hash from 8 bytes secret string using SHA-3 hashing
    cbid = hashlib.sha3_224(secret_string.encode('utf-8')).hexdigest()
    uri = network_endpoint_utils.to_uri_authority(self.callback_endpoint)
    # return uri with provided hostname
    if network_endpoint_utils.has_hostname(self.callback_endpoint):
      return '%s.%s' % (cbid, uri)
    # return uri with provided ip address
    return 'http://%s/%s' % (uri, cbid)

  def has_oob_log(self, secret_string: str) -> bool:
    """Check if callback server has received OOB log.

    Args:
      secret_string: Callback unique ID that is bonded to the scan target. Used
        to include in the request URI.

    Returns:
      If callback server has out-of-bounds log.
    """
    result = self._send_polling_request(secret_string)
    if result:
      return result.has_dns_interaction or result.has_http_interaction
    return False

  def _send_polling_request(
      self, secret_string: str
  ) -> Optional[polling_pb2.PollingResult]:
    """Send HTTP requests to the callback server.

    Args:
      secret_string: Callback unique ID that is bonded to the scan target. Used
        to include in the request URI.

    Returns:
      The polling results of whether or not the scan target has DNS or HTTP
      interaction. Returns None if polling request failed.
    """
    request = self._build_polling_request(secret_string)
    try:
      response = self.http_client.send(request)
      if response.status.is_success():
        return json_format.Parse(
            response.body_string(), polling_pb2.PollingResult()
        )
      else:
        logging.info('OOB server returned %s.', response.status.code)
    except (json_format.ParseError, ValueError):
      logging.exception('Polling request failed.')
    return None

  def _build_polling_request(self, secret_string: str) -> HttpRequest:
    url = '%s/?secret=%s' % (self.polling_base_url, secret_string)
    return (
        HttpRequest.get(url)
        .set_headers(
            HttpHeaders.builder()
            .add_header('Cache-Control', 'no-cache')
            .build()
        )
        .build()
    )

  def _create_callback_address(
      self, address: str, port: int
  ) -> network_pb2.NetworkEndpoint:
    try:
      return (
          network_endpoint_utils.for_ip(address)
          if port == 80
          else network_endpoint_utils.for_ip_and_port(address, port)
      )
    except ValueError:
      return (
          network_endpoint_utils.for_hostname(address)
          if port == 80
          else network_endpoint_utils.for_hostname_and_port(address, port)
      )

  def _remove_trailing_slashes(self, url: str) -> str:
    return url.strip('/')
