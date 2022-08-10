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
package com.google.tsunami.plugin;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.flogger.GoogleLogger;
import com.google.tsunami.proto.DetectionReportList;
import com.google.tsunami.proto.ListPluginsRequest;
import com.google.tsunami.proto.MatchedPlugin;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.PluginDefinition;
import com.google.tsunami.proto.RunRequest;
import com.google.tsunami.proto.TargetInfo;
import io.grpc.Channel;
import io.grpc.Deadline;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import java.util.Set;
import java.util.concurrent.ExecutionException;

final class RemoteVulnDetectorImpl implements RemoteVulnDetector {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  // Default duration deadline for all RPC calls
  private static final Deadline DEFAULT_DEADLINE = Deadline.after(120, SECONDS);

  private final PluginServiceClient service;
  private final Set<MatchedPlugin> pluginsToRun;

  RemoteVulnDetectorImpl(Channel channel) {
    this.service = new PluginServiceClient(checkNotNull(channel));
    this.pluginsToRun = Sets.newHashSet();
  }

  @Override
  public DetectionReportList detect(
      TargetInfo target, ImmutableList<NetworkService> matchedServices) {
    try {
      if (service
          .checkHealthWithDeadline(HealthCheckRequest.getDefaultInstance(), DEFAULT_DEADLINE)
          .get()
          .getStatus()
          .equals(HealthCheckResponse.ServingStatus.SERVING)) {
        return service
            .runWithDeadline(
                RunRequest.newBuilder().setTarget(target).addAllPlugins(pluginsToRun).build(),
                DEFAULT_DEADLINE)
            .get()
            .getReports();
      } else {
        logger.atWarning().log(
            "Server health status is not SERVING. Will not run matched plugins.");
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new LanguageServerException("Failed to get response from language server.", e);
    }
    return DetectionReportList.getDefaultInstance();
  }

  @Override
  public ImmutableList<PluginDefinition> getAllPlugins() {
    try {
      if (service
          .checkHealthWithDeadline(HealthCheckRequest.getDefaultInstance(), DEFAULT_DEADLINE)
          .get()
          .getStatus()
          .equals(HealthCheckResponse.ServingStatus.SERVING)) {
        return ImmutableList.copyOf(
            service
                .listPluginsWithDeadline(ListPluginsRequest.getDefaultInstance(), DEFAULT_DEADLINE)
                .get()
                .getPluginsList());
      } else {
        logger.atWarning().log("Server health status is not SERVING. Will not retrieve plugins.");
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new LanguageServerException("Failed to get plugins from language server.", e);
    }
    return ImmutableList.of();
  }

  @Override
  public void addMatchedPluginToDetect(MatchedPlugin plugin) {
    this.pluginsToRun.add(plugin);
  }
}
