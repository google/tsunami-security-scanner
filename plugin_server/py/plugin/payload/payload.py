"""Payload is the type returned by the Payload Generator."""

from typing import Optional

from absl import logging

from tsunami.plugin_server.py.plugin.payload.validator import Validator
from tsunami.proto import payload_generator_pb2


class Payload:
  """Out-of-bound payload to be sent to the scan target."""

  def __init__(
      self,
      payload: str,
      validator: Validator,
      attributes: payload_generator_pb2.PayloadAttributes,
      config: payload_generator_pb2.PayloadGeneratorConfig,
  ):
    self.payload = payload
    self.validator = validator
    self.attributes = attributes
    self.config = config

  def get_payload(self) -> str:
    """Get the string representation of the payload.

    Returns:
      The payload string
    """
    logging.info(
        '%s generated payload `%s`, %s use the callback server',
        self.config,
        self.payload,
        'does' if self.attributes.uses_callback_server else 'does not',
    )
    return self.payload

  def check_if_executed(
      self, payload_data: Optional[str | bytes] = None
  ) -> bool:
    """Check if the payload was executed on the scan target.

    Args:
      payload_data: Optional string or bytees representation of the payload to
        be verified.

    Returns:
      Whether the payload was executed.
    """
    payload = (
        payload_data.encode('utf-8')
        if isinstance(payload_data, str)
        else payload_data
    )
    return self.validator.is_executed(payload)

  def get_payload_attributes(self) -> payload_generator_pb2.PayloadAttributes:
    """Get the payload attributes.

    Returns:
      Details of the payload.
    """
    return self.attributes
