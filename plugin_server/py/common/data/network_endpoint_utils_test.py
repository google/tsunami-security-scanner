"""Tests for google3.third_party.java_src.tsunami.plugin_server.py.common.data.network_endpoint_utils."""

from absl.testing import absltest
from absl.testing import parameterized
from tsunami.plugin_server.py.common.data import network_endpoint_utils
from tsunami.proto import network_pb2

AddressFamily = network_pb2.AddressFamily
Hostname = network_pb2.Hostname
IpAddress = network_pb2.IpAddress
NetworkEndpoint = network_pb2.NetworkEndpoint
Port = network_pb2.Port
_IPV4 = '8.8.8.8'
_IPV6 = '2001:0db8:85a3:0000:0000:8a2e:0370:7334'
_PORT = 80
_HOSTNAME = 'localhost'


def _make_ipv4_endpoint(port=False):
  network_endpoint = NetworkEndpoint(
      ip_address=IpAddress(address=_IPV4, address_family=AddressFamily.IPV4),
  )
  return _add_network_type_and_port(network_endpoint, port)


def _make_ipv6_endpoint(port=False):
  network_endpoint = NetworkEndpoint(
      ip_address=IpAddress(address=_IPV6, address_family=AddressFamily.IPV6),
  )
  return _add_network_type_and_port(network_endpoint, port)


def _make_hostname_endpoint(port=False):
  network_endpoint = NetworkEndpoint(
      hostname=Hostname(name=_HOSTNAME),
  )
  if port:
    network_endpoint.port.port_number = _PORT
    network_endpoint.type = NetworkEndpoint.Type.HOSTNAME_PORT
  else:
    network_endpoint.type = NetworkEndpoint.Type.HOSTNAME
  return network_endpoint


def _add_network_type_and_port(network_endpoint, port=False):
  if port:
    network_endpoint.port.port_number = _PORT
    network_endpoint.type = NetworkEndpoint.Type.IP_PORT
  else:
    network_endpoint.type = NetworkEndpoint.Type.IP
  return network_endpoint


class NetworkEndpointUtilsTest(parameterized.TestCase):
  def test_has_hostname(self):
    self.assertTrue(network_endpoint_utils.has_hostname(
        _make_hostname_endpoint()))
    self.assertTrue(network_endpoint_utils.has_hostname(
        _make_hostname_endpoint(True)))

    network_endpoint = _make_ipv4_endpoint(True)
    network_endpoint.hostname.name = _HOSTNAME
    network_endpoint.type = NetworkEndpoint.Type.IP_HOSTNAME_PORT
    self.assertTrue(network_endpoint_utils.has_hostname(network_endpoint))

  def test_has_port(self):
    self.assertTrue(network_endpoint_utils.has_port(
        _make_hostname_endpoint(True)))

    network_endpoint = _make_ipv4_endpoint(True)
    network_endpoint.hostname.name = _HOSTNAME
    network_endpoint.type = NetworkEndpoint.Type.IP_HOSTNAME_PORT
    self.assertTrue(network_endpoint_utils.has_port(network_endpoint))

  @parameterized.named_parameters(
      ('ipv6', _make_ipv6_endpoint()),
      ('ipv6_and_port', _make_ipv6_endpoint(True)))
  def test_is_ipv6_endpoint(self, network_endpoint: NetworkEndpoint):
    self.assertTrue(network_endpoint_utils.is_ipv6_endpoint(network_endpoint))

  @parameterized.named_parameters(
      ('ipv4', _make_ipv4_endpoint()),
      ('ipv4_and_port', _make_ipv4_endpoint(True)),
      ('hostname', _make_hostname_endpoint()),
      ('hostname_and_port', _make_hostname_endpoint(True)))
  def test_is_ipv6_endpoint_returns_false(self,
                                          network_endpoint: NetworkEndpoint):
    self.assertFalse(network_endpoint_utils.is_ipv6_endpoint(network_endpoint))

  @parameterized.named_parameters(
      ('ipv4', _make_ipv4_endpoint(), _IPV4),
      ('ipv4_and_port', _make_ipv4_endpoint(True), '8.8.8.8:80'),
      ('ipv6', _make_ipv6_endpoint(),
       '[2001:0db8:85a3:0000:0000:8a2e:0370:7334]'),
      ('ipv6_and_port', _make_ipv6_endpoint(True),
       '[2001:0db8:85a3:0000:0000:8a2e:0370:7334]:80'),
      ('hostname', _make_hostname_endpoint(), _HOSTNAME),
      ('hostname_and_port', _make_hostname_endpoint(True),
       'localhost:80'))
  def test_to_uri_authority(self, network_endpoint: NetworkEndpoint,
                            uri_authority: str):
    self.assertEqual(uri_authority,
                     network_endpoint_utils.to_uri_authority(network_endpoint))

  @parameterized.named_parameters(
      ('ipv4', _make_ipv4_endpoint(), NetworkEndpoint.Type.IP_HOSTNAME,
       _HOSTNAME), ('ipv6_endpoint', _make_ipv6_endpoint(),
                    NetworkEndpoint.Type.IP_HOSTNAME, _HOSTNAME),
      ('ipv4_and_port', _make_ipv4_endpoint(True),
       NetworkEndpoint.Type.IP_HOSTNAME_PORT, 'localhost:80'),
      ('ipv6_and_port', _make_ipv6_endpoint(True),
       NetworkEndpoint.Type.IP_HOSTNAME_PORT, 'localhost:80'))
  def test_to_uri_authority_ip_and_host(self, network_endpoint: NetworkEndpoint,
                                        endpoint_type: NetworkEndpoint.Type,
                                        uri_authority: str):
    network_endpoint.hostname.name = _HOSTNAME
    network_endpoint.type = endpoint_type
    self.assertEqual(uri_authority,
                     network_endpoint_utils.to_uri_authority(network_endpoint))

  def test_to_uri_authority_ip_and_host_with_unspecified_type(self):
    network_endpoint = NetworkEndpoint(
        type=NetworkEndpoint.Type.TYPE_UNSPECIFIED,
    )
    with self.assertRaises(ValueError) as exc:
      network_endpoint_utils.to_uri_authority(network_endpoint)
    self.assertEqual('Invalid network endpoint type: TYPE_UNSPECIFIED.',
                     str(exc.exception))

  def test_for_ip_with_ipv4(self):
    self.assertEqual(_make_ipv4_endpoint(),
                     network_endpoint_utils.for_ip(_IPV4))

  def test_for_ip_with_ipv6(self):
    self.assertEqual(_make_ipv6_endpoint(),
                     network_endpoint_utils.for_ip(_IPV6))

  def test_for_ip_and_port_with_ipv4_and_port(self):
    self.assertEqual(_make_ipv4_endpoint(True),
                     network_endpoint_utils.for_ip_and_port(_IPV4, _PORT))

  def test_for_ip_and_port_with_ipv6_and_port(self):
    self.assertEqual(_make_ipv6_endpoint(True),
                     network_endpoint_utils.for_ip_and_port(_IPV6, _PORT))

  def test_for_hostname_with_hostname(self):
    network_endpoint = _make_hostname_endpoint()
    self.assertEqual(network_endpoint,
                     network_endpoint_utils.for_hostname(_HOSTNAME))

  def test_for_hostname_and_port_with_hostname_and_port(self):
    network_endpoint = _make_hostname_endpoint(True)
    self.assertEqual(
        network_endpoint,
        network_endpoint_utils.for_hostname_and_port(_HOSTNAME, _PORT))

  def test_for_ip_and_hostname_with_ipv4_and_hostname(self):
    network_endpoint = _make_ipv4_endpoint()
    network_endpoint.hostname.name = _HOSTNAME
    network_endpoint.type = NetworkEndpoint.Type.IP_HOSTNAME
    self.assertEqual(
        network_endpoint,
        network_endpoint_utils.for_ip_and_hostname(_IPV4, _HOSTNAME))

  def test_for_ip_and_hostname_with_ipv6_and_hostname(self):
    network_endpoint = _make_ipv6_endpoint()
    network_endpoint.hostname.name = _HOSTNAME
    network_endpoint.type = NetworkEndpoint.Type.IP_HOSTNAME
    self.assertEqual(
        network_endpoint,
        network_endpoint_utils.for_ip_and_hostname(_IPV6, _HOSTNAME))

  def test_for_ip_hostname_and_port_with_ipv4_hostname_and_port(self):
    network_endpoint = _make_ipv4_endpoint(True)
    network_endpoint.hostname.name = _HOSTNAME
    network_endpoint.type = NetworkEndpoint.Type.IP_HOSTNAME_PORT
    self.assertEqual(
        network_endpoint,
        network_endpoint_utils.for_ip_hostname_and_port(_IPV4, _HOSTNAME,
                                                        _PORT))

  def test_for_ip_hostname_and_port_with_ipv6_hostname_and_port(self):
    network_endpoint = _make_ipv6_endpoint(True)
    network_endpoint.hostname.name = _HOSTNAME
    network_endpoint.type = NetworkEndpoint.Type.IP_HOSTNAME_PORT
    self.assertEqual(
        network_endpoint,
        network_endpoint_utils.for_ip_hostname_and_port(_IPV6, _HOSTNAME,
                                                        _PORT))

  def test_for_ip_hostname_and_port_with_invalid_port(self):
    with self.assertRaises(ValueError) as exc:
      network_endpoint_utils.for_ip_hostname_and_port(_IPV6, _HOSTNAME, 65536)
    self.assertEqual('Port out of range. Expected [0, 65535].',
                     str(exc.exception))

  def test_for_network_endpoint_and_port_with_ipv4_and_port(self):
    network_endpoint = network_endpoint_utils.for_ip(_IPV4)
    with self.assertRaises(ValueError) as exc:
      network_endpoint_utils.for_network_endpoint_and_port(network_endpoint, -1)
    self.assertEqual('Port out of range. Expected [0, 65535].',
                     str(exc.exception))

  def test_for_network_endpoint_and_port_with_ipv6_and_port(self):
    network_endpoint = network_endpoint_utils.for_ip(_IPV6)
    self.assertEqual(
        _make_ipv6_endpoint(True),
        network_endpoint_utils.for_network_endpoint_and_port(
            network_endpoint, _PORT))

  def test_for_network_endpoint_and_port_with_hostname_and_port(self):
    network_endpoint = network_endpoint_utils.for_hostname(_HOSTNAME)
    self.assertEqual(
        _make_hostname_endpoint(True),
        network_endpoint_utils.for_network_endpoint_and_port(
            network_endpoint, _PORT))

  def test_for_network_endpoint_and_port_with_ip_and_port(self):
    network_endpoint = _make_ipv4_endpoint()
    network_endpoint.hostname.name = _HOSTNAME
    network_endpoint.type = NetworkEndpoint.Type.IP_HOSTNAME
    ip_hostname_port_endpoint = network_endpoint_utils.for_ip_and_port(
        _IPV4, _PORT)
    ip_hostname_port_endpoint.hostname.name = _HOSTNAME
    ip_hostname_port_endpoint.type = NetworkEndpoint.Type.IP_HOSTNAME_PORT
    self.assertEqual(
        ip_hostname_port_endpoint,
        network_endpoint_utils.for_network_endpoint_and_port(
            network_endpoint, _PORT))

  def test_for_ip_with_invalid_ip(self):
    with self.assertRaises(ValueError) as exc:
      network_endpoint_utils.for_ip('123')
    self.assertEqual('123 is not an IP address.', str(exc.exception))

  def test_for_ip_and_port_with_invalid_ip(self):
    with self.assertRaises(ValueError) as exc:
      network_endpoint_utils.for_ip_and_port('123', _PORT)
    self.assertEqual('123 is not an IP address.', str(exc.exception))

  def test_for_ip_and_port_with_invalid_port(self):
    with self.assertRaises(ValueError) as exc:
      network_endpoint_utils.for_ip_and_port(_IPV4, -1)
    self.assertEqual('Port out of range. Expected [0, 65535].',
                     str(exc.exception))
    with self.assertRaises(ValueError) as exc:
      network_endpoint_utils.for_ip_and_port(_IPV4, 65536)
    self.assertEqual('Port out of range. Expected [0, 65535].',
                     str(exc.exception))

  def test_for_hostname_with_ip(self):
    with self.assertRaises(Exception) as exc:
      network_endpoint_utils.for_hostname(_IPV4)
    self.assertEqual("Expected hostname, got IP address '%s'." % _IPV4,
                     str(exc.exception))
    with self.assertRaises(Exception) as exc:
      network_endpoint_utils.for_hostname(_IPV6)
    self.assertEqual("Expected hostname, got IP address '%s'." % _IPV6,
                     str(exc.exception))

  def test_for_hostname_and_port_with_invalid_ip(self):
    with self.assertRaises(Exception) as exc:
      network_endpoint_utils.for_hostname_and_port(_IPV4, _PORT)
    self.assertEqual("Expected hostname, got IP address '%s'." % _IPV4,
                     str(exc.exception))
    with self.assertRaises(Exception) as exc:
      network_endpoint_utils.for_hostname_and_port(_IPV6, _PORT)
    self.assertEqual("Expected hostname, got IP address '%s'." % _IPV6,
                     str(exc.exception))

  def test_for_hostname_and_port_with_invalid_port(self):
    with self.assertRaises(ValueError) as exc:
      network_endpoint_utils.for_hostname_and_port(_HOSTNAME, -1)
    self.assertEqual('Port out of range. Expected [0, 65535].',
                     str(exc.exception))
    with self.assertRaises(ValueError) as exc:
      network_endpoint_utils.for_hostname_and_port(_HOSTNAME, 65536)
    self.assertEqual('Port out of range. Expected [0, 65535].',
                     str(exc.exception))

  def test_for_network_endpoint_and_port_with_invalid_endpoint_type(self):
    with self.assertRaises(ValueError) as exc:
      network_endpoint_utils.for_network_endpoint_and_port(
          _make_ipv4_endpoint(True), _PORT)
    self.assertEqual('Invalid network endpoint type: IP_PORT.',
                     str(exc.exception))
    with self.assertRaises(ValueError) as exc:
      network_endpoint_utils.for_network_endpoint_and_port(
          _make_hostname_endpoint(True), _PORT)
    self.assertEqual('Invalid network endpoint type: HOSTNAME_PORT.',
                     str(exc.exception))

  def testfor_network_endpoint_and_port_with_invalid_port(self):
    with self.assertRaises(ValueError) as exc:
      network_endpoint_utils.for_network_endpoint_and_port(
          _make_ipv4_endpoint(), -1)
    self.assertEqual('Port out of range. Expected [0, 65535].',
                     str(exc.exception))
    with self.assertRaises(ValueError) as exc:
      network_endpoint_utils.for_network_endpoint_and_port(
          _make_hostname_endpoint(), 65536)
    self.assertEqual('Port out of range. Expected [0, 65535].',
                     str(exc.exception))

if __name__ == '__main__':
  absltest.main()
