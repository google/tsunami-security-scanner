"""Utility service for retrieving and parsing payload."""

from ruamel import yaml
from net.proto2.python.public import json_format
from pyglib import resources
from tsunami.proto import payload_generator_pb2 as pg


_PATH = 'google3/third_party/java_src/tsunami/plugin/src/main/resources/com/google/tsunami/plugin/payload/payload_definitions.yaml'


def get_parsed_payload() -> list[pg.PayloadDefinition]:
  """Get payload from payload_definitions.yaml.

  Returns:
   PayloadLibrary: List of payload definitions after validation.

  Raises:
    ValueError: If payload is missing a name, interpretation_environment,
      uses_callback_server, payload_string, or vulnerability_type. If the
      payload definition is invalid. Examples of invalidity include:
        - Payload that uses callback but the tsunami payload string is missing.
        - Payload that does not uses callback but has no specified validation
          type.
        - Payload that uses validation regex but does not specify the regex to
          be used.
  """
  payload_str = str(resources.GetResource(_PATH, 'r'))
  payload_dict = yaml.safe_load(payload_str)
  payload_library = json_format.ParseDict(payload_dict, pg.PayloadLibrary())
  return _validate_payloads([p for p in payload_library.payloads])


def _validate_payloads(
    payloads: list[pg.PayloadDefinition],
) -> list[pg.PayloadDefinition]:
  """Validate the pre-loaded payloads."""
  for payload in payloads:
    if not payload.HasField('name'):
      raise ValueError('Parse payload does not have a name.')
    if (
        payload.interpretation_environment
        is pg.PayloadGeneratorConfig.InterpretationEnvironment.INTERPRETATION_ENVIRONMENT_UNSPECIFIED
    ):
      raise ValueError(
          'Parse payload does not have an interpretation environment.'
      )
    if (
        payload.execution_environment
        is pg.PayloadGeneratorConfig.ExecutionEnvironment.EXECUTION_ENVIRONMENT_UNSPECIFIED
    ):
      raise ValueError('Parse payload does not have an execution environment.')
    if (
        pg.PayloadGeneratorConfig.VulnerabilityType.VULNERABILITY_TYPE_UNSPECIFIED
        in payload.vulnerability_type
    ):
      raise ValueError('Parse payload does not have a vulnerability type.')
    if not payload.HasField('payload_string'):
      raise ValueError('Parse payload does not have a payload string.')
    if bool(
        payload.uses_callback_server.ByteSize()
    ) and '$TSUNAMI_PAYLOAD_TOKEN_URL' not in str(payload.payload_string):
      raise ValueError(
          'Parse payload uses callback server but $TSUNAMI_PAYLOAD_TOKEN_URL'
          ' not found in payload string.'
      )
    if not bool(payload.uses_callback_server.ByteSize()):
      if (
          payload.validation_type
          is pg.PayloadValidationType.VALIDATION_TYPE_UNSPECIFIED
      ):
        raise ValueError(
            'Parse payload does not have a validation type and'
            ' does not use the callback server.'
        )
      if (
          payload.validation_type is pg.PayloadValidationType.VALIDATION_REGEX
          and not payload.HasField('validation_regex')
      ):
        raise ValueError(
            'Parse payload has no validation regex but uses'
            ' PayloadValidationType.REGEX.'
        )
  return payloads
