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
"""Interface that all Python TsunamiPlugins will need to implement to run detection."""
import abc

from tsunami.proto import detection_pb2
from tsunami.proto import network_service_pb2
from tsunami.proto import plugin_representation_pb2
from tsunami.proto import reconnaissance_pb2

TargetInfo = reconnaissance_pb2.TargetInfo
NetworkService = network_service_pb2.NetworkService
DetectionReportList = detection_pb2.DetectionReportList
PluginDefinition = plugin_representation_pb2.PluginDefinition


class TsunamiPlugin(metaclass=abc.ABCMeta):

  @abc.abstractmethod
  def GetPluginDefinition(self) -> PluginDefinition:
    pass

  @classmethod
  def __subclasshook__(cls, subclass: abc.ABCMeta) -> bool:
    return (hasattr(subclass, 'GetPluginDefinition') and
            callable(subclass.GetPluginDefinition))


class VulnDetector(TsunamiPlugin):
  """A TsunamiPlugin that detects potential vulnerabilities on the target.

  Usually a vulnerability detector takes the information about an exposed
  network service, detects whether the service is vulnerable to a
  specific vulnerability, and reports the detection results.
  """

  @abc.abstractmethod
  def Detect(self, target: TargetInfo,
             matched_services: list[NetworkService]) -> DetectionReportList:
    """Run detection logic for the target.

    Args:
      target: Information about the scanning target itself.
      matched_services: A list of network services whose vulnerabilities could
        be detected by this plugin.

    Returns:
      A DetectionReportList for all the vulnerabilities of the scanning target.
    """
