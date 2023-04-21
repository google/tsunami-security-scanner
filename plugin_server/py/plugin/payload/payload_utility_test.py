"""Tests for google3.third_party.java_src.tsunami.plugin_server.py.plugin.payload.payload_utility."""

import unittest

from pyglib import resources
from testing.pybase import googletest
from tsunami.plugin_server.py.plugin.payload import payload_utility


class PayloadUtilityTest(googletest.TestCase):

  def test_get_parsed_payload_with_good_payloads_returns_payloads(self):
    pd_string = """payloads:
      - name: The Good PD
        interpretation_environment: LINUX_SHELL
        execution_environment: EXEC_INTERPRETATION_ENVIRONMENT
        uses_callback_server: true
        payload_string: curl $TSUNAMI_PAYLOAD_TOKEN_URL
        vulnerability_type:
          - REFLECTIVE_RCE
    """
    resources.GetResource = unittest.mock.Mock(return_value=pd_string)
    self.assertLen(payload_utility.get_parsed_payload(), 1)

  def test_get_parsed_payload_without_name_raise_valueerror(self):
    pd_string = """payloads:
      - interpretation_environment: LINUX_SHELL
        execution_environment: EXEC_INTERPRETATION_ENVIRONMENT
        uses_callback_server: true
        payload_string: curl $TSUNAMI_PAYLOAD_TOKEN_URL
        vulnerability_type:
          - REFLECTIVE_RCE
    """
    with self.assertRaises(ValueError) as exc:
      resources.GetResource = unittest.mock.Mock(return_value=pd_string)
      payload_utility.get_parsed_payload()
    self.assertEqual('Parse payload does not have a name.', str(exc.exception))

  def test_get_parsed_payload_no_interpretation_environment_raise_error(self):
    pd_string = """payloads:
      - name: The Good PD
        interpretation_environment: INTERPRETATION_ENVIRONMENT_UNSPECIFIED
        execution_environment: EXEC_INTERPRETATION_ENVIRONMENT
        uses_callback_server: true
        payload_string: curl $TSUNAMI_PAYLOAD_TOKEN_URL
        vulnerability_type:
          - REFLECTIVE_RCE
    """
    with self.assertRaises(ValueError) as exc:
      resources.GetResource = unittest.mock.Mock(return_value=pd_string)
      payload_utility.get_parsed_payload()
    self.assertEqual(
        'Parse payload does not have an interpretation environment.',
        str(exc.exception),
    )

  def test_get_parsed_payload_no_execution_environment_raise_error(self):
    pd_string = """payloads:
      - name: The Good PD
        interpretation_environment: LINUX_SHELL
        execution_environment: EXECUTION_ENVIRONMENT_UNSPECIFIED
        uses_callback_server: true
        payload_string: curl $TSUNAMI_PAYLOAD_TOKEN_URL
        vulnerability_type:
          - REFLECTIVE_RCE
    """
    with self.assertRaises(ValueError) as exc:
      resources.GetResource = unittest.mock.Mock(return_value=pd_string)
      payload_utility.get_parsed_payload()
    self.assertEqual(
        'Parse payload does not have an execution environment.',
        str(exc.exception),
    )

  def test_get_parsed_payload_no_vulnerability_type_raise_error(self):
    pd_string = """payloads:
      - name: The Good PD
        interpretation_environment: LINUX_SHELL
        execution_environment: EXEC_INTERPRETATION_ENVIRONMENT
        payload_string: curl $TSUNAMI_PAYLOAD_TOKEN_URL
        uses_callback_server: true
        vulnerability_type:
          - VULNERABILITY_TYPE_UNSPECIFIED
    """
    with self.assertRaises(ValueError) as exc:
      resources.GetResource = unittest.mock.Mock(return_value=pd_string)
      payload_utility.get_parsed_payload()
    self.assertEqual(
        'Parse payload does not have a vulnerability type.',
        str(exc.exception),
    )

  def test_get_parsed_payload_no_payload_string_raise_error(self):
    pd_string = """payloads:
      - name: The Good PD
        interpretation_environment: LINUX_SHELL
        execution_environment: EXEC_INTERPRETATION_ENVIRONMENT
        uses_callback_server: true
        vulnerability_type:
          - REFLECTIVE_RCE
    """
    with self.assertRaises(ValueError) as exc:
      resources.GetResource = unittest.mock.Mock(return_value=pd_string)
      payload_utility.get_parsed_payload()
    self.assertEqual(
        'Parse payload does not have a payload string.',
        str(exc.exception),
    )

  def test_get_parsed_payload_no_callback_constant_in_payload_raise_error(self):
    pd_string = """payloads:
      - name: The Good PD
        interpretation_environment: LINUX_SHELL
        execution_environment: EXEC_INTERPRETATION_ENVIRONMENT
        uses_callback_server: true
        payload_string: curl soemthing here
        vulnerability_type:
          - REFLECTIVE_RCE
    """
    with self.assertRaises(ValueError) as exc:
      resources.GetResource = unittest.mock.Mock(return_value=pd_string)
      payload_utility.get_parsed_payload()
    self.assertEqual(
        (
            'Parse payload uses callback server but $TSUNAMI_PAYLOAD_TOKEN_URL'
            ' not found in payload string.'
        ),
        str(exc.exception),
    )

  def test_get_parsed_payload_no_callback_server_no_validation_type_raise_error(
      self):
    pd_string = """payloads:
      - name: The Good PD
        interpretation_environment: LINUX_SHELL
        execution_environment: EXEC_INTERPRETATION_ENVIRONMENT
        uses_callback_server: false
        payload_string: curl $TSUNAMI_PAYLOAD_TOKEN_URL
        validation_type: VALIDATION_TYPE_UNSPECIFIED
        vulnerability_type:
          - REFLECTIVE_RCE
    """
    with self.assertRaises(ValueError) as exc:
      resources.GetResource = unittest.mock.Mock(return_value=pd_string)
      payload_utility.get_parsed_payload()
    self.assertEqual(
        (
            'Parse payload does not have a validation type and does not use the'
            ' callback server.'
        ),
        str(exc.exception),
    )

  def test_get_parsed_payload_no_callback_server_no_val_regex_raise_error(
      self):
    pd_string = """payloads:
      - name: The Good PD
        interpretation_environment: LINUX_SHELL
        execution_environment: EXEC_INTERPRETATION_ENVIRONMENT
        uses_callback_server: false
        payload_string: curl $TSUNAMI_PAYLOAD_TOKEN_URL
        validation_type: VALIDATION_REGEX
        vulnerability_type:
          - REFLECTIVE_RCE
    """
    with self.assertRaises(ValueError) as exc:
      resources.GetResource = unittest.mock.Mock(return_value=pd_string)
      payload_utility.get_parsed_payload()
    self.assertEqual(
        (
            'Parse payload has no validation regex but uses'
            ' PayloadValidationType.REGEX.'
        ),
        str(exc.exception),
    )


if __name__ == '__main__':
  googletest.main()
