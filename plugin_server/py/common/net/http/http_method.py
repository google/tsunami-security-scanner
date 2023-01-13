"""HTTP method for CRUD operations."""

import enum


class HttpMethod(enum.Enum):
  """HTTP request type.

  Attributes:
    string: HTTP method.
    to_string: Get the HTTP method string.
  """
  GET = "GET"
  HEAD = "HEAD"
  POST = "POST"
  PUT = "PUT"
  DELETE = "DELETE"

  def __init__(self, string):
    self.string = string

  def to_string(self):
    return self.string
