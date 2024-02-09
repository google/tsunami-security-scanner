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

import com.google.api.client.util.BackOff;
import com.google.api.client.util.ExponentialBackOff;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.flogger.GoogleLogger;
import com.google.common.util.concurrent.Uninterruptibles;
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
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** Facilitates communication with remote detectors. */
public final class RemoteVulnDetectorImpl implements RemoteVulnDetector {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  // Default duration deadline for all RPC calls
  // Remote detectors, especially ones using the callback server, require additional buffer to send
  // requests and responses.
  private static final Deadline DEFAULT_DEADLINE = Deadline.after(150, SECONDS);
  private static final int INITIAL_WAIT_TIME_MS = 200;
  private static final int MAX_WAIT_TIME_MS = 30000;
  private static final int WAIT_TIME_MULTIPLIER = 3;
  private static final int MAX_ATTEMPTS = 3;
  private final ExponentialBackOff backoff =
      new ExponentialBackOff.Builder()
          .setInitialIntervalMillis(INITIAL_WAIT_TIME_MS)
          .setRandomizationFactor(0.1)
          .setMultiplier(WAIT_TIME_MULTIPLIER)
          .setMaxElapsedTimeMillis(MAX_WAIT_TIME_MS)
          .build();
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
      if (checkHealthWithBackoffs()) {
        logger.atInfo().log("Detecting with language server plugins...");
        return service
            .runWithDeadline(
                RunRequest.newBuilder().setTarget(target).addAllPlugins(pluginsToRun).build(),
                DEFAULT_DEADLINE)
            .get()
            .getReports();
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new LanguageServerException("Failed to get response from language server.", e);
    }
    return DetectionReportList.getDefaultInstance();
  }

  @Override
  public ImmutableList<PluginDefinition> getAllPlugins() {
    try {
      if (checkHealthWithBackoffs()) {
        logger.atInfo().log("Getting language server plugins...");
        return ImmutableList.copyOf(
            service
                .listPluginsWithDeadline(ListPluginsRequest.getDefaultInstance(), DEFAULT_DEADLINE)
                .get()
                .getPluginsList());
      } else {
        return ImmutableList.of();
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new LanguageServerException("Failed to get response from language server.", e);
    }
  }

  private boolean checkHealthWithBackoffs() {
    // After starting the language server, this is our first attempt to establish a connection
    // between the Java and the language server.
    // Sometimes the language server may need longer time to ramp up its health service, so we need
    // to implement exponential retries to manage those circumstances.
    backoff.reset();
    int attempt = 0;
    while (attempt < MAX_ATTEMPTS) {
      try {
        var healthy =
            service
                .checkHealthWithDeadline(HealthCheckRequest.getDefaultInstance(), DEFAULT_DEADLINE)
                .get()
                .getStatus()
                .equals(HealthCheckResponse.ServingStatus.SERVING);
        if (!healthy) {
          logger.atWarning().log("Language server is not serving.");
        }
        return healthy;
      } catch (InterruptedException | ExecutionException e) {
        attempt++;
        try {
          long backOffMillis = backoff.nextBackOffMillis();
          if (backOffMillis != BackOff.STOP) {
            Uninterruptibles.sleepUninterruptibly(backOffMillis, TimeUnit.MILLISECONDS);
          }
        } catch (IOException ioe) {
          // ignore
          logger.atWarning().log("Failed to sleep for %s", ioe.getCause().getMessage());
        }
        if (attempt == MAX_ATTEMPTS) {
          throw new LanguageServerException("Language service is not registered.", e.getCause());
        }
      }
    }
    return false;
  }

  @Override
  public void addMatchedPluginToDetect(MatchedPlugin plugin) {
    this.pluginsToRun.add(plugin);
  }
}
