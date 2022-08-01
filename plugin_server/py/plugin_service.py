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
"""Python gRPC PluginService server adapter to provide communication with the Java client."""

from concurrent import futures
from typing import cast

from absl import logging

from tsunami.plugin_server.py import tsunami_plugin
from tsunami.proto import detection_pb2
from tsunami.proto import plugin_representation_pb2
from tsunami.proto import plugin_service_pb2
from tsunami.proto import plugin_service_pb2_grpc

RunResponse = plugin_service_pb2.RunResponse
ListPluginsRequest = plugin_service_pb2.ListPluginsRequest
ListPluginsResponse = plugin_service_pb2.ListPluginsResponse
_PluginServiceServicer = plugin_service_pb2_grpc.PluginServiceServicer
_PluginType = plugin_representation_pb2.PluginInfo.PluginType

_DETECTION_TIMEOUT = 60


class PluginServiceServicer(plugin_service_pb2_grpc.PluginServiceServicer):
  """PluginService server implementation for communication with the Java client.

  This class executes requests called by the Java client. All request types are
  given by the plugin_service proto definition.

  """

  def __init__(self, py_plugins: list[tsunami_plugin.TsunamiPlugin],
               max_workers: int):
    self.py_plugins = py_plugins
    self.max_workers = max_workers

  def Run(
      self, request: plugin_service_pb2.RunRequest,
      servicer_context: plugin_service_pb2_grpc.PluginServiceServicer
  ) -> RunResponse:
    logging.info('Received Run request = %s', request)
    report_list = detection_pb2.DetectionReportList()

    detection_futures = []

    with futures.ThreadPoolExecutor(max_workers=self.max_workers) as executor:
      for matched_plugin in request.plugins:
        plugin_def = matched_plugin.plugin
        for plugin in self.py_plugins:
          if plugin.GetPluginDefinition() == plugin_def:
            logging.info('Running python plugin %s.', type(plugin).__name__)

            if plugin_def.info.type is _PluginType.VULN_DETECTION:
              plugin = cast(tsunami_plugin.VulnDetector, plugin)
              detection_futures.append(
                  executor.submit(plugin.Detect, request.target,
                                  matched_plugin.services))

    for detection in detection_futures:
      report_list.detection_reports.extend(
          detection.result(timeout=_DETECTION_TIMEOUT).detection_reports)

    response = RunResponse()
    response.reports.CopyFrom(report_list)
    return response

  def ListPlugins(
      self, request: ListPluginsRequest,
      servicer_context: _PluginServiceServicer) -> ListPluginsResponse:
    response = ListPluginsResponse()
    response.plugins.MergeFrom(
        [plugin.GetPluginDefinition() for plugin in self.py_plugins])
    return response
