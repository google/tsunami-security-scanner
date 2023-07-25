"""Tests for google3.third_party.java_src.tsunami.plugin_server.py.common.net.requests_http_client."""

from unittest import mock

from absl.testing import absltest
from absl.testing import parameterized
import requests

from tsunami.plugin_server.py.common.net.http.host_resolver_http_adapter import HostResolverHttpAdapter
from tsunami.plugin_server.py.common.net.http.http_header_fields import HttpHeaderFields
from tsunami.plugin_server.py.common.net.http.http_method import HttpMethod


class HostResolverHttpAdapterTest(parameterized.TestCase):

  @classmethod
  def setUpClass(cls):
    super().setUpClass()
    cls.custom_adapter = HostResolverHttpAdapter(5, 10)

  def setUp(self):
    super().setUp()
    self.addCleanup(mock.patch.stopall)
    # Mock of requests's HTTPAdapter
    response = requests.Response()
    response.status_code = 200
    mock.patch.object(
        requests.adapters.HTTPAdapter,
        'send',
        return_value=response,
    ).start()
    # Mock hostname lookup
    self.mock_getaddrinfo = mock.patch('socket.getaddrinfo').start()

  @parameterized.named_parameters(
      ('with_hostname', 'vuln-app.com'),
      ('with_ipv4', '199.21.82.88'),
      (
          'with_ipv6',
          '[2001:0db8:85a3:0000:0000:8a2e:0370:7334]',
      ),
  )
  def test_send_dispatches_with_host_header(self, host):
    url = 'http://{}:8080/send'.format(host)
    request = self._prepare_request(url)

    self.custom_adapter.send(request)

    requests.adapters.HTTPAdapter.send.assert_called_with(request)
    self.assertEqual(
        request.headers.get(HttpHeaderFields.HOST.value), '{}:8080'.format(host)
    )

  def test_send_without_target_ip_dispatches_default_hostname(self):
    url = 'http://vuln-app.com:8080/send'
    request = self._prepare_request(url)

    self.custom_adapter.send(request)

    requests.adapters.HTTPAdapter.send.assert_called_with(request)
    self.assertEqual(request.url, url)

  def test_send_when_hostname_resolves_to_ip_uses_default_hostname(self):
    url = 'http://vuln-app.com:8080/send'
    ip = '199.21.82.88'
    request = self._prepare_request(url)

    self.mock_getaddrinfo.return_value = [ip]
    self.custom_adapter.send(request, ip=ip)

    requests.adapters.HTTPAdapter.send.assert_called_once_with(request)
    self.assertEqual(
        request.headers.get(HttpHeaderFields.HOST.value), 'vuln-app.com:8080'
    )
    self.assertEqual(request.url, url)

  def test_send_when_hostname_is_the_ip_uses_default_hostname(self):
    ip = '2001:0db8:85a3:0000:0000:8a2e:0370:7334'
    url = 'http://[{}]:8080/send'.format(ip)
    request = self._prepare_request(url)

    self.custom_adapter.send(request, ip=ip)

    requests.adapters.HTTPAdapter.send.assert_called_once_with(request)
    self.assertEqual(
        request.headers.get(HttpHeaderFields.HOST.value),
        '[{}]:8080'.format(ip))
    self.assertEqual(request.url, url)

  def test_send_when_hostname_is_case_insensitive(self):
    url = 'http://vuln-APP.com:8080/send'
    ip = '199.21.82.88'
    request = self._prepare_request(url)

    self.custom_adapter.send(request, ip=ip)

    requests.adapters.HTTPAdapter.send.assert_called_once_with(request)
    self.assertEqual(
        request.headers.get(HttpHeaderFields.HOST.value), 'vuln-APP.com:8080'
    )
    self.assertEqual(request.url, 'http://199.21.82.88:8080/send')

  def test_send_when_hostname_does_not_resolve_to_ipv4_uses_ipv4(self):
    url = 'http://vuln-app.com:8080/send'
    ip = '199.21.82.88'
    request = self._prepare_request(url)

    self.mock_getaddrinfo.return_value = ['1.1.1.1']
    self.custom_adapter.send(request, ip=ip)

    requests.adapters.HTTPAdapter.send.assert_called_once_with(request)
    self.assertEqual(
        request.headers.get(HttpHeaderFields.HOST.value), 'vuln-app.com:8080'
    )
    self.assertEqual(request.url, 'http://199.21.82.88:8080/send')

  def test_send_when_hostname_does_not_resolve_to_ipv6_uses_ipv6(self):
    url = 'http://vuln-app.com:8080/send'
    ip = '2001:0db8:85a3:0000:0000:8a2e:0370:7334'
    request = self._prepare_request(url)

    self.mock_getaddrinfo.return_value = ['1.1.1.1']
    self.custom_adapter.send(request, ip=ip)

    requests.adapters.HTTPAdapter.send.assert_called_once_with(request)
    self.assertEqual(
        request.headers.get(HttpHeaderFields.HOST.value), 'vuln-app.com:8080'
    )
    self.assertEqual(
        request.url,
        'http://[2001:0db8:85a3:0000:0000:8a2e:0370:7334]:8080/send',
    )

  def _prepare_request(self, url):
    request = requests.Request(
        method=HttpMethod.GET, url=url, data=b'HTML content'
    )
    request = request.prepare()
    request.url = url
    return request


if __name__ == '__main__':
  absltest.main()
