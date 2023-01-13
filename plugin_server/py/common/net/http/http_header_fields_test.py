"""Tests for google3.third_party.java_src.tsunami.plugin_server.py.common.net.http_headers."""

from absl.testing import absltest
from tsunami.plugin_server.py.common.net.http import http_header_fields


HttpHeaderFields = http_header_fields.HttpHeaderFields


class HttpHeaderFieldsTest(absltest.TestCase):
  def test_list_returns_all_header_fields(self):
    self.assertEqual(HttpHeaderFields.list().__len__(), 165)

  def test_get_from_lower_with_existing_field(self):
    self.assertEqual(
        HttpHeaderFields.get_from_lower('CDN-LOOP'),
        HttpHeaderFields.CDN_LOOP.value)

  def test_get_from_lower_with_unknown_field(self):
    self.assertIsNone(HttpHeaderFields.get_from_lower('CDN-LAP'))

if __name__ == '__main__':
  absltest.main()
