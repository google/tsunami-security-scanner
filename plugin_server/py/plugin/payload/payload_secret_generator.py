"""Payload Secret Generator class."""

import base64
import os


class PayloadSecretGenerator:
  """Payload Secret Generator creates a one-time secret."""

  def generate(self, size: int) -> str:
    """Generate a random secret string with n bytes.

    Args:
      size: number of bytes.

    Returns:
      The decoded string.
    """
    random_bytes = os.urandom(size)
    return base64.b16encode(random_bytes).decode()
