"""Base class for HTTP client."""
import abc

from typing import Optional
from tsunami.plugin_server.py.common.data.network_service_utils import NetworkService
from tsunami.plugin_server.py.common.net.http.http_request import HttpRequest
from tsunami.plugin_server.py.common.net.http.http_response import HttpResponse


class HttpClient(metaclass=abc.ABCMeta):
  """HTTP client library used for communicating with remote servers.

  Attributes:
    allow_redirects: Optional boolean to determine whether requests may be
      redirected. True by default.
    log_id: the log id.
    timeout_sec: Optional float. How long to wait for the server to send
      data before giving up.
    verify_ssl: Optional boolean to verify SSL certification. True by default.
  """
  TSUNAMI_USER_AGENT = 'TsunamiSecurityScanner'

  def __init__(self,
               log_id: Optional[str] = None,
               timeout_sec: Optional[float] = None,
               allow_redirects: Optional[bool] = True,
               verify_ssl: Optional[bool] = True):
    self.allow_redirects = allow_redirects
    self.log_id = log_id
    self.timeout_sec = timeout_sec
    self.verify_ssl = verify_ssl

  @abc.abstractmethod
  def get_log_id(self) -> str:
    pass

  @abc.abstractmethod
  def send(self,
           http_request: HttpRequest,
           network_service: Optional[NetworkService] = None) -> HttpResponse:
    """Send the HTTP request using this client."""
    pass

  @abc.abstractmethod
  def send_async(
      self,
      http_request: HttpRequest,
      network_service: Optional[NetworkService] = None) -> HttpResponse:
    """Send the HTTP request asynchronously."""
    pass

  @abc.abstractmethod
  def modify(self):
    """Allows client code to modify the configurations of the HTTP client."""
    pass


class Builder(metaclass=abc.ABCMeta):
  """Base builder for implementations of HttpClient."""

  @abc.abstractmethod
  def set_allow_redirects(self, allow_redirects: bool):
    pass

  @abc.abstractmethod
  def set_verify_ssl(self, verify_ssl: bool):
    pass

  @abc.abstractmethod
  def set_log_id(self, log_id: str):
    pass

  @abc.abstractmethod
  def set_timeout_sec(self, timeout_sec: float):
    pass

  @abc.abstractmethod
  def build(self):
    pass
