"""Tests for google3.third_party.java_src.tsunami.plugin_server.py.plugin.tcs_client."""

from absl.testing import absltest
from absl.testing import parameterized
import requests_mock
from tsunami.plugin_server.py.common.net.http.requests_http_client import RequestsHttpClientBuilder
from tsunami.plugin_server.py.plugin.tcs_client import TcsClient


SECRET = 'a3d9ed89deadbeef'
CBID = '04041e8898e739ca33a250923e24f59ca41a8373f8cf6a45a1275f3b'
IPV4 = '127.0.0.1'
IPV6 = '2001:0db8:85a3:0000:0000:8a2e:0370:7334'
PORT = 8000
DOMAIN = 'valid.com'
URL = 'http://valid.com'
INVALID_ADDRESS = 'http://invalid.com'


class TcsClientTest(parameterized.TestCase):

  @classmethod
  def setUpClass(cls):
    super().setUpClass()
    cls.http_client = RequestsHttpClientBuilder().build()

  @parameterized.named_parameters(
      (
          'with_valid_ipv4_returns_uri',
          IPV4,
          PORT,
          'http://%s:%s/%s' % (IPV4, PORT, CBID),
      ),
      (
          'with_valid_ipv6_returns_uri',
          IPV6,
          PORT,
          'http://[%s]:%s/%s' % (IPV6, PORT, CBID),
      ),
      (
          'with_valid_domain_returns_uri',
          DOMAIN,
          PORT,
          '%s.%s:%s' % (CBID, DOMAIN, PORT),
      ),
      (
          'with_port_80_and_ip_returns_uri_without_port',
          IPV4,
          80,
          'http://%s/%s' % (IPV4, CBID),
      ),
      (
          'with_port_80_and_domain_returns_uri_without_port',
          DOMAIN,
          80,
          '%s.%s' % (CBID, DOMAIN),
      ),
  )

  def test_get_callback_uri(self, address, port, exepected_uri):
    client = TcsClient(address, port, URL, self.http_client)
    resulted_uri = client.get_callback_uri(SECRET)
    self.assertEqual(resulted_uri, exepected_uri)

  @parameterized.named_parameters(
      ('with_valid_ipv4_returns_true', IPV4, URL, True),
      ('with_valid_ipv6_returns_true', IPV6, URL, True),
      ('with_valid_hostname_and_base_url_returns_true', DOMAIN, URL, True),
      ('with_invalid_hostname_returns_false', '', URL, False),
      ('with_invalid_base_url_returns_false', DOMAIN, '', False),
  )
  def test_is_callback_server_enabled(
      self, address, url, expected
  ):
    client = TcsClient(address, PORT, url, self.http_client)
    self.assertEqual(client.is_callback_server_enabled(), expected)

  def test_tcs_client_with_invalid_port_raises_error(self):
    with self.assertRaises(ValueError):
      TcsClient(DOMAIN, 100000, URL, self.http_client)

  @requests_mock.mock()
  def test_has_oob_log_sends_polling_request(self, mock):
    body = '{ "has_dns_interaction":false, "has_http_interaction":true}'
    mock.register_uri(
        'GET', '%s/?secret=%s' % (URL, SECRET), content=body.encode('utf-8')
    )
    client = TcsClient(DOMAIN, PORT, URL, self.http_client)
    self.assertTrue(client.has_oob_log(SECRET))

  @requests_mock.mock()
  def test_has_oob_log_with_no_logs_returns_false(self, mock):
    body = '{ "has_dns_interaction":false, "has_http_interaction":false}'
    mock.register_uri(
        'GET', '%s/?secret=%s' % (URL, SECRET), content=body.encode('utf-8')
    )
    client = TcsClient(DOMAIN, PORT, URL, self.http_client)
    self.assertFalse(client.has_oob_log(SECRET))

  @requests_mock.mock()
  def test_has_oob_log_with_no_response_returns_false(self, mock):
    mock.register_uri('GET', '%s/?secret=%s' % (URL, SECRET))
    client = TcsClient(DOMAIN, PORT, URL, self.http_client)
    self.assertFalse(client.has_oob_log(SECRET))

  @requests_mock.mock()
  def test_has_oob_log_with_unsuccessful_response_returns_false(self, mock):
    mock.register_uri('GET', '%s/?secret=%s' % (URL, SECRET), status_code=500)
    client = TcsClient(DOMAIN, PORT, URL, self.http_client)
    self.assertFalse(client.has_oob_log(SECRET))

  def test_create_tscclient_removes_trailing_slashes(self):
    client = TcsClient(DOMAIN, PORT, 'http://my-url.com//', self.http_client)
    self.assertEqual('http://my-url.com', client.polling_base_url)

if __name__ == '__main__':
  absltest.main()
