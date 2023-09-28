"""Tests for google3.third_party.java_src.tsunami.plugin_server.py.plugin.payload.payload_generator."""

import hashlib
import unittest

from absl.testing import absltest
from absl.testing import parameterized
import requests_mock

from google.protobuf import wrappers_pb2
from tsunami.plugin_server.py.common.net.http.requests_http_client import RequestsHttpClientBuilder
from tsunami.plugin_server.py.plugin.payload.payload_generator import PayloadGenerator
from tsunami.plugin_server.py.plugin.payload.payload_generator_test_helper import ANY_SSRF_CONFIG
from tsunami.plugin_server.py.plugin.payload.payload_generator_test_helper import JAVA_REFLECTIVE_RCE_CONFIG
from tsunami.plugin_server.py.plugin.payload.payload_generator_test_helper import LINUX_REFLECTIVE_RCE_CONFIG
from tsunami.plugin_server.py.plugin.payload.payload_generator_test_helper import LINUX_UNSPECIFIED_CONFIG
from tsunami.plugin_server.py.plugin.payload.payload_secret_generator import PayloadSecretGenerator
from tsunami.plugin_server.py.plugin.payload.payload_utility import get_parsed_payload
from tsunami.plugin_server.py.plugin.tcs_client import TcsClient
from tsunami.proto import payload_generator_pb2 as pg


_IP_ADDRESS = '127.0.0.1'
_PORT = 8000
_URL = 'http://valid.com'
_SECRET = '_1234_HERE_IT_IS_'


class PayloadGeneratorWithCallbackTest(parameterized.TestCase):

  @classmethod
  def setUpClass(cls):
    super().setUpClass()
    psg = PayloadSecretGenerator()
    psg.generate = unittest.mock.MagicMock(return_value=_SECRET)
    client = TcsClient(
        _IP_ADDRESS, _PORT, _URL, RequestsHttpClientBuilder().build()
    )
    payloads = get_parsed_payload()
    cls.payload_generator = PayloadGenerator(psg, payloads, client)

  def test_is_callback_server_enabled_returns_true(self):
    self.assertTrue(self.payload_generator.is_callback_server_enabled())

  @parameterized.named_parameters(
      ('linux_config', LINUX_REFLECTIVE_RCE_CONFIG, 'curl'),
      (
          'ssrf_config',
          ANY_SSRF_CONFIG,
          hashlib.sha3_224(_SECRET.encode('utf-8')).hexdigest(),
      ),
  )
  def test_generate_with_callback_returns_payload(
      self, config, expected_payload
  ):
    payload = self.payload_generator.generate(config)
    self.assertIn(expected_payload, payload.payload)
    self.assertIn(_IP_ADDRESS, payload.payload)
    self.assertIn(str(_PORT), payload.payload)
    self.assertTrue(payload.get_payload_attributes().uses_callback_server)

  @parameterized.named_parameters(
      (
          'linux_config',
          LINUX_REFLECTIVE_RCE_CONFIG,
          (
              'printf %s%s%s'
              ' TSUNAMI_PAYLOAD_START {secret} TSUNAMI_PAYLOAD_END'
          ).format(secret=_SECRET),
      ),
      (
          'ssrf_config',
          ANY_SSRF_CONFIG,
          'http://public-firing-range.appspot.com/',
      ),
      (
          'java_config',
          JAVA_REFLECTIVE_RCE_CONFIG,
          (
              'String.format("%s%s%s", "TSUNAMI_PAYLOAD_START",'
              ' "{secret}", "TSUNAMI_PAYLOAD_END")'
          ).format(secret=_SECRET),
      ),
  )
  def test_generate_no_callback_returns_payload(
      self, config, expected_payload
  ):
    payload = self.payload_generator.generate_no_callback(config)
    self.assertIn(expected_payload, payload.payload)
    self.assertFalse(payload.get_payload_attributes().uses_callback_server)

  @requests_mock.mock()
  def test_check_if_executed_linux_and_payload_exec_returns_true(self, mock):
    body = '{ "has_dns_interaction":false, "has_http_interaction":true}'
    mock.register_uri('GET', _URL, content=body.encode('utf-8'))
    payload = self.payload_generator.generate(LINUX_REFLECTIVE_RCE_CONFIG)
    self.assertTrue(payload.check_if_executed())

  @requests_mock.mock()
  def test_check_if_executed_linux_and_payload_exec_with_dns_returns_true(
      self, mock):
    body = '{ "has_dns_interaction":true, "has_http_interaction":true}'
    mock.register_uri('GET', _URL, content=body.encode('utf-8'))
    payload = self.payload_generator.generate(LINUX_REFLECTIVE_RCE_CONFIG)
    self.assertTrue(payload.check_if_executed())

  @requests_mock.mock()
  def test_check_if_executed_linux_no_payload_exec_returns_false(self, mock):
    body = '{ "has_dns_interaction":false, "has_http_interaction":false}'
    mock.register_uri('GET', _URL, content=body.encode('utf-8'))
    payload = self.payload_generator.generate(LINUX_REFLECTIVE_RCE_CONFIG)
    self.assertFalse(payload.check_if_executed())

  @requests_mock.mock()
  def test_check_if_executed_ssrf_and_payload_exec_returns_true(self, mock):
    body = '{ "has_dns_interaction":true, "has_http_interaction":false}'
    mock.register_uri('GET', _URL, content=body.encode('utf-8'))
    payload = self.payload_generator.generate(ANY_SSRF_CONFIG)
    self.assertTrue(payload.check_if_executed())

  @requests_mock.mock()
  def test_check_if_executed_ssrf_and_no_payload_exec_returns_false(self, mock):
    body = '{ "has_dns_interaction":false, "has_http_interaction":false}'
    mock.register_uri('GET', _URL, content=body.encode('utf-8'))
    payload = self.payload_generator.generate(ANY_SSRF_CONFIG)
    self.assertFalse(payload.check_if_executed())

  def test_generate_with_no_vulnerability_type_raises_lookup_error(self):
    with self.assertRaises(LookupError) as exc:
      self.payload_generator.generate(
          pg.PayloadGeneratorConfig(
              interpretation_environment=pg.PayloadGeneratorConfig.InterpretationEnvironment.INTERPRETATION_ANY,
              execution_environment=pg.PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT,
          )
      )
    self.assertEqual(
        (
            'No payload implemented for VULNERABILITY_TYPE_UNSPECIFIED'
            ' vulnerability type, INTERPRETATION_ANY interpretation'
            ' environment, and EXEC_INTERPRETATION_ENVIRONMENT execution'
            ' environment.'
        ),
        str(exc.exception),
    )

  def test_generate_with_no_interpretation_environment_raises_error(self):
    with self.assertRaises(LookupError) as exc:
      self.payload_generator.generate(
          pg.PayloadGeneratorConfig(
              vulnerability_type=pg.PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE,
              execution_environment=pg.PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT,
          )
      )
    self.assertEqual(
        (
            'No payload implemented for REFLECTIVE_RCE vulnerability type,'
            ' INTERPRETATION_ENVIRONMENT_UNSPECIFIED interpretation'
            ' environment, and EXEC_INTERPRETATION_ENVIRONMENT execution'
            ' environment.'
        ),
        str(exc.exception),
    )

  def test_generate_with_no_execution_environment_raises_lookup_error(self):
    with self.assertRaises(LookupError) as exc:
      self.payload_generator.generate(
          pg.PayloadGeneratorConfig(
              interpretation_environment=pg.PayloadGeneratorConfig.InterpretationEnvironment.INTERPRETATION_ANY,
              vulnerability_type=pg.PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE,
          )
      )
    self.assertEqual(
        (
            'No payload implemented for REFLECTIVE_RCE vulnerability type,'
            ' INTERPRETATION_ANY interpretation environment, and'
            ' EXECUTION_ENVIRONMENT_UNSPECIFIED execution environment.'
        ),
        str(exc.exception),
    )

  def test_generate_with_no_config_raises_lookup_error(self):
    with self.assertRaises(LookupError) as exc:
      self.payload_generator.generate(pg.PayloadGeneratorConfig())
    self.assertEqual(
        (
            'No payload implemented for VULNERABILITY_TYPE_UNSPECIFIED'
            ' vulnerability type, INTERPRETATION_ENVIRONMENT_UNSPECIFIED'
            ' interpretation environment, and EXECUTION_ENVIRONMENT_UNSPECIFIED'
            ' execution environment.'
        ),
        str(exc.exception),
    )


class PayloadGeneratorWithoutCallbackTest(parameterized.TestCase):

  @classmethod
  def setUpClass(cls):
    super().setUpClass()
    psg = PayloadSecretGenerator()
    psg.generate = unittest.mock.MagicMock(return_value=_SECRET)
    payloads = get_parsed_payload()
    disabled_client = TcsClient('', 0, '', RequestsHttpClientBuilder().build())
    cls.payload_generator_without_callback_server = PayloadGenerator(
        psg, payloads, disabled_client
    )

  def test_is_callback_server_enabled_with_no_callback_returns_false(self):
    self.assertFalse(
        self.payload_generator_without_callback_server.is_callback_server_enabled()
    )

  @parameterized.named_parameters(
      (
          'linux_config',
          LINUX_REFLECTIVE_RCE_CONFIG,
          (
              'printf %s%s%s TSUNAMI_'
              'PAYLOAD_START {secret} TSUNAMI_PAYLOAD_END'
          ).format(secret=_SECRET),
      ),
      (
          'ssrf_config',
          ANY_SSRF_CONFIG,
          'http://public-firing-range.appspot.com/',
      ),
      (
          'java_config',
          JAVA_REFLECTIVE_RCE_CONFIG,
          (
              'String.format("%s%s%s", "TSUNAMI_PAYLOAD_START",'
              ' "{secret}", "TSUNAMI_PAYLOAD_END")'
          ).format(secret=_SECRET),
      ),
  )
  def test_generate_without_callback_returns_payload(
      self, config, expected_payload
  ):
    payload = self.payload_generator_without_callback_server.generate(config)
    self.assertEqual(expected_payload, payload.payload)
    self.assertFalse(payload.get_payload_attributes().uses_callback_server)

  @parameterized.named_parameters(
      (
          'linux_config',
          LINUX_REFLECTIVE_RCE_CONFIG,
          ('TSUNAMI_PAYLOAD_START{secret}TSUNAMI_PAYLOAD_END').format(
              secret=_SECRET
          ),
      ),
      ('ssrf_config', ANY_SSRF_CONFIG, '<h1>What is the Firing Range?</h1>'),
      (
          'java_config',
          JAVA_REFLECTIVE_RCE_CONFIG,
          ('TSUNAMI_PAYLOAD_START{secret}TSUNAMI_PAYLOAD_END').format(
              secret=_SECRET
          ),
      ),
  )
  def test_check_if_executed_with_correct_payload_input_returns_true(
      self, config, expected_payload
  ):
    payload = self.payload_generator_without_callback_server.generate(config)
    payload_byte = bytes(expected_payload, 'utf-8')
    self.assertTrue(payload.check_if_executed(payload_byte))

  @parameterized.named_parameters(
      ('linux_config', LINUX_REFLECTIVE_RCE_CONFIG, 'Random input'),
      ('ssrf_config', ANY_SSRF_CONFIG, '<h1>Not Firing Range</h1>'),
      (
          'java_config',
          JAVA_REFLECTIVE_RCE_CONFIG,
          'TSUNAMI_PAYLOAD_START_Nothing_here_TSUNAMI_PAYLOAD_END',
      ),
  )
  def test_check_if_executed_with_bad_payload_input_returns_false(
      self, config, expected_payload
  ):
    payload = self.payload_generator_without_callback_server.generate(config)
    payload_byte = bytes(expected_payload, 'utf-8')
    self.assertFalse(payload.check_if_executed(payload_byte))

  def test_generate_with_no_callback_no_vulnerability_type_raises_error(self):
    with self.assertRaises(LookupError) as exc:
      self.payload_generator_without_callback_server.generate(
          pg.PayloadGeneratorConfig(
              interpretation_environment=pg.PayloadGeneratorConfig.InterpretationEnvironment.INTERPRETATION_ANY,
              execution_environment=pg.PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT,
          )
      )
    self.assertEqual(
        (
            'No payload implemented for VULNERABILITY_TYPE_UNSPECIFIED'
            ' vulnerability type, INTERPRETATION_ANY interpretation'
            ' environment, and EXEC_INTERPRETATION_ENVIRONMENT execution'
            ' environment.'
        ),
        str(exc.exception),
    )

  def test_generate_with_no_callback_no_interpretation_env_raises_error(self):
    with self.assertRaises(LookupError) as exc:
      self.payload_generator_without_callback_server.generate(
          pg.PayloadGeneratorConfig(
              vulnerability_type=pg.PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE,
              execution_environment=pg.PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT,
          )
      )
    self.assertEqual(
        (
            'No payload implemented for REFLECTIVE_RCE vulnerability type,'
            ' INTERPRETATION_ENVIRONMENT_UNSPECIFIED interpretation'
            ' environment, and EXEC_INTERPRETATION_ENVIRONMENT execution'
            ' environment.'
        ),
        str(exc.exception),
    )

  def test_generate_with_no_callback_and_no_execution_env_raises_error(self):
    with self.assertRaises(LookupError) as exc:
      self.payload_generator_without_callback_server.generate(
          pg.PayloadGeneratorConfig(
              interpretation_environment=pg.PayloadGeneratorConfig.InterpretationEnvironment.INTERPRETATION_ANY,
              vulnerability_type=pg.PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE,
          )
      )
    self.assertEqual(
        (
            'No payload implemented for REFLECTIVE_RCE vulnerability type,'
            ' INTERPRETATION_ANY interpretation environment, and'
            ' EXECUTION_ENVIRONMENT_UNSPECIFIED execution environment.'
        ),
        str(exc.exception),
    )

  def test_generate_with_no_callback_and_no_config_raises_lookup_error(self):
    with self.assertRaises(LookupError) as exc:
      self.payload_generator_without_callback_server.generate(
          pg.PayloadGeneratorConfig()
      )
    self.assertEqual(
        (
            'No payload implemented for VULNERABILITY_TYPE_UNSPECIFIED'
            ' vulnerability type, INTERPRETATION_ENVIRONMENT_UNSPECIFIED'
            ' interpretation environment, and EXECUTION_ENVIRONMENT_UNSPECIFIED'
            ' execution environment.'
        ),
        str(exc.exception),
    )

  def test_generate_with_no_callback_and_no_validation_type_raises_error(self):
    payload_list = [pg.PayloadDefinition(
        interpretation_environment=pg.PayloadGeneratorConfig.InterpretationEnvironment.LINUX_SHELL,
        name=wrappers_pb2.StringValue(value='linux_printf'),
        execution_environment=pg.PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT,
        vulnerability_type=[pg.PayloadGeneratorConfig.VulnerabilityType.VULNERABILITY_TYPE_UNSPECIFIED],
        uses_callback_server=wrappers_pb2.BoolValue(value=False),
        payload_string=wrappers_pb2.StringValue(value='printf %s%s%s TSUNAMI_'
                                                'PAYLOAD_START $TSUNAMI_PAYLOAD'
                                                '_TOKEN_RANDOM TSUNAMI_PAYLOAD_'
                                                'END'),
        )]
    psg = PayloadSecretGenerator()
    psg.generate = unittest.mock.MagicMock(return_value=_SECRET)
    payload_generator_no_callback = PayloadGenerator(
        psg,
        payload_list,
        TcsClient('', 0, '', RequestsHttpClientBuilder().build()),
    )
    with self.assertRaises(NotImplementedError) as exc:
      payload_generator_no_callback.generate(LINUX_UNSPECIFIED_CONFIG)
    self.assertEqual(
        'Validation type VULNERABILITY_TYPE_UNSPECIFIED not supported.',
        str(exc.exception),
    )

  def test_check_if_executed_with_no_callback_and_no_payload_raises_error(self):
    payload = self.payload_generator_without_callback_server.generate(
        LINUX_REFLECTIVE_RCE_CONFIG
    )
    with self.assertRaises(ValueError) as exc:
      payload.check_if_executed()
    self.assertEqual('No valid payload input is entered.', str(exc.exception))


if __name__ == '__main__':
  absltest.main()
