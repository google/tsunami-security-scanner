"""Tests for google3.third_party.java_src.tsunami.plugin_server.py.common.net.http_headers."""

import collections
from absl.testing import absltest
from absl.testing import parameterized
from tsunami.plugin_server.py.common.net.http import http_header_fields
from tsunami.plugin_server.py.common.net.http import http_headers


HttpHeaderFields = http_header_fields.HttpHeaderFields


class HttpHeadersTest(parameterized.TestCase):
  def test_builder_add_header_always_puts_in_headers_map(self):
    headers = http_headers.Builder().add_header('test_h', 'test_v').build()
    expected = collections.defaultdict(list, {'test_h': ['test_v']})
    self.assertEqual(headers.raw_headers, expected)

  def test_builder_add_header_with_known_header_canonicalizes_header_name(self):
    field = HttpHeaderFields.ACCEPT_LANGUAGE.value
    headers = http_headers.Builder().add_header(field.upper(), 'en').build()
    expected = collections.defaultdict(list, {field: ['en']})
    self.assertEqual(headers.raw_headers, expected)

  def test_builder_add_header_with_enabled_canonicalization(self):
    field = HttpHeaderFields.ACCEPT_LANGUAGE.value
    headers = http_headers.Builder().add_header(field, 'en', True).build()
    expected = collections.defaultdict(list, {field: ['en']})
    self.assertEqual(headers.raw_headers, expected)

  def test_builder_add_header_with_disabled_canonicalization_adds_raw_header(
      self):
    headers = http_headers.Builder().add_header('ACCEPT_Language', 'en',
                                                False).build()
    expected = collections.defaultdict(list, {'ACCEPT_Language': ['en']})
    self.assertEqual(headers.raw_headers, expected)

  @parameterized.named_parameters(
      ('with_no_name', None, 'en', 'Name cannot be None.'),
      ('with_no_value', HttpHeaderFields.ACCEPT_LANGUAGE.value, None,
       'Value cannot be None.'),
      ('with_illegal_header_name', ':::', 'en', 'Illegal header name :::.'),
      ('with_illegal_header_value', HttpHeaderFields.ACCEPT_LANGUAGE.value,
       chr(1), 'Illegal header value %s.' % chr(1)),
  )
  def test_builder_add_header_raises_error(self, field, value, message):
    with self.assertRaises(ValueError) as exc:
      http_headers.Builder().add_header(field, value).build()
    self.assertEqual(message, str(exc.exception))

  def test_names_always_returns_all_header_names(self):
    headers = http_headers.Builder().add_header(
        HttpHeaderFields.ACCEPT.value, 'application/xml').add_header(
            HttpHeaderFields.CONTENT_TYPE.value,
            'text/html').add_header(HttpHeaderFields.ACCEPT.value,
                                    'image/webp').build()
    expected = set(
        [HttpHeaderFields.ACCEPT.value, HttpHeaderFields.CONTENT_TYPE.value])
    self.assertEqual(headers.names(), expected)

  def test_get_when_requested_header_exists_returns_requested_header(self):
    headers = http_headers.Builder().add_header(
        HttpHeaderFields.ACCEPT.value,
        'application/xml').add_header(HttpHeaderFields.CONTENT_TYPE.value,
                                      'text/html').build()
    self.assertEqual(
        headers.get(HttpHeaderFields.ACCEPT.value), 'application/xml')

  def test_get_when_multiple_values_exist_returns_first_value(self):
    headers = http_headers.Builder().add_header(
        HttpHeaderFields.ACCEPT.value, 'application/xml').add_header(
            HttpHeaderFields.CONTENT_TYPE.value,
            'text/html').add_header(HttpHeaderFields.ACCEPT.value,
                                    'image/webp').build()
    self.assertEqual(
        headers.get(HttpHeaderFields.ACCEPT.value), 'application/xml')

  def test_get_when_requested_header_does_not_exist_returns_none(self):
    headers = http_headers.Builder().add_header(HttpHeaderFields.ACCEPT.value,
                                                'app/xml').build()
    self.assertIsNone(headers.get('cookie'))

  def test_get_with_none_header_name_raise_error(self):
    headers = http_headers.Builder().add_header(HttpHeaderFields.ACCEPT.value,
                                                'app/xml').build()
    with self.assertRaises(ValueError) as exc:
      headers.get(None)
    self.assertEqual('Name cannot be None.', str(exc.exception))

  def test_get_all_always_returns_requested_values(self):
    headers = http_headers.Builder().add_header(
        HttpHeaderFields.ACCEPT.value, 'application/xml').add_header(
            HttpHeaderFields.CONTENT_TYPE.value,
            'text/html').add_header(HttpHeaderFields.ACCEPT.value,
                                    'image/webp').build()
    expected = ['application/xml', 'image/webp']
    self.assertEqual(headers.get_all('accept'), expected)

  def test_get_all_with_known_header_value_canonicalizes_requested_header(self):
    headers = http_headers.Builder().add_header(
        HttpHeaderFields.ACCEPT.value, 'application/xml').add_header(
            HttpHeaderFields.CONTENT_TYPE.value,
            'text/html').add_header(HttpHeaderFields.ACCEPT.value,
                                    'image/webp').build()
    expected = ['application/xml', 'image/webp']
    self.assertEqual(headers.get_all('ACCEPT'), expected)

  def test_get_all_when_request_value_does_not_exist_returns_empty_list(self):
    headers = http_headers.Builder().add_header(
        HttpHeaderFields.ACCEPT.value, 'application/xml').add_header(
            HttpHeaderFields.CONTENT_TYPE.value,
            'text/html').add_header(HttpHeaderFields.ACCEPT.value,
                                    'image/webp').build()
    self.assertEmpty(headers.get_all('cookie'))

  def test_get_all_with_none_header_name_raise_error(self):
    headers = http_headers.Builder().add_header(HttpHeaderFields.ACCEPT.value,
                                                'app/xml').build()
    with self.assertRaises(ValueError) as exc:
      headers.get_all(None)
    self.assertEqual('Name cannot be None.', str(exc.exception))

if __name__ == '__main__':
  absltest.main()
