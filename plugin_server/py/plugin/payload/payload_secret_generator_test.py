"""Tests for google3.third_party.java_src.tsunami.plugin_server.py.plugin.payload.payload_secret_generator."""

import base64

from testing.pybase import googletest
from tsunami.plugin_server.py.plugin.payload.payload_secret_generator import PayloadSecretGenerator


_SECRET_BYTE_SIZE = 8


class PayloadSecretGeneratorTest(googletest.TestCase):

  def test_generate_returns_secret_with_specified_size(self):
    secret = PayloadSecretGenerator().generate(_SECRET_BYTE_SIZE)
    self.assertLen(base64.b16decode(secret), _SECRET_BYTE_SIZE)


if __name__ == "__main__":
  googletest.main()
