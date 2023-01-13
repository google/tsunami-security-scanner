"""Http Status Codes utility class."""

import aenum as enum


class HttpStatus(enum.MultiValueEnum):
  """HTTP Status Codes defined in RFC 2616, RFC 6585, RFC 4918 and RFC 7538.

  @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html"
      target="_top">http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html</a>
  @see <a href="http://tools.ietf.org/html/rfc6585"
      target="_top">http://tools.ietf.org/html/rfc6585</a>
  @see <a href="https://tools.ietf.org/html/rfc4918"
      target="_top">https://tools.ietf.org/html/rfc4918</a>
  @see <a href="https://tools.ietf.org/html/rfc7538"
      target="_top">https://tools.ietf.org/html/rfc7538</a>

  Attributes:
    from_code: Get the HttpStatus from status code.
    is_redirect: Check if it is a redirected status response.
    is_success: Check if it is a success status response.
    to_string: Get the status name.
  """
  # Default
  HTTP_STATUS_UNSPECIFIED = 0, "Status Unspecified"

  # Informational 1xx
  CONTINUE = 100, "Continue"
  SWITCHING_PROTOCOLS = 101, "Switching Protocols"

  # Successful 2xx
  OK = 200, "OK"
  CREATED = 201, "Created"
  ACCEPTED = 202, "Accepted"
  NON_AUTHORITATIVE_INFORMATION = 203, "Non-Authoritative Information"
  NO_CONTENT = 204, "No Content"
  RESET_CONTENT = 205, "Reset Content"
  PARTIAL_CONTENT = 206, "Partial Content"
  MULTI_STATUS = 207, "Multi-Status"

  # Redirection 3xx
  MULTIPLE_CHOICES = 300, "Multiple Choices"
  MOVED_PERMANENTLY = 301, "Moved Permanently"
  FOUND = 302, "Found"
  SEE_OTHER = 303, "See Other"
  NOT_MODIFIED = 304, "Not Modified"
  USE_PROXY = 305, "Use Proxy"
  TEMPORARY_REDIRECT = 307, "Temporary Redirect"
  PERMANENT_REDIRECT = 308, "Permanent Redirect"

# Client Error 4xx
  BAD_REQUEST = 400, "Bad Request"
  UNAUTHORIZED = 401, "Unauthorized"
  PAYMENT_REQUIRED = 402, "Payment Required"
  FORBIDDEN = 403, "Forbidden"
  NOT_FOUND = 404, "Not Found"
  METHOD_NOT_ALLOWED = 405, "Method Not Allowed"
  NOT_ACCEPTABLE = 406, "Not Acceptable"
  PROXY_AUTHENTICATION_REQUIRED = 407, "Proxy Authentication Required"
  REQUEST_TIMEOUT = 408, "Request Timeout"
  CONFLICT = 409, "Conflict"
  GONE = 410, "Gone"
  LENGTH_REQUIRED = 411, "Length Required"
  PRECONDITION_FAILED = 412, "Precondition Failed"
  REQUEST_ENTITY_TOO_LARGE = 413, "Request Entity Too Large"
  REQUEST_URI_TOO_LONG = 414, "Request URI Too Long"
  UNSUPPORTED_MEDIA_TYPE = 415, "Unsupported Media Type"
  REQUEST_RANGE_NOT_SATISFIABLE = 416, "Request Range Not Satisfiable"
  EXPECTATION_FAILED = 417, "Expectation Failed"
  UNPROCESSABLE_ENTITY = 422, "Unprocessable Entity"
  LOCKED = 423, "Locked"
  FAILED_DEPENDENCY = 424, "Failed Dependency"
  PRECONDITION_REQUIRED = 428, "Precondition Required"
  TOO_MANY_REQUESTS = 429, "Too Many Requests"
  REQUEST_HEADER_FIELDS_TOO_LARGE = 431, "Request Header Fields Too Large"

  # Server Error 5xx
  INTERNAL_SERVER_ERROR = 500, "Internal Server Error"
  NOT_IMPLEMENTED = 501, "Not Implemented"
  BAD_GATEWAY = 502, "Bad Gateway"
  SERVICE_UNAVAILABLE = 503, "Service Unavailable"
  GATEWAY_TIMEOUT = 504, "Gateway Timeout"
  HTTP_VERSION_NOT_SUPPORTED = 505, "HTTP Version Not Supported"
  INSUFFICIENT_STORAGE = 507, "Insufficient Storage"
  NETWORK_AUTHENTICATION_REQUIRED = 511, "Network Authentication Required"

  # IE returns 1223 for 'Operation Aborted' instead of 204 with the status text
  # 'Unknown' and an empty response headers. Known to occur on IE 6 on XP
  # through IE9 on Window 7.
  QUIRK_IE_NO_CONTENT = 1223, "Quirk IE No Content"

  @classmethod
  def from_code(cls, code: int):
    """Gets the HTTP status from the status code.

    Args:
      code: The HTTP status code.

    Returns:
      The matching HTTP status or HTTP_STATUS_UNSPECIFIED if no known status is
      found.
    """
    try:
      status = HttpStatus(code)
      return status
    except ValueError:
      return HttpStatus.HTTP_STATUS_UNSPECIFIED

  def __init__(self, code, message=""):
    self.code = code
    self.message = message

  def __str__(self) -> str:
    return self.message

  def is_redirect(self) -> bool:
    return bool(self in _REDIRECTED_HTTP_STATUS)

  def is_success(self) -> bool:
    return self.code >= 200 and self.code < 300


_REDIRECTED_HTTP_STATUS = [
    HttpStatus.MULTIPLE_CHOICES,
    HttpStatus.MOVED_PERMANENTLY,
    HttpStatus.FOUND,
    HttpStatus.SEE_OTHER,
    HttpStatus.TEMPORARY_REDIRECT,
    HttpStatus.PERMANENT_REDIRECT,
]
