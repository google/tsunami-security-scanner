"""HTTP request utility."""

from typing import Optional

from tsunami.plugin_server.py.common.net.http.http_headers import Builder as HttpHeadersBuilder
from tsunami.plugin_server.py.common.net.http.http_headers import HttpHeaders
from tsunami.plugin_server.py.common.net.http.http_method import HttpMethod


def check_url_argument(func):
  def wrapper(cls, url):
    if not url:
      raise ValueError('Url cannot be None.')
    return func(cls, url)
  return wrapper


class HttpRequest:
  """HTTP request utility class.

  Please use Builder() to create instances of this class.

  Attributes:
    method: The HTTP request type.
    url: String address of the request.
    headers: The HTTP request headers in key/value pairs.
    body: The HTTP body could be empty per the request type. GET and
      HEAD request types must have empty request_body.
  """

  def __init__(self):
    self.method: HttpMethod = None
    self.url: Optional[str] = None
    self.headers: HttpHeaders = None
    self.body: Optional[bytes] = None

  @classmethod
  @check_url_argument
  def get(cls, url: str):
    return Builder().set_method(HttpMethod.GET).set_url(url)

  @classmethod
  @check_url_argument
  def head(cls, url: str):
    return Builder().set_method(HttpMethod.HEAD).set_url(url)

  @classmethod
  @check_url_argument
  def post(cls, url: str):
    return Builder().set_method(HttpMethod.POST).set_url(url)

  @classmethod
  @check_url_argument
  def put(cls, url: str):
    return Builder().set_method(HttpMethod.PUT).set_url(url)

  @classmethod
  @check_url_argument
  def delete(cls, url: str):
    return Builder().set_method(HttpMethod.DELETE).set_url(url)

  @classmethod
  def builder(cls):
    return Builder()


class Builder:
  """Builder class to create HTTP request object.

  Attributes:
    http_request: HTTP request object built.
  """

  def __init__(self):
    self.http_request = HttpRequest()

  def set_method(self, method: HttpMethod) -> 'Builder':
    """Set the HttpMethod type."""
    self.http_request.method = method
    return self

  def set_url(self, url: str) -> 'Builder':
    """Set the string address for the request."""
    self.http_request.url = url
    return self

  def set_headers(self, headers: HttpHeaders) -> 'Builder':
    """Set the HttpHeaders for the request."""
    self.http_request.headers = headers
    return self

  def set_request_body(self, request_body: Optional[bytes] = None) -> 'Builder':
    """Set the request body."""
    self.http_request.body = request_body
    return self

  def with_empty_headers(self) -> 'Builder':
    """Set an empty Http_headers for the request."""
    self.set_headers(HttpHeadersBuilder().build())
    return self

  def build(self) -> 'HttpRequest':
    if (
        self.http_request.method == HttpMethod.GET
        or self.http_request.method == HttpMethod.HEAD
    ):
      if self.http_request.body:
        raise ValueError(
            'A request body is not allowed for HTTP GET/HEAD request.')
    return self.http_request
