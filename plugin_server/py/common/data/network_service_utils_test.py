"""Tests for google3.third_party.java_src.tsunami.plugin_server.py.common.data.network_service_utils."""

import socket
from unittest import mock
import urllib.parse

from absl.testing import absltest
from absl.testing import parameterized
from tsunami.plugin_server.py.common.data import network_service_utils
from tsunami.proto import network_pb2
from tsunami.proto import network_service_pb2

AddressFamily = network_pb2.AddressFamily
NetworkEndpoint = network_pb2.NetworkEndpoint
NetworkService = network_service_pb2.NetworkService
Hostname = network_pb2.Hostname
IpAddress = network_pb2.IpAddress
Port = network_pb2.Port
TransportProtocol = network_pb2.TransportProtocol
WebServiceContext = network_service_pb2.WebServiceContext
_ROOT = 'i_am_root'
_PORT = 8888


class NetworkServiceUtilsTest(parameterized.TestCase):
  def make_web_service(self, service_name: str):
    network_service = NetworkService(service_name=service_name,)
    return network_service

  def make_ip_port_endpoint(self, port: int = _PORT):
    network_endpoint = NetworkEndpoint(
        port=Port(port_number=port),
        ip_address=IpAddress(
            address='0.0.0.0', address_family=AddressFamily.IPV4),
        type=NetworkEndpoint.Type.IP_PORT,
    )
    return network_endpoint

  def make_web_service_context(self, root: str = _ROOT):
    web_service_context = WebServiceContext(application_root=root)
    return web_service_context

  @parameterized.named_parameters(
      ('http', 'http'),
      ('http-alt', 'http-alt'),
      ('http-proxy', 'http-proxy'),
      ('https', 'https'),
      ('ssl/http', 'ssl/http'),
      ('ssl/https', 'ssl/https'),
      ('radan-http', 'radan-http'),
      ('is_case_insensitive', 'HTTPS'))
  def test_is_web_service(self, service_name: str):
    network_service = self.make_web_service(service_name)
    self.assertTrue(network_service_utils.is_web_service(network_service))

  def test_is_web_service_with_invalid_web_service(self):
    network_service = self.make_web_service('ssh')
    self.assertFalse(network_service_utils.is_web_service(network_service))

  @parameterized.named_parameters(
      ('http', 'http'),
      ('http-alt', 'http-alt'),
      ('http-proxy', 'http-proxy'),
      ('radan-http', 'radan-http'))
  def test_is_plain_http_service(self, service_name: str):
    network_service = self.make_web_service(service_name)
    self.assertTrue(
        network_service_utils.is_plain_http_service(network_service))

  @parameterized.named_parameters(
      ('https', 'https'),
      ('ssh', 'ssh'),
      ('ssl/http', 'ssl/http'),
      ('ssl/https', 'ssl/https'))
  def test_is_plain_http_service_with_invalid_service(self, service_name: str):
    network_service = self.make_web_service(service_name)
    self.assertFalse(
        network_service_utils.is_plain_http_service(network_service))

  def test_get_service_name_with_software(self):
    network_service = self.make_web_service('ssh')
    network_service.network_endpoint.CopyFrom(self.make_ip_port_endpoint())
    network_service.software.name = 'Oracle'
    self.assertEqual('oracle',
                     network_service_utils.get_service_name(network_service))

  def test_get_service_name_with_web_service(self):
    network_service = self.make_web_service('ssh')
    network_service.network_endpoint.CopyFrom(self.make_ip_port_endpoint())
    self.assertEqual('ssh',
                     network_service_utils.get_service_name(network_service))

  @parameterized.named_parameters(
      ('http_without_root', 'http', _PORT, '', 'http://0.0.0.0:8888/'),
      ('http_with_root_path', 'http', _PORT, _ROOT,
       'http://0.0.0.0:8888/i_am_root/'),
      ('root_path_no_leading_slash', 'http', _PORT, '/i_am_root',
       'http://0.0.0.0:8888/i_am_root/'),
      ('http_on_port_80', 'http', 80, _ROOT, 'http://0.0.0.0/i_am_root/'),
      ('https_on_port_443', 'https', 443, _ROOT, 'https://0.0.0.0/i_am_root/'),
      ('https_without_root', 'https', 8000, '', 'https://0.0.0.0:8000/'))
  def test_build_web_application_root_url(self, service_name: str, port: int,
                                          root: str, url: str):
    network_service = self.make_web_service(service_name)
    network_service.network_endpoint.CopyFrom(self.make_ip_port_endpoint(port))
    network_service.service_context.web_service_context.CopyFrom(
        self.make_web_service_context(root))
    self.assertEqual(
        url,
        network_service_utils.build_web_application_root_url(network_service))

  def test_build_uri_network_service_with_ipv4(self):
    network_service = self.make_web_service('https')
    network_service.transport_protocol = TransportProtocol.TCP
    network_service.service_context.web_service_context.CopyFrom(
        self.make_web_service_context('/i_am_root'))
    address_info = socket.getaddrinfo('localhost', 443)[0]
    ip_address = address_info[4][0]
    address_family = network_service_utils.get_address_family(address_info[0])

    network_endpoint = NetworkEndpoint(
        port=Port(port_number=443),
        ip_address=IpAddress(
            address=ip_address, address_family=address_family),
        type=NetworkEndpoint.Type.IP_HOSTNAME_PORT,
        hostname=Hostname(name='localhost')
    )
    network_service.network_endpoint.CopyFrom(network_endpoint)

    self.assertEqual(
        network_service,
        network_service_utils.build_uri_network_service(
            'https://localhost/i_am_root'))

  @mock.patch.object(
      socket,
      'getaddrinfo',
      new=mock.MagicMock(
          return_value=[[socket.AF_INET6, None, None, None, ['::1']]]))
  def test_build_uri_network_service_with_ipv6(self):
    network_service = self.make_web_service('http')
    network_service.transport_protocol = TransportProtocol.TCP
    network_service.service_context.web_service_context.CopyFrom(
        self.make_web_service_context('/i_am_root'))

    network_endpoint = NetworkEndpoint(
        port=Port(port_number=80),
        ip_address=IpAddress(
            address='::1', address_family=AddressFamily.IPV6),
        type=NetworkEndpoint.Type.IP_HOSTNAME_PORT,
        hostname=Hostname(name='some_hostname_with_ipv6')
    )
    network_service.network_endpoint.CopyFrom(network_endpoint)

    self.assertEqual(
        network_service,
        network_service_utils.build_uri_network_service(
            'http://some_hostname_with_ipv6/i_am_root'))

  @mock.patch.object(
      urllib.parse,
      'urlparse',
      new=mock.MagicMock(
          return_value={
              'hostname': 'some_hostname_with_ipv6',
              'scheme': 'http',
              'port': 0,
              'path': 'i_am_root'
          }))
  @mock.patch.object(
      socket,
      'getaddrinfo',
      new=mock.MagicMock(
          return_value=[[socket.AF_INET6, None, None, None, ['::1']]]))
  def test_build_uri_network_service_with_port_number_zero(self):
    network_service = self.make_web_service('http')
    network_service.transport_protocol = TransportProtocol.TCP
    network_service.service_context.web_service_context.CopyFrom(
        self.make_web_service_context('/i_am_root'))

    network_endpoint = NetworkEndpoint(
        port=Port(port_number=0),
        ip_address=IpAddress(
            address='::1', address_family=AddressFamily.IPV6),
        type=NetworkEndpoint.Type.IP_HOSTNAME_PORT,
        hostname=Hostname(name='some_hostname_with_ipv6')
    )
    network_service.network_endpoint.CopyFrom(network_endpoint)

    self.assertEqual(
        network_service,
        network_service_utils.build_uri_network_service(
            'http://some_hostname_with_ipv6:0/i_am_root'))

  def test_build_uri_network_service_with_invalid_scheme(self):
    with self.assertRaises(ValueError) as err:
      network_service_utils.build_uri_network_service(
          'http-alt://localhost/i_am_root')
    self.assertEqual(
        "URI scheme should be one of the following: 'http', 'https'",
        str(err.exception))

if __name__ == '__main__':
  absltest.main()
