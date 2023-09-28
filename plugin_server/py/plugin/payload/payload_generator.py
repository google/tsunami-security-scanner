"""Payload generator create custom payload for Tsunami detectors."""

import re
from typing import Any, Callable, Optional

from tsunami.plugin_server.py.plugin.payload.payload import Payload
from tsunami.plugin_server.py.plugin.payload.payload_secret_generator import PayloadSecretGenerator
from tsunami.plugin_server.py.plugin.payload.validator import Validator
from tsunami.plugin_server.py.plugin.tcs_client import TcsClient
from tsunami.proto import payload_generator_pb2 as pg


class PayloadGenerator:
  """Select a payload with the given payload generator config."""

  SECRET_LENGTH = 8
  TOKEN_CALLBACK_SERVER_URL = '$TSUNAMI_PAYLOAD_TOKEN_URL'
  TOKEN_RANDOM_STRING = '$TSUNAMI_PAYLOAD_TOKEN_RANDOM'

  def __init__(
      self,
      payload_secret_generator: PayloadSecretGenerator,
      payloads: list[pg.PayloadDefinition],
      tcs_client: TcsClient,
  ):
    """Initialize Payload Generator.

    Args:
      payload_secret_generator: A secret generator used to create a one-time
        string for payload use.
      payloads: A list of pre-generated payload definitions for selection.
      tcs_client: UI for interacting with the Tsunami Callback Server.
    """
    self.payload_secret_generator = payload_secret_generator
    self.payloads = payloads
    self.tcs_client = tcs_client

  def is_callback_server_enabled(self):
    """Check if callback server is enabled.

    Returns:
      Whether the callback server is enabled.
    """
    return self.tcs_client.is_callback_server_enabled()

  def generate(self, config: pg.PayloadGeneratorConfig) -> Payload:
    """Find a matching payload that uses callback server.

    The algorithm prioritizes the matching payload with callback server enabled
    and falls back to any other matching payload.

    Args:
      config: Payload generator config detailing the attributes required for the
        selected payload.

    Returns:
      A matching payload per the payload generator config.
    """
    return self._generate_payload(config, True)

  def generate_no_callback(self, config: pg.PayloadGeneratorConfig) -> Payload:
    """Find a matching payload that does not uses callback server.

    Args:
      config: Payload generator config detailing the attributes required for the
        selected payload.

    Returns:
      A matching payload per the payload generator config.
    """
    return self._generate_payload(config, False)

  def _generate_payload(
      self, config: pg.PayloadGeneratorConfig, use_callback: bool
  ) -> Payload:
    """Find matching payload per the provided attributes."""
    payload = None
    if self.tcs_client.is_callback_server_enabled() and use_callback:
      payload = self._find_matching_payload(config, use_callback)
    if not payload:
      payload = self._find_matching_payload(config, False)
    if not payload:
      raise LookupError(
          'No payload implemented for %s vulnerability type, %s interpretation'
          ' environment, and %s execution environment.'
          % (
              pg.PayloadGeneratorConfig.VulnerabilityType.Name(
                  config.vulnerability_type
              ),
              pg.PayloadGeneratorConfig.InterpretationEnvironment.Name(
                  config.interpretation_environment
              ),
              pg.PayloadGeneratorConfig.ExecutionEnvironment.Name(
                  config.execution_environment
              ),
          )
      )
    return payload

  def _find_matching_payload(
      self, config: pg.PayloadGeneratorConfig, use_callback: bool
  ) -> Optional[Payload]:
    for payload in self.payloads:
      if (self._payload_matches_config(payload, config, use_callback)):
        return self._parse_payload(payload, config)

  def _parse_payload(
      self, payload: pg.PayloadDefinition, config: pg.PayloadGeneratorConfig
  ) -> Payload:
    """Create payload from the selected payload definition."""
    secret = self.payload_secret_generator.generate(self.SECRET_LENGTH)
    if bool(payload.uses_callback_server.ByteSize()):
      payload_string = payload.payload_string.value.replace(
          self.TOKEN_CALLBACK_SERVER_URL,
          self.tcs_client.get_callback_uri(secret),
      )
      validator = type(
          'PayloadValidator',
          (Validator,),
          {'is_executed': lambda s, _: self.tcs_client.has_oob_log(secret)},
      )()
      return Payload(
          payload_string,
          validator,
          pg.PayloadAttributes(uses_callback_server=True),
          config,
      )
    else:
      payload_string = payload.payload_string.value.replace(
          self.TOKEN_RANDOM_STRING, secret
      )
      if payload.validation_type != pg.PayloadValidationType.Value(
          'VALIDATION_REGEX'
      ):
        raise NotImplementedError(
            'Validation type %s not supported.'
            % pg.PayloadGeneratorConfig.VulnerabilityType.Name(
                config.vulnerability_type)
            )
      regex = payload.validation_regex.value.replace(
          self.TOKEN_RANDOM_STRING, secret
      )
      validator = type(
          'PayloadValidator',
          (Validator,),
          {'is_executed': _is_executed(regex)},
      )()
      return Payload(
          payload_string,
          validator,
          pg.PayloadAttributes(uses_callback_server=False),
          config,
      )

  def _payload_matches_config(
      self,
      payload: pg.PayloadDefinition,
      config: pg.PayloadGeneratorConfig,
      use_callback: bool,
  ) -> bool:
    return (
        config.vulnerability_type in payload.vulnerability_type
        and config.interpretation_environment
        == payload.interpretation_environment
        and config.execution_environment == payload.execution_environment
        and bool(payload.uses_callback_server.ByteSize()) == use_callback
    )


def _is_executed(regex: str) -> Callable[[Any, Optional[bytes]], bool]:
  """Check if the returned payload is executed by validating against the regex."""

  def check_payload_execution(_, data: Optional[bytes]) -> bool:
    if data is None:
      raise ValueError('No valid payload input is entered.')
    string = data.decode('utf-8')
    return bool(re.compile(regex).search(string)) or False

  return check_payload_execution
