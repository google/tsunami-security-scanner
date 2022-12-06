"""Static utility methods pertaining to network endpoint protocol buffer.

For any utility update, please consider if Java's network endpoint utils
(common/src/main/java/com/google/tsunami/common/data/NetworkEndpointUtils.java)
also needs the modification.
"""

import ipaddress
from typing import Optional
from tsunami.proto import network_pb2

AddressFamily = network_pb2.AddressFamily
Hostname = network_pb2.Hostname
IpAddress = network_pb2.IpAddress
NetworkEndpoint = network_pb2.NetworkEndpoint
Type = NetworkEndpoint.Type
DEFINITELY_IP_TYPES = [
    NetworkEndpoint.Type.IP,
    NetworkEndpoint.Type.IP_PORT,
    NetworkEndpoint.Type.IP_HOSTNAME,
    NetworkEndpoint.Type.IP_HOSTNAME_PORT
]
DEFINITELY_HOSTNAME_TYPES = [
    NetworkEndpoint.Type.HOSTNAME,
    NetworkEndpoint.Type.HOSTNAME_PORT,
    NetworkEndpoint.Type.IP_HOSTNAME,
    NetworkEndpoint.Type.IP_HOSTNAME_PORT
]
DEFINITELY_PORT_TYPES = [
    NetworkEndpoint.Type.IP_PORT,
    NetworkEndpoint.Type.IP_HOSTNAME_PORT,
    NetworkEndpoint.Type.HOSTNAME_PORT
]

MAX_PORT_NUMBER = 65535


def has_ip_address(network_endpoint: NetworkEndpoint) -> bool:
  return network_endpoint.type in DEFINITELY_IP_TYPES


def has_hostname(network_endpoint: NetworkEndpoint) -> bool:
  return network_endpoint.type in DEFINITELY_HOSTNAME_TYPES


def has_port(network_endpoint: NetworkEndpoint) -> bool:
  return network_endpoint.type in DEFINITELY_PORT_TYPES


def is_ipv6_endpoint(network_endpoint: NetworkEndpoint) -> bool:
  return has_ip_address(
      network_endpoint
  ) and network_endpoint.ip_address.address_family == AddressFamily.IPV6


def to_uri_authority(network_endpoint: NetworkEndpoint) -> str:
  """Converts network endpoint to URI string.

  Composes URI from the given endpoint information.
  Hostname takes precedence over IP address as hostname is a stable identifier
  for a physical thing whereas IP addresses are ephemeral and unstable.

  Args:
    network_endpoint: instance of a network endpoint protobuf.

  Returns:
    Return None with an unspecified endpoint type. Return URI string with
    valid endpoint type. For example, various combination of ip address, port,
    and hostname would generate the below uri:
      *  ip_v4 = "1.2.3.4" -> uri = "1.2.3.4"
      *  ip_v6 = "3ffe::1" -> uri = "[3ffe::1]"
      *   host = "localhost" -> uri = "localhost"
      *  ip_v4 = "1.2.3.4"   port = 8888 -> uri = "1.2.3.4:8888"
      *  ip_v6 = "3ffe::1"   port = 8888 -> uri = "[3ffe::1]:8888"
      *   host = "localhost" port = 8888 -> uri = "localhost:8888"

  Raises:
    ValueError: an error occurred while looking up network endpoint type.
  """

  ip_address = network_endpoint.ip_address.address
  port = network_endpoint.port.port_number
  hostname = network_endpoint.hostname.name
  uri = ''

  if network_endpoint.type == NetworkEndpoint.Type.TYPE_UNSPECIFIED:
    raise_invalid_network_endpoint_type(network_endpoint.type)
  if network_endpoint.type == NetworkEndpoint.Type.IP:
    uri = ip_to_uri(ip_address)
  elif network_endpoint.type == NetworkEndpoint.Type.IP_PORT:
    uri = ip_to_uri(ip_address) + ':' + str(port)
  elif network_endpoint.type == NetworkEndpoint.Type.IP_HOSTNAME or network_endpoint.type == NetworkEndpoint.Type.HOSTNAME:
    uri = hostname
  elif network_endpoint.type == NetworkEndpoint.Type.IP_HOSTNAME_PORT or network_endpoint.type == NetworkEndpoint.Type.HOSTNAME_PORT:
    uri = hostname + ':' + str(port)
  return uri


def for_ip(ip_address: str) -> NetworkEndpoint:
  """Convert ip address to network endpoint protobuf."""
  network_endpoint = create_with_ip(ip_address)
  network_endpoint.type = NetworkEndpoint.Type.IP
  return network_endpoint


def for_ip_and_port(ip_address: str, port: int) -> NetworkEndpoint:
  """Convert ip address and port to network endpoint protobuf."""
  validate_port(port)
  network_endpoint = create_with_ip(ip_address)
  network_endpoint.type = NetworkEndpoint.Type.IP_PORT
  network_endpoint.port.port_number = port
  return network_endpoint


def for_hostname(hostname: str) -> NetworkEndpoint:
  """Convert hostname to network endpoint protobuf."""
  validate_hostname(hostname)
  network_endpoint = NetworkEndpoint()
  network_endpoint.hostname.name = hostname
  network_endpoint.type = NetworkEndpoint.Type.HOSTNAME
  return network_endpoint


def for_ip_and_hostname(ip_address: str, hostname: str) -> NetworkEndpoint:
  """Convert ip address and hostname to network endpoint protobuf."""
  network_endpoint = create_with_ip(ip_address)
  network_endpoint.type = NetworkEndpoint.Type.IP_HOSTNAME
  network_endpoint.hostname.name = hostname
  return network_endpoint


def for_hostname_and_port(hostname: str, port: int) -> NetworkEndpoint:
  """Convert hostname and port to network endpoint protobuf."""
  validate_port(port)
  validate_hostname(hostname)
  network_endpoint = NetworkEndpoint()
  network_endpoint.hostname.name = hostname
  network_endpoint.type = NetworkEndpoint.Type.HOSTNAME_PORT
  network_endpoint.port.port_number = port
  return network_endpoint


def for_ip_hostname_and_port(ip_address: str, hostname: str,
                             port: int) -> NetworkEndpoint:
  """Convert ip address, hostname and port to network endpoint protobuf."""
  validate_port(port)
  network_endpoint = create_with_ip(ip_address)
  network_endpoint.type = NetworkEndpoint.Type.IP_HOSTNAME_PORT
  network_endpoint.hostname.name = hostname
  network_endpoint.port.port_number = port
  return network_endpoint


def for_network_endpoint_and_port(network_endpoint: NetworkEndpoint,
                                  port: int) -> Optional[NetworkEndpoint]:
  """Create protobuf from endpoint type lookup."""
  validate_port(port)

  if network_endpoint.type == NetworkEndpoint.Type.IP:
    return for_ip_and_port(network_endpoint.ip_address.address, port)
  elif network_endpoint.type == NetworkEndpoint.Type.HOSTNAME:
    return for_hostname_and_port(network_endpoint.hostname.name, port)
  elif network_endpoint.type == NetworkEndpoint.Type.IP_HOSTNAME:
    return for_ip_hostname_and_port(network_endpoint.ip_address.address,
                                    network_endpoint.hostname.name, port)
  else:
    raise_invalid_network_endpoint_type(network_endpoint.type)


def create_with_ip(ip_address: str) -> NetworkEndpoint:
  """Converts ip address to protobuf network endpoint."""
  try:
    ipaddress.ip_address(ip_address)
  except ValueError as exc:
    raise ValueError('%s is not an IP address.' % ip_address) from exc
  return NetworkEndpoint(
      ip_address=IpAddress(
          address=ip_address,
          address_family=address_family(ip_address)))


def validate_port(port: int) -> None:
  if (port < 0 or port > MAX_PORT_NUMBER):
    raise ValueError('Port out of range. Expected [0, %s].' % MAX_PORT_NUMBER)


def validate_hostname(hostname: str) -> None:
  try:
    ipaddress.ip_address(hostname)
    raise Exception("Expected hostname, got IP address '%s'." % hostname)
  except ValueError:
    pass


def raise_invalid_network_endpoint_type(endpoint_type: Type) -> None:
  raise ValueError('Invalid network endpoint type: %s.' %
                   NetworkEndpoint.Type.Name(endpoint_type))


def address_family(ip_address: str) -> AddressFamily:
  try:
    ipaddress.IPv4Address(ip_address)
    return AddressFamily.IPV4
  except ipaddress.AddressValueError:
    return AddressFamily.IPV6


def ip_to_uri(ip_address: str) -> str:
  return ip_address if address_family(
      ip_address) == AddressFamily.IPV4 else '[%s]' % ip_address
