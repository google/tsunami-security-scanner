# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Tests for plugin_service."""

import time

from absl.testing import absltest
import grpc_testing
import ipaddr

from google.protobuf import timestamp_pb2
from tsunami.plugin_server.py import plugin_service
from tsunami.plugin_server.py import tsunami_plugin
from tsunami.proto import detection_pb2
from tsunami.proto import network_pb2
from tsunami.proto import network_service_pb2
from tsunami.proto import plugin_representation_pb2
from tsunami.proto import plugin_service_pb2
from tsunami.proto import reconnaissance_pb2
from tsunami.proto import vulnerability_pb2

_NetworkEndpoint = network_pb2.NetworkEndpoint
_NetworkService = network_service_pb2.NetworkService
_PluginInfo = plugin_representation_pb2.PluginInfo
_TargetInfo = reconnaissance_pb2.TargetInfo
_AddressFamily = network_pb2.AddressFamily
_ServiceDescriptor = plugin_service_pb2.DESCRIPTOR.services_by_name[
    'PluginService']
_RunMethod = _ServiceDescriptor.methods_by_name['Run']
_ListPluginsMethod = _ServiceDescriptor.methods_by_name['ListPlugins']
MAX_WORKERS = 1


class PluginServiceTest(absltest.TestCase):

  def setUp(self):
    super().setUp()
    self.test_plugin = FakeVulnDetector()
    self._time = grpc_testing.strict_fake_time(time.time())
    self._server = grpc_testing.server_from_dictionary(
        {
            _ServiceDescriptor:
                plugin_service.PluginServiceServicer(
                    py_plugins=[self.test_plugin], max_workers=MAX_WORKERS),
        }, self._time)

    self._channel = grpc_testing.channel(
        plugin_service_pb2.DESCRIPTOR.services_by_name.values(), self._time)

  def tearDown(self):
    self._channel.close()
    super().tearDown()

  def test_run_plugins_registered_returns_valid_response(self):
    plugin_to_test = FakeVulnDetector()
    endpoint = _build_network_endpoint('1.1.1.1', 80)
    service = _NetworkService(
        network_endpoint=endpoint,
        transport_protocol=network_pb2.TCP,
        service_name='http')
    target = _TargetInfo(network_endpoints=[endpoint])
    services = [service]
    request = plugin_service_pb2.RunRequest(
        target=target,
        plugins=[
            plugin_service_pb2.MatchedPlugin(
                services=services, plugin=plugin_to_test.GetPluginDefinition())
        ])

    rpc = self._server.invoke_unary_unary(_RunMethod, (), request, None)
    response, _, _, _ = rpc.termination()

    self.assertLen(response.reports.detection_reports, 1)
    self.assertEqual(
        plugin_to_test._BuildFakeDetectionReport(
            target=target, network_service=services[0]),
        response.reports.detection_reports[0])

  def test_run_no_plugins_registered_returns_empty_response(self):
    endpoint = _build_network_endpoint('1.1.1.1', 80)
    target = _TargetInfo(network_endpoints=[endpoint])
    request = plugin_service_pb2.RunRequest(target=target, plugins=[])

    rpc = self._server.invoke_unary_unary(_RunMethod, (), request, None)
    response, _, _, _ = rpc.termination()

    self.assertEmpty(response.reports.detection_reports)

  def test_list_plugins_plugins_registered_returns_valid_response(self):
    request = plugin_service.ListPluginsRequest()
    rpc = self._server.invoke_unary_unary(_ListPluginsMethod, (), request, None)
    response, _, _, _ = rpc.termination()
    self.assertEqual(
        plugin_service.ListPluginsResponse(
            plugins=[self.test_plugin.GetPluginDefinition()]), response)


def _build_network_endpoint(ip: str, port: int) -> _NetworkEndpoint:
  return _NetworkEndpoint(
      type=_NetworkEndpoint.IP,
      ip_address=network_pb2.IpAddress(address_family=_get_address_family(ip)),
      port=network_pb2.Port(port_number=port))


def _get_address_family(ip: str) -> _AddressFamily:
  inet_addr = ipaddr.IPAddress(ip)
  if inet_addr.version == 4:
    return _AddressFamily.IPV4
  elif inet_addr.version == 6:
    return _AddressFamily.IPV6
  else:
    raise ValueError('Unknown IP address family for IP \'%s\'' % ip)


class FakeVulnDetector(tsunami_plugin.VulnDetector):
  """Fake Vulnerability detector class for testing only."""

  def GetPluginDefinition(self):
    return tsunami_plugin.PluginDefinition(
        info=_PluginInfo(
            type=_PluginInfo.VULN_DETECTION,
            name='fake',
            version='v0.1',
            description='fake description',
            author='fake author'),
        target_service_name=plugin_representation_pb2.TargetServiceName(
            value=['fake service']),
        target_software=plugin_representation_pb2.TargetSoftware(
            name='fake software'),
        for_web_service=False)

  def Detect(self, target, matched_services):
    return detection_pb2.DetectionReportList(detection_reports=[
        self._BuildFakeDetectionReport(target, matched_services[0])
    ])

  def _BuildFakeDetectionReport(self, target, network_service):
    return detection_pb2.DetectionReport(
        target_info=target,
        network_service=network_service,
        detection_timestamp=timestamp_pb2.Timestamp(nanos=1234567890),
        detection_status=detection_pb2.VULNERABILITY_VERIFIED,
        vulnerability=vulnerability_pb2.Vulnerability(
            main_id=vulnerability_pb2.VulnerabilityId(
                publisher='GOOGLE', value='FakeVuln1'),
            severity=vulnerability_pb2.CRITICAL,
            title='FakeTitle1',
            description='FakeDescription1'))


# TODO(b/239628051): Add a failed VulnDetector class to test failed cases.

if __name__ == '__main__':
  absltest.main()
