/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.tsunami.plugin.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.protobuf.util.Timestamps;
import com.google.tsunami.plugin.PluginType;
import com.google.tsunami.plugin.RemoteVulnDetector;
import com.google.tsunami.proto.DetectionReport;
import com.google.tsunami.proto.DetectionReportList;
import com.google.tsunami.proto.DetectionStatus;
import com.google.tsunami.proto.MatchedPlugin;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.PluginDefinition;
import com.google.tsunami.proto.PluginInfo;
import com.google.tsunami.proto.Severity;
import com.google.tsunami.proto.TargetInfo;
import com.google.tsunami.proto.Vulnerability;
import com.google.tsunami.proto.VulnerabilityId;
import java.util.Set;

/**
 * Fake {@link RemoteVulnDetector} implementation that only contains one {@link PluginDefinition}
 * proto available to run.
 */
@com.google.tsunami.plugin.annotations.PluginInfo(
    type = PluginType.REMOTE_VULN_DETECTION,
    name = "FakeRemoteVulnDetector",
    version = "v0.1",
    description = "fake description",
    author = "fake",
    bootstrapModule = FakeRemoteVulnDetectorBootstrapModule.class)
public final class FakeRemoteVulnDetector implements RemoteVulnDetector {

  private final Set<MatchedPlugin> matchedPluginsToRun;

  // Used when multiple instances of this {@link RemoteVulnDetector} are created.
  private final int fakePluginId;

  public FakeRemoteVulnDetector() {
    this(0);
  }

  public FakeRemoteVulnDetector(int fakePluginId) {
    this.fakePluginId = fakePluginId;
    this.matchedPluginsToRun = Sets.newHashSet();
  }

  @Override
  public DetectionReportList detect(TargetInfo target, ImmutableList<NetworkService> services) {
    ImmutableList<ImmutableList<DetectionReport>> detectionReports =
        matchedPluginsToRun.stream()
            .map(
                plugin ->
                    plugin.getServicesList().stream()
                        .map(service -> getFakeDetectionReport(target, service))
                        .collect(toImmutableList()))
            .collect(toImmutableList());
    var reportListBuilder = DetectionReportList.newBuilder();
    detectionReports.forEach(reportListBuilder::addAllDetectionReports);
    return reportListBuilder.build();
  }

  public static DetectionReport getFakeDetectionReport(
      TargetInfo targetInfo, NetworkService networkService) {
    return DetectionReport.newBuilder()
        .setTargetInfo(targetInfo)
        .setNetworkService(networkService)
        .setDetectionTimestamp(Timestamps.fromMillis(1234567890L))
        .setDetectionStatus(DetectionStatus.VULNERABILITY_VERIFIED)
        .setVulnerability(
            Vulnerability.newBuilder()
                .setMainId(
                    VulnerabilityId.newBuilder().setPublisher("GOOGLE").setValue("FakeRemoteVuln"))
                .setSeverity(Severity.CRITICAL)
                .setTitle("FakeTitle")
                .setDescription("FakeRemoteDescription"))
        .build();
  }

  @Override
  public ImmutableList<PluginDefinition> getAllPlugins() {
    return ImmutableList.of(
        PluginDefinition.newBuilder()
            .setInfo(
                PluginInfo.newBuilder()
                    .setType(PluginInfo.PluginType.VULN_DETECTION)
                    .setName("FakeRemoteVuln" + fakePluginId)
                    .setVersion("v0.1")
                    .setDescription("FakeRemoteDescription" + fakePluginId)
                    .setAuthor("fake"))
            .build());
  }

  @Override
  public void addMatchedPluginToDetect(MatchedPlugin plugin) {
    this.matchedPluginsToRun.add(plugin);
  }
}
