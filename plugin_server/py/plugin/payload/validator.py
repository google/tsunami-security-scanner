"""Interface type for the function to verify payload execution."""

import abc
from typing import Optional


class Validator(metaclass=abc.ABCMeta):
  """Type used to verify payload execution in the out-of-bound Payload."""

  @abc.abstractmethod
  def is_executed(self, data: Optional[bytes]) -> bool:
    """Checks whether the payload is executed.

    Args:
      data: Optional data in bytes representation.

    Returns:
      Whether the payload is executed.
    """
