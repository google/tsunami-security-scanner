"""Tests for google3.third_party.java_src.tsunami.plugin_server.py.plugin.payload.payload."""
from typing import Optional

from absl.testing import absltest
from absl.testing import parameterized

from tsunami.plugin_server.py.plugin.payload.payload import Payload
from tsunami.plugin_server.py.plugin.payload.validator import Validator
from tsunami.proto import payload_generator_pb2 as pg

_CONFIG = pg.PayloadGeneratorConfig()
_PAYLOAD_ATTRIBUTES = pg.PayloadAttributes()


class PayloadTest(parameterized.TestCase):

  class MockValidator(Validator):
    was_called = False

    def is_executed(self, payload_data: Optional[bytes]):
      self.was_called = True
      return False

  def test_get_payload_returns_payload_string(self):
    payload = Payload(
        'payload string', self.MockValidator(), _PAYLOAD_ATTRIBUTES, _CONFIG
    )
    self.assertEqual('payload string', payload.get_payload())

  @parameterized.named_parameters(
      ('with_no_payload_runs_validator', None),
      ('with_payload_string_runs_validator', 'payload string'),
      ('with_bytes_runs_validator', b'payload string'),
  )
  def test_check_if_executed(self, param):
    validator = self.MockValidator()
    payload = Payload('payload string', validator, _PAYLOAD_ATTRIBUTES, _CONFIG)
    payload.check_if_executed(param)
    self.assertTrue(validator.was_called)

  def test_get_payload_attributes_returns_payload_attributes(self):
    validator = self.MockValidator()
    payload = Payload('payload string', validator, _PAYLOAD_ATTRIBUTES, _CONFIG)
    self.assertEqual(_PAYLOAD_ATTRIBUTES, payload.get_payload_attributes())


if __name__ == '__main__':
  absltest.main()
