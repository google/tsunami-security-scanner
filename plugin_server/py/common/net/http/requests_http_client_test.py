"""Tests for google3.third_party.java_src.tsunami.plugin_server.py.common.net.requests_http_client."""

import unittest

from absl.testing import absltest
import requests
import requests_mock

from tsunami.plugin_server.py.common.data import network_endpoint_utils
from tsunami.plugin_server.py.common.net.http.host_resolver_http_adapter import HostResolverHttpAdapter
from tsunami.plugin_server.py.common.net.http.http_header_fields import HttpHeaderFields
from tsunami.plugin_server.py.common.net.http.http_headers import HttpHeaders
from tsunami.plugin_server.py.common.net.http.http_method import HttpMethod
from tsunami.plugin_server.py.common.net.http.http_request import HttpRequest
from tsunami.plugin_server.py.common.net.http.http_response import HttpResponse
from tsunami.plugin_server.py.common.net.http.http_status import HttpStatus
from tsunami.plugin_server.py.common.net.http.requests_http_client import RequestsHttpClient
from tsunami.plugin_server.py.common.net.http.requests_http_client import RequestsHttpClientBuilder
from tsunami.proto import network_pb2
from tsunami.proto import network_service_pb2


class RequestsHttpClientTest(absltest.TestCase):
  @classmethod
  def setUpClass(cls):
    super().setUpClass()
    cls.client = RequestsHttpClientBuilder().build()

  def test_get_log_id(self):
    self.assertEqual(self.client.modify().set_log_id(1).build().get_log_id(), 1)

  @requests_mock.mock()
  def test_send_returns_expected_http_response(self, mock):
    url = 'http://example.com/send/%2e%2e/%2e%2e/etc/path'
    mock.register_uri(HttpMethod.GET, url)
    response = self.client.send(
        HttpRequest().get(url).with_empty_headers().build()
    )
    expected = self._create_expected_response(
        url, headers=HttpHeaders().builder().build()
    )
    self._assert_response_is_expected(response, expected)

  @requests_mock.mock()
  def test_send_with_get_request_returns_expected_http_response(self, mock):
    body = 'GET BODY'.encode('utf-8')
    url = 'http://example.com/get/[]%$/test-path'
    header_field = HttpHeaderFields.CONTENT_TYPE.value
    header_value = 'text/html; charset=utf-8'
    mock.register_uri(
        HttpMethod.GET, url, content=body, headers={header_field: header_value}
    )
    response = self.client.send(
        HttpRequest()
        .get(url)
        .with_empty_headers()
        .build()
    )
    expected = self._create_expected_response(url, body=body)
    self._assert_response_is_expected(response, expected)

  @requests_mock.mock()
  def test_send_async_with_get_request_returns_expected_http_response(
      self, mock
  ):
    body = 'GET BODY'.encode('utf-8')
    url = 'http://example.com/get/[]%$/test-path'
    header_field = HttpHeaderFields.CONTENT_TYPE.value
    header_value = 'text/html; charset=utf-8'
    mock.register_uri(
        HttpMethod.GET, url, content=body, headers={header_field: header_value}
    )
    response = self.client.send_async(
        HttpRequest()
        .get(url)
        .with_empty_headers()
        .build()
    )
    expected = self._create_expected_response(url, body=body)
    self._assert_response_is_expected(response, expected)

  @requests_mock.mock()
  def test_send_with_head_request_returns_http_response_without_body(
      self, mock
  ):
    body = 'Body should not exist.'.encode('utf-8')
    url = 'http://example.com/send/[]%$/test-path'
    header_field = HttpHeaderFields.CONTENT_TYPE.value
    header_value = 'text/html; charset=utf-8'
    mock.register_uri(
        HttpMethod.HEAD, url, content=body, headers={header_field: header_value}
    )
    response = self.client.send(
        HttpRequest()
        .head(url)
        .with_empty_headers()
        .build()
    )
    expected = self._create_expected_response(url, body=body)
    self._assert_response_is_expected(response, expected)

  @requests_mock.mock()
  def test_send_async_with_head_request_returns_expected_http_response_without_body(
      self, mock
  ):
    body = 'HEAD BODY'.encode('utf-8')
    url = 'http://example.com/post/[]%$/test-path'
    header_field = HttpHeaderFields.CONTENT_TYPE.value
    header_value = 'text/html; charset=utf-8'
    mock.register_uri(
        HttpMethod.HEAD, url, content=body, headers={header_field: header_value}
    )
    response = self.client.send_async(
        HttpRequest()
        .head(url)
        .with_empty_headers()
        .build()
    )
    expected = self._create_expected_response(url, body=body)
    self._assert_response_is_expected(response, expected)

  @requests_mock.mock()
  def test_send_with_post_request_returns_expected_http_response(
      self, mock
  ):
    body = 'POST BODY'.encode('utf-8')
    url = 'http://example.com/post/[]%$/test-path'
    header_field = HttpHeaderFields.CONTENT_TYPE.value
    header_value = 'text/html; charset=utf-8'
    mock.register_uri(
        HttpMethod.POST, url, content=body, headers={header_field: header_value}
    )
    response = self.client.send(
        HttpRequest()
        .post(url)
        .set_headers(
            HttpHeaders()
            .builder()
            .add_header(header_field, header_value)
            .build()
        )
        .build()
    )
    expected = self._create_expected_response(url, body=body)
    self._assert_response_is_expected(response, expected)

  @requests_mock.mock()
  def test_send_async_with_post_request_returns_expected_http_response(
      self, mock
  ):
    body = 'POST BODY'.encode('utf-8')
    url = 'http://example.com/post/[]%$/test-path'
    header_field = HttpHeaderFields.CONTENT_TYPE.value
    header_value = 'text/html; charset=utf-8'
    mock.register_uri(
        HttpMethod.POST, url, content=body, headers={header_field: header_value}
    )
    response = self.client.send_async(
        HttpRequest()
        .post(url)
        .set_headers(
            HttpHeaders()
            .builder()
            .add_header(header_field, header_value)
            .build()
        )
        .build()
    )
    expected = self._create_expected_response(url, body=body)
    self._assert_response_is_expected(response, expected)

  @requests_mock.mock()
  def test_send_with_post_request_with_empty_headers_returns_expected_http_response(
      self, mock
  ):
    body = '{ "test": "json" }'.encode('utf-8')
    url = 'http://example.com/post/test-path'
    header_field = HttpHeaderFields.CONTENT_TYPE.value
    header_value = 'text/html; charset=utf-8'
    mock.register_uri(
        HttpMethod.POST, url, content=body, headers={header_field: header_value}
    )
    response = self.client.send(
        HttpRequest()
        .post(url)
        .with_empty_headers()
        .build()
    )
    expected = self._create_expected_response(url, body=body)
    self._assert_response_is_expected(response, expected)

  @requests_mock.mock()
  def test_send_async_with_post_request_with_empty_headers_returns_expected_http_response(
      self, mock
  ):
    body = '{ \"test\": \"json\" }'.encode('utf-8')
    url = 'http://example.com/post/test-path'
    header_field = HttpHeaderFields.CONTENT_TYPE.value
    header_value = 'text/html; charset=utf-8'
    mock.register_uri(
        HttpMethod.POST, url, content=body, headers={header_field: header_value}
    )
    response = self.client.send_async(
        HttpRequest().post(url).with_empty_headers().build()
    )
    expected = self._create_expected_response(
        url, body='{ "test": "json" }'.encode('utf-8')
    )
    self._assert_response_is_expected(response, expected)

  @requests_mock.mock()
  def test_send_with_hostname_and_ip_use_hostname_as_proxy(self, mock):
    url = 'http://example.com/post/test-path'
    network_endpoint = network_endpoint_utils.for_ip_and_hostname(
        '127.0.0.1', 'proxy.com'
    )
    network_service = network_service_pb2.NetworkService(
        network_endpoint=network_endpoint,
        transport_protocol=network_pb2.TransportProtocol.TCP,
    )
    adapter = HostResolverHttpAdapter(5, 10)
    requests.Session.get_adapter = unittest.mock.MagicMock(return_value=adapter)
    mock.register_uri(HttpMethod.GET, url)
    response = self.client.send(
        HttpRequest()
        .get(url)
        .with_empty_headers()
        .build(),
        network_service=network_service,
    )
    expected = self._create_expected_response(
        url, headers=HttpHeaders.builder().build()
    )
    self._assert_response_is_expected(response, expected)

  @requests_mock.mock()
  def test_send_async_with_hostname_and_ip_use_hostname_as_proxy(self, mock):
    url = 'http://example.com/post/test-path'
    network_endpoint = network_endpoint_utils.for_ip_and_hostname(
        '127.0.0.1', 'proxy.com'
    )
    network_service = network_service_pb2.NetworkService(
        network_endpoint=network_endpoint,
        transport_protocol=network_pb2.TransportProtocol.TCP,
    )
    adapter = HostResolverHttpAdapter(5, 10)
    requests.Session.get_adapter = unittest.mock.MagicMock(return_value=adapter)
    mock.register_uri(HttpMethod.GET, url)
    response = self.client.send_async(
        HttpRequest()
        .get(url)
        .with_empty_headers()
        .build(),
        network_service=network_service,
    )
    expected = self._create_expected_response(
        url, headers=HttpHeaders.builder().build()
    )
    self._assert_response_is_expected(response, expected)

  def test_requests_http_client_default_configs(self):
    self.assertTrue(self.client.allow_redirects)
    self.assertTrue(self.client.verify_ssl)

  @requests_mock.mock()
  def test_send_with_modified_configuration(self, mock):
    url = 'http://example.com/post/test-path'
    network_endpoint = network_endpoint_utils.for_ip_and_hostname(
        '127.0.0.1', 'proxy.com'
    )
    network_service = network_service_pb2.NetworkService(
        network_endpoint=network_endpoint,
        transport_protocol=network_pb2.TransportProtocol.TCP,
    )
    mock.register_uri(HttpMethod.GET, url)
    client = (
        RequestsHttpClient.modify()
        .set_timeout_sec(1.1)
        .set_verify_ssl(False)
        .set_allow_redirects(False)
        .set_max_workers(2)
        .set_pool_connections(1)
        .set_pool_maxsize(1)
    )
    self.assertFalse(client.allow_redirects)
    self.assertFalse(client.verify_ssl)
    self.assertEqual(client.timeout_sec, 1.1)
    self.assertEqual(client.max_workers, 2)
    response = client.build().send(
        HttpRequest().get(url).with_empty_headers().build(),
        network_service=network_service,
    )
    expected = self._create_expected_response(
        url, headers=HttpHeaders.builder().build()
    )
    self._assert_response_is_expected(response, expected)

  def test_send_when_request_failed_raise_error(self):
    url = 'http://example.com/post/test-path'
    with self.assertRaises(requests.exceptions.RequestException):
      self.client.send(
          HttpRequest().post(url).with_empty_headers().build()
      )

  def test_send_async_when_request_failed_raise_error(self):
    url = 'http://example.com/delete/test-path'
    with self.assertRaises(requests.exceptions.RequestException):
      self.client.send_async(
          HttpRequest().delete(url).with_empty_headers().build()
      )

  def test__serialize_request_headers_include_custom_user_agent(self):
    field = HttpHeaderFields.CONTENT_TYPE.value
    value = 'text/html; charset=utf-8'
    headers = HttpHeaders.builder().add_header(field, value).build()
    self.assertEqual(
        self.client._serialize_request_headers(headers),
        {
            field: value,
            HttpHeaderFields.USER_AGENT.value: 'TsunamiSecurityScanner',
        },
    )

  def _assert_response_is_expected(self, response, expected):
    self.assertEqual(response.body_json(), expected.body_json())
    self.assertEqual(response.status, expected.status)
    self.assertEqual(response.url, expected.url)
    self.assertEqual(response.headers.raw_headers, expected.headers.raw_headers)

  def _create_expected_response(
      self,
      url,
      status=HttpStatus.OK,
      headers=HttpHeaders.builder()
      .add_header(
          HttpHeaderFields.CONTENT_TYPE.value, 'text/html; charset=utf-8'
      )
      .build(),
      body=None,
  ):
    return (
        HttpResponse()
        .builder()
        .set_url(url)
        .set_status(status)
        .set_headers(headers)
        .set_response_body(body)
        .build()
    )

if __name__ == '__main__':
  absltest.main()
