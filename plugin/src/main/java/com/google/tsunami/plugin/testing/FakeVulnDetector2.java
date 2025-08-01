/*
 * Copyright 2020 Google LLC
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
import com.google.protobuf.util.Timestamps;
import com.google.tsunami.plugin.PluginType;
import com.google.tsunami.plugin.VulnDetector;
import com.google.tsunami.plugin.annotations.PluginInfo;
import com.google.tsunami.proto.DetectionReport;
import com.google.tsunami.proto.DetectionReportList;
import com.google.tsunami.proto.DetectionStatus;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.Severity;
import com.google.tsunami.proto.TargetInfo;
import com.google.tsunami.proto.Vulnerability;
import com.google.tsunami.proto.VulnerabilityId;

/** A fake VulnDetector plugin for testing purpose only. */
@PluginInfo(
    type = PluginType.VULN_DETECTION,
    name = "FakeVulnDetector2",
    version = "v0.1",
    description = "Another fake VulnDetector.",
    author = "fake",
    bootstrapModule = FakeVulnDetectorBootstrapModule2.class)
public class FakeVulnDetector2 implements VulnDetector {
  public static DetectionReport getFakeDetectionReport(
      TargetInfo targetInfo, NetworkService networkService) {
    return DetectionReport.newBuilder()
        .setTargetInfo(targetInfo)
        .setNetworkService(networkService)
        .setDetectionTimestamp(Timestamps.fromMillis(9876543210L))
        .setDetectionStatus(DetectionStatus.VULNERABILITY_VERIFIED)
        .setVulnerability(getAdvisoriesStatic().get(0))
        .build();
  }

  @Override
  public ImmutableList<Vulnerability> getAdvisories() {
    return getAdvisoriesStatic();
  }

  @Override
  public DetectionReportList detect(
      TargetInfo targetInfo, ImmutableList<NetworkService> matchedServices) {
    return DetectionReportList.newBuilder()
        .addAllDetectionReports(
            matchedServices.stream()
                .map(networkService -> getFakeDetectionReport(targetInfo, networkService))
                .collect(toImmutableList()))
        .build();
  }

  private static ImmutableList<Vulnerability> getAdvisoriesStatic() {
    return ImmutableList.of(
        Vulnerability.newBuilder()
            .setMainId(VulnerabilityId.newBuilder().setPublisher("GOOGLE").setValue("FakeVuln2"))
            .setSeverity(Severity.MEDIUM)
            .setTitle("FakeTitle2")
            .setDescription("FakeDescription2")
            .build());
  }
}
