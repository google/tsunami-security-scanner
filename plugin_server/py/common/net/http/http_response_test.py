"""Tests for google3.third_party.java_src.tsunami.plugin_server.py.common.net.http_response."""

from absl.testing import absltest

from tsunami.plugin_server.py.common.net.http.http_headers import HttpHeaders
from tsunami.plugin_server.py.common.net.http.http_response import HttpResponse
from tsunami.plugin_server.py.common.net.http.http_status import HttpStatus


class HttpResponseTest(absltest.TestCase):
  def test_body_json_with_valid_response_body_returns_parsed_json(self):
    response = HttpResponse.builder().set_status(
        HttpStatus.OK).set_response_body(
            bytes('{ \"test_value\": 1 }',
                  'utf-8')).set_url('http://localhost/url').build()
    self.assertNotEmpty(response.body_json())
    self.assertTrue(type(response.body_json()), 'json')
    self.assertEqual(response.body_json()['test_value'], 1)

  def test_body_json_with_no_response_body_returns_null(self):
    response = HttpResponse.builder().set_status(HttpStatus.OK).set_headers(
        HttpHeaders.builder().build()).set_url('http://localhost/url').build()
    self.assertIsNone(response.body_json())

  def test_body_json_with_non_json_response_body_returns_null(self):
    response = HttpResponse.builder().set_status(HttpStatus.OK).set_headers(
        HttpHeaders.builder().build()).set_response_body(bytes(
            'abc', 'utf-8')).set_url('http://localhost/url').build()
    self.assertIsNone(response.body_json())

  def test_json_field_has_value_with_empty_json_body_returns_false(self):
    response = HttpResponse.builder().set_status(HttpStatus.OK).set_headers(
        HttpHeaders.builder().build()).set_response_body(bytes(
            '{ }', 'utf-8')).set_url('http://localhost/url').build()
    self.assertFalse(response.json_field_has_value('field', 'value'))

  def test_json_field_has_value_with_no_body_returns_false(self):
    response = HttpResponse.builder().set_status(HttpStatus.OK).set_headers(
        HttpHeaders.builder().build()).set_url('http://localhost/url').build()
    self.assertFalse(response.json_field_has_value('field', 'value'))

  def test_json_field_has_value_with_non_empty_json_body_returns_true(self):
    response = HttpResponse.builder().set_status(HttpStatus.OK).set_headers(
        HttpHeaders.builder().build()).set_response_body(
            bytes('{ \"field\": \"value\"}',
                  'utf-8')).set_url('http://localhost/url').build()
    self.assertTrue(response.json_field_has_value('field', 'value'))

  def test_is_success_with_unspecified_status_returns_false(self):
    response = HttpResponse.builder().set_status(
        HttpStatus.HTTP_STATUS_UNSPECIFIED).set_headers(
            HttpHeaders.builder().build()).set_url(
                'http://localhost/url').build()
    self.assertFalse(response.status.is_success())

if __name__ == '__main__':
  absltest.main()
