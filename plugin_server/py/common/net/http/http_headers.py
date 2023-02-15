"""HTTP headers utility."""

import collections
import re
from typing import Optional
from tsunami.plugin_server.py.common.net.http.http_header_fields import HttpHeaderFields


class HttpHeaders:
  """HTTP headers utility class.

  Please use Builder() to create instances of this class.

  Attributes:
    raw_headers: Collection of header fields and values. Each field could have
      multiple values.
  """

  def __init__(self):
    self.raw_headers = collections.defaultdict(list)

  def names(self) -> set[str]:
    """Get the list of unique header field names."""
    return set(self.raw_headers.keys())

  def get(self, name: str) -> Optional[str]:
    """Get the first value for a specified HTTP field name."""
    values = self.get_all(name)
    if not values:
      return None
    return values[0]

  def get_all(self, name: str) -> list[str]:
    """Get all values for a specified HTTP field name.

    Values are in the order they were added to the builder.

    Args:
      name: header name

    Returns:
      List of matched header values.
    """
    if name is None:
      raise ValueError('Name cannot be None.')
    values = self.raw_headers.get(name, [])
    if values:
      return values
    canonicalized_name = _canonicalize(name)
    return self.raw_headers.get(canonicalized_name, [])

  @classmethod
  def builder(cls) -> 'Builder':
    return Builder()


class Builder:
  """Builder class to create HTTP headers object.

  Attributes:
    http_headers: Collection of header field names and corresponding values.
  """
  # RFC 2616 section 4.2.
  # Allow printable graphic characters except for colon
  HEADER_NAME_MATCHER = '^([!-~])[^:]*$'
  # No control characters except for horizontal tab and space
  HEADER_VALUE_MATCHER = '^[^\x00-\x08\x0A-\x1f\x7f]*$'

  def __init__(self):
    self.http_headers = HttpHeaders()

  def build(self) -> HttpHeaders:
    return self.http_headers

  def add_header(self, name: str, value: str, canonicalize: bool = True):
    """Add HTTP header to headers object.

    Args:
      name: HTTP header field name
      value: HTTP header value
      canonicalize: Optional boolean to normalize header or not. Default is
        True.

    Returns:
      The builder object.

    Raises:
      ValueError: If name or value is None. If header name or value pair does
      not comply with standards.
    """
    if name is None:
      raise ValueError('Name cannot be None.')
    if value is None:
      raise ValueError('Value cannot be None.')
    if canonicalize:
      name = self._canonicalize_header_name(name, value)
    self.http_headers.raw_headers[name].append(value)
    return self

  def _canonicalize_header_name(self, name, value) -> str:
    if not self._is_legal_header_name(name):
      raise ValueError('Illegal header name %s.' % name)
    if not self._is_legal_header_value(value):
      raise ValueError('Illegal header value %s.' % value)
    return _canonicalize(name)

  def _is_legal_header_name(self, name: str) -> bool:
    return bool(re.fullmatch(self.HEADER_NAME_MATCHER, name))

  def _is_legal_header_value(self, value: str) -> bool:
    return bool(re.fullmatch(self.HEADER_VALUE_MATCHER, value))


def _canonicalize(header_name: str) -> str:
  """Normalize header field name.

  Args:
    header_name: An HTTP header field name.

  Returns:
    An HttpHeaderField value or the header_name in lowercase.
  """
  try:
    return HttpHeaderFields(header_name).value
  except ValueError:
    return HttpHeaderFields.get_from_lower(header_name) or header_name.lower()
