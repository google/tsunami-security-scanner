"""Custom HTTP Adapter to handle host-based routing support for load balancers."""

import socket
from typing import Optional
from urllib import parse

import requests

from tsunami.plugin_server.py.common.net.http.http_header_fields import HttpHeaderFields


class HostResolverHttpAdapter(requests.adapters.HTTPAdapter):
  """Custom HTTP adapter for proper hostname resolution.

  When load balancers are used, there is a chance that the hostname does not
  resolve to the IP address of the vulnerable application. When the hostname
  does not resolve to the given IP address, the IP address returned by NMAP is
  prioritized and used in the "netloc" portion of the URL (see
  parse.urlsplit()). This Adapter also adds the host header of the request
  package that would have been otherwise omitted by default.

  Attributes:
    pool_connections: Number of connection pools to cache.
    pool_max: Maximum number of connections to save in the pool.
  """

  def __init__(self, pool_connections: int, pool_maxsize: int):
    super().__init__(
        pool_connections=pool_connections, pool_maxsize=pool_maxsize
    )

  def _add_host_header(
      self, request: requests.PreparedRequest, hostname: str
  ) -> None:
    """Adds host:port as the host header."""
    request.headers[HttpHeaderFields.HOST.value] = hostname

  def _require_ipv6_brackets(self, ip: str) -> str:
    """Adds enclosing brackets if IPV6."""
    try:
      socket.inet_pton(socket.AF_INET6, ip)
      return "[%s]" % ip
    except OSError:
      return ip

  def _resolve(self,
               hostname: str,
               ip: Optional[str] = None,
               port: Optional[int] = None) -> Optional[str]:
    """Use the hostname if it resolves to the ip, else use the ip address.

    Args:
      hostname: Hostname of the target network. This could be the domain name or
        the IP address.
      ip: Optional IP address of target network.
      port: Optional port of target network.

    Returns:
      String of the resolved hostname.
    """
    if hostname == ip or not ip or ip in socket.getaddrinfo(hostname, port):
      return hostname
    return ip

  def send(
      self,
      request: requests.PreparedRequest,
      ip: Optional[str] = None,
      **kwargs
  ) -> requests.Response:
    result = parse.urlparse(request.url)
    self._add_host_header(request, result.netloc)
    # use local dns
    resolved_host = self._resolve(result.hostname, ip=ip, port=result.port)
    if resolved_host != result.hostname:
      resolved_host = self._require_ipv6_brackets(resolved_host)
      netloc = result.netloc.lower().replace(result.hostname, resolved_host)
      request.url = parse.urlunparse((
          result.scheme,
          netloc,
          result.path,
          result.params,
          result.query,
          result.fragment,
      ))
    return super().send(request, **kwargs)
