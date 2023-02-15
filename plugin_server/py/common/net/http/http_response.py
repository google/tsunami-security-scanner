"""HTTP response utility."""

import abc
import json
from typing import Any, Optional
from tsunami.plugin_server.py.common.net.http.http_headers import HttpHeaders
from tsunami.plugin_server.py.common.net.http.http_status import HttpStatus


class HttpResponse(metaclass=abc.ABCMeta):
  """HTTP request utility class.

  Please use Builder() to create instances of this class.

  Attributes:
    status: The HTTP response status.
    url: String address that produced this HTTP response.
    headers: The HTTP response headers in key/value pairs.
    body: The HTTP body data.
  """

  def __init__(self):
    self.status: HttpStatus = None
    self.url: Optional[str] = None
    self.headers: HttpHeaders = None
    self.body: Optional[bytes] = None

  def body_string(self) -> str:
    """Get the body data as a UTF-8 encoded string."""
    try:
      return self.body.decode('utf-8')
    except AttributeError:
      return ''

  def body_json(self) -> Optional[dict[str, Any]]:
    """Parse the response body as JSON. Returns null if parsing failed."""
    try:
      return json.loads(self.body_string())
    except ValueError:
      return None

  def json_field_has_value(self, field: str, value: str):
    """Check if JSON body of the response has field and value pair."""
    body = self.body_json()
    if body is not None:
      return body.get(field) == value
    return False

  @classmethod
  def builder(cls):
    return Builder()


class Builder:
  """Builder class to create HTTP request object.

  Attributes:
    http_response: HTTP request object built.
  """

  def __init__(self):
    self.http_response = HttpResponse()

  def set_status(self, status: HttpStatus):
    """Set the HttpStatus type."""
    self.http_response.status = status
    return self

  def set_headers(self, headers: HttpHeaders):
    """Set the HttpHeaders for the response."""
    self.http_response.headers = headers
    return self

  def set_response_body(self, response_body: Optional[bytes]):
    """Set the body for the response."""
    self.http_response.body = response_body
    return self

  def set_url(self, url: str):
    """Set URL response."""
    self.http_response.url = url
    return self

  def build(self):
    return self.http_response
