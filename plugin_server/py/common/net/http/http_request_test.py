"""Tests for google3.third_party.java_src.tsunami.plugin_server.py.common.net.http_request."""

from absl.testing import absltest
from absl.testing import parameterized

from tsunami.plugin_server.py.common.net.http.http_headers import HttpHeaders
from tsunami.plugin_server.py.common.net.http.http_method import HttpMethod
from tsunami.plugin_server.py.common.net.http.http_request import HttpRequest


class HttpRequestTest(parameterized.TestCase):

  @parameterized.named_parameters(
      ('get_method', HttpRequest.get), ('head_method', HttpRequest.head),
      ('post_method', HttpRequest.post), ('delete_method', HttpRequest.delete))
  def test_http_request_methods_with_empty_url_raise_error(
      self, request_method):
    with self.assertRaises(ValueError) as exc:
      request_method('')
    self.assertEqual(str(exc.exception), 'Url cannot be None.')

  def test_get_builds_http_get_request(self):
    request = HttpRequest.get(
        'http://localhost/url').with_empty_headers().build()
    self.assertEqual(request.method, HttpMethod.GET)
    self.assertEqual(request.url, 'http://localhost/url')

  def test_head_builds_http_head_request(self):
    request = HttpRequest.head(
        'http://localhost/url').with_empty_headers().build()
    self.assertEqual(request.method, HttpMethod.HEAD)
    self.assertEqual(request.url, 'http://localhost/url')

  def test_head_builds_http_post_request(self):
    request = HttpRequest.post(
        'http://localhost/url').with_empty_headers().build()
    self.assertEqual(request.method, HttpMethod.POST)
    self.assertEqual(request.url, 'http://localhost/url')

  def test_head_builds_http_delete_request(self):
    request = HttpRequest.delete(
        'http://localhost/url').with_empty_headers().build()
    self.assertEqual(request.method, HttpMethod.DELETE)
    self.assertEqual(request.url, 'http://localhost/url')

  def test_put_builds_http_put_request(self):
    request = HttpRequest.put(
        'http://localhost/url').with_empty_headers().build()
    self.assertEqual(request.method, HttpMethod.PUT)
    self.assertEqual(request.url, 'http://localhost/url')

  def test_get_with_request_body_raise_error(self):
    with self.assertRaises(ValueError) as exc:
      HttpRequest.builder().set_method(
          HttpMethod.GET).set_url('http://localhost/url').set_headers(
              HttpHeaders.builder()).set_request_body(bytes(
                  'abc', 'utf-8')).build()
    self.assertEqual('A request body is not allowed for HTTP GET/HEAD request.',
                     str(exc.exception))

if __name__ == '__main__':
  absltest.main()
