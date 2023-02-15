"""HTTP method for CRUD operations."""

import enum


class HttpMethod(str, enum.Enum):
  """HTTP request type.

  Attributes:
    string: HTTP method.
  """
  GET = "GET"
  HEAD = "HEAD"
  POST = "POST"
  PUT = "PUT"
  DELETE = "DELETE"

  def __init__(self, string):
    self.string = string
