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

import com.google.common.collect.ImmutableList;
import com.google.tsunami.plugin.PluginType;
import com.google.tsunami.plugin.VulnDetector;
import com.google.tsunami.plugin.annotations.PluginInfo;
import com.google.tsunami.proto.DetectionReportList;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.TargetInfo;
import com.google.tsunami.proto.Vulnerability;

/** A fake VulnDetector plugin that instantly fails for testing purpose only. */
@PluginInfo(
    type = PluginType.VULN_DETECTION,
    name = "FailedVulnDetector",
    version = "v0.1",
    description = "A fake VulnDetector that instantly fails.",
    author = "fake",
    bootstrapModule = FailedVulnDetectorBootstrapModule.class)
public class FailedVulnDetector implements VulnDetector {

  @Override
  public ImmutableList<Vulnerability> getAdvisories() {
    return ImmutableList.of();
  }

  @Override
  public DetectionReportList detect(
      TargetInfo targetInfo, ImmutableList<NetworkService> matchedServices) {
    throw new RuntimeException("VulnDetector failed.");
  }
}
