"""Utilities for handling network_service_pb2 protos.

For any utility update, please consider if Java's network service utils
(common/src/main/java/com/google/tsunami/common/data/NetworkServiceUtils.java)
also needs the modification.
"""


import socket
import urllib.parse
from tsunami.plugin_server.py.common.data import network_endpoint_utils
from tsunami.proto import network_pb2
from tsunami.proto import network_service_pb2

urlparse = urllib.parse.urlparse
NetworkService = network_service_pb2.NetworkService
ServiceContext = network_service_pb2.ServiceContext
WebServiceContext = network_service_pb2.WebServiceContext
AddressFamily = network_pb2.AddressFamily
Hostname = network_pb2.Hostname
IpAddress = network_pb2.IpAddress
NetworkEndpoint = network_pb2.NetworkEndpoint
Port = network_pb2.Port
TransportProtocol = network_pb2.TransportProtocol

is_plain_http_by_known_web_service_name = {
    "http": True,
    "http-alt": True,
    "http-proxy": True,
    "https": False,
    "radan-http": True,
    "ssl/http": False,
    "ssl/https": False,
}


def is_web_service(network_service: NetworkService) -> bool:
  return network_service.service_name.lower(
  ) in is_plain_http_by_known_web_service_name


def is_plain_http_service(network_service: NetworkService) -> bool:
  return is_web_service(
      network_service) and is_plain_http_by_known_web_service_name.get(
          network_service.service_name.lower(), False)


def get_service_name(network_service: NetworkService) -> str:
  return network_service.software.name.lower(
  ) or network_service.service_name.lower()


def build_uri_network_service(uri_string: str) -> NetworkService:
  """Compose network service protobuf from URI.

  Parses the URI string into host, scheme, port, ip address, and ip address
  family. Then uses the above information to compose the network endpoint.

  Args:
    uri_string: the uri of the endpoint

  Returns:
    Network endpoint protobuf

  Raises:
    ValueError: if uri is not http or https or if ip address is invalid
  """
  uri = urlparse(uri_string)
  hostname = uri.hostname
  scheme = uri.scheme
  validate_scheme(scheme)

  port = sanitize_port(uri.port, scheme)
  address_info = socket.getaddrinfo(hostname, port)[0]
  ip_address = address_info[4][0]
  address_family = get_address_family(address_info[0])

  network_endpoint = NetworkEndpoint(
      ip_address=IpAddress(
          address_family=address_family,
          address=ip_address,
      ),
      type=NetworkEndpoint.Type.IP_HOSTNAME_PORT,
      hostname=Hostname(name=hostname),
      port=Port(port_number=port),
  )
  return NetworkService(
      network_endpoint=network_endpoint,
      transport_protocol=TransportProtocol.TCP,
      service_name=scheme,
      service_context=ServiceContext(
          web_service_context=WebServiceContext(application_root=uri.path)
      )
  )


def build_web_application_root_url(network_service: NetworkService) -> str:
  """Build the root url for web application service.

  Args:
    network_service: network service protobuf with a valid service defined
    in the is_plain_http_by_known_web_service_name dict.

  Returns:
    The root URL for the web service which always ends with a "/"
    (i.e., http://localhost:8080/, https://127.1.23.1/pathway/)
  """
  if not is_web_service(network_service):
    raise ValueError("Invalid network service: %s" % network_service)
  return build_web_protocol(
      network_service) + build_web_uri_authority(
          network_service) + build_web_app_root_path(network_service)


def build_web_protocol(network_service: NetworkService) -> str:
  if is_plain_http_service(network_service):
    return "http://"
  else:
    return "https://"


def build_web_uri_authority(network_service: NetworkService) -> str:
  """Creates URI authority using the network service.

  The URI authority has 2 components: domain name and port number. Removes the
  port number from URI authority if the web service uses the standard default
  port. Port 80 for http/unsecure network and port 443 for https/secure
  network.

  Args:
    network_service: contains the service name and network endpoint needed to
    construct the URI authority.

  Returns:
    The URI authority. For example, various combination of ip
    address, port, hostname and service would generate the below uri:

      With services 'http', 'http-alt', 'http-proxy', and 'radan-http:
        *  ip_v4 = "1.2.3.4" port = 80 -> uri_athority= "1.2.3.4"
        *  ip_v6 = "3ffe::1" port = 80 -> uri_athority = "[3ffe::1]"
        *   host = "localhost" port = 80 -> uri_athority = "localhost"
        *  ip_v4 = "1.2.3.4"   port = 443 -> uri_athority = "1.2.3.4:443"
        *  ip_v6 = "3ffe::1"   port = 8000 -> uri_athority = "[3ffe::1]:8000"
        *   host = "localhost" port = 8888 -> uri_athority = "localhost:8888"

      With services 'https', 'ssl/http', and 'ssl/https':
        *  ip_v4 = "1.2.3.4" port = 443 -> uri_athority = "1.2.3.4"
        *  ip_v6 = "3ffe::1" port = 443 -> uri_athority = "[3ffe::1]"
        *   host = "localhost" port = 443 -> uri_athority = "localhost"
        *  ip_v4 = "1.2.3.4"   port = 8000 -> uri_athority = "1.2.3.4:8000"
        *  ip_v6 = "3ffe::1"   port = 80 -> uri_athority = "[3ffe::1]:80"
        *   host = "localhost" port = 8888 -> uri_athority = "localhost:8888"
  """
  uri_authority = network_endpoint_utils.to_uri_authority(
      network_service.network_endpoint)
  if is_plain_http_service(network_service) and uri_authority.endswith(
      ":80"):
    return uri_authority.replace(":80", "")
  if not is_plain_http_service(
      network_service) and uri_authority.endswith(":443"):
    return uri_authority.replace(":443", "")
  return uri_authority


def build_web_app_root_path(network_service: NetworkService) -> str:
  if network_service.service_context:
    root_path = network_service.service_context.web_service_context.application_root
  else:
    root_path = "/"
  if not root_path.startswith("/"):
    root_path = "/" + root_path
  if not root_path.endswith("/"):
    root_path = root_path + "/"
  return root_path


def get_address_family(address_family: socket.AddressFamily) -> AddressFamily:
  if address_family == socket.AF_INET:
    return AddressFamily.IPV4
  elif address_family == socket.AF_INET6:
    return AddressFamily.IPV6
  else:
    raise ValueError("Invalid address family: %s" % address_family)


def sanitize_port(port: int or None, scheme: str) -> int:
  if isinstance(port, type(None)):
    return get_port(-1, scheme)
  return get_port(port, scheme)


def get_port(port: int, scheme: str) -> int:
  if port >= 0:
    return port
  return 80 if scheme == "http" else 443


def validate_scheme(scheme: str) -> None:
  if scheme == "http" or scheme == "https":
    pass
  else:
    raise ValueError(
        "URI scheme should be one of the following: 'http', 'https'")
