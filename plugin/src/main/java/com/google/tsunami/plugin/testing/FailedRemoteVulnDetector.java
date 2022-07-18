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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.tsunami.plugin.LanguageServerException;
import com.google.tsunami.plugin.PluginType;
import com.google.tsunami.plugin.RemoteVulnDetector;
import com.google.tsunami.plugin.annotations.PluginInfo;
import com.google.tsunami.proto.DetectionReportList;
import com.google.tsunami.proto.MatchedPlugin;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.PluginDefinition;
import com.google.tsunami.proto.TargetInfo;
import java.util.Set;

/** Fake {@link RemoteVulnDetector} implementation that fails to run. */
@PluginInfo(
    type = PluginType.REMOTE_VULN_DETECTION,
    name = "FailedRemoteVulnDetector",
    version = "v0.1",
    description = "fake description",
    author = "fake",
    bootstrapModule = FailedRemoteVulnDetectorBootstrapModule.class)
public final class FailedRemoteVulnDetector implements RemoteVulnDetector {

  private final Set<MatchedPlugin> matchedPluginsToRun;

  public FailedRemoteVulnDetector() {
    this.matchedPluginsToRun = Sets.newHashSet();
  }

  @Override
  public DetectionReportList detect(TargetInfo target, ImmutableList<NetworkService> services) {
    throw new LanguageServerException("RemoteVulnDetector failed.");
  }

  @Override
  public ImmutableList<PluginDefinition> getAllPlugins() {
    return ImmutableList.of(PluginDefinition.getDefaultInstance());
  }

  @Override
  public void addMatchedPluginToDetect(MatchedPlugin plugin) {
    this.matchedPluginsToRun.add(plugin);
  }
}
