/*
 * Copyright 2025 Google LLC
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
package com.google.tsunami.workflow;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.flogger.GoogleLogger;
import com.google.protobuf.TextFormat;
import com.google.tsunami.plugin.PluginManager;
import com.google.tsunami.proto.VulnerabilityList;
import java.io.IOException;
import java.io.PrintWriter;
import javax.inject.Inject;

/** Workflow for dumping advisories. */
public final class AdvisoriesWorkflow {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private final PluginManager pluginManager;

  @Inject
  AdvisoriesWorkflow(PluginManager pluginManager) {
    this.pluginManager = checkNotNull(pluginManager);
  }

  public Void run(String path) {
    logger.atInfo().log("Dumping advisories to %s", path);

    var vulnerabilityList = buildVulnerabilityList();
    var vulnerabilityText = TextFormat.printer().printToString(vulnerabilityList);

    try (PrintWriter writer = new PrintWriter(path, UTF_8.name())) {
      writer.write(vulnerabilityText);
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to dump advisories to %s", path);
    }

    return null;
  }

  private VulnerabilityList buildVulnerabilityList() {
    var vulnerabilities =
        pluginManager.getAllVulnDetectors().stream()
            .flatMap(plugin -> plugin.getAdvisories().stream())
            .collect(toImmutableList());

    return VulnerabilityList.newBuilder().addAllVulnerabilities(vulnerabilities).build();
  }
}
