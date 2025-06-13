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
import com.google.tsunami.common.server.CompactRunRequestHelper;
import com.google.tsunami.proto.DetectionReportList;
import com.google.tsunami.proto.ListPluginsRequest;
import com.google.tsunami.proto.MatchedPlugin;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.PluginDefinition;
import com.google.tsunami.proto.RunRequest;
import com.google.tsunami.proto.RunResponse;
import com.google.tsunami.proto.TargetInfo;
import com.google.tsunami.proto.Vulnerability;
import io.grpc.Channel;
import io.grpc.Deadline;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** Facilitates communication with remote detectors. */
public final class RemoteVulnDetectorImpl implements RemoteVulnDetector {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  // Default duration deadline for the detect() RPC call
  // Remote detectors, especially ones using the callback server, require additional buffer to send
  // requests and responses.
  private static final Duration DEFAULT_DEADLINE_DETECT = Duration.ofSeconds(150);
  // For all other operations, including health check:
  private static final Duration DEFAULT_DEADLINE = Duration.ofSeconds(10);

  private final PluginServiceClient service;
  private final Set<MatchedPlugin> pluginsToRun;
  private final ExponentialBackOff backoff;
  private final int maxAttempts;
  private final Duration detectDeadline;
  private boolean wantCompactRunRequest = false;

  RemoteVulnDetectorImpl(
      Channel channel, ExponentialBackOff backoff, int maxAttempts, Duration detectDeadline) {
    this.service = new PluginServiceClient(checkNotNull(channel));
    this.pluginsToRun = Sets.newHashSet();
    this.backoff = backoff;
    this.maxAttempts = maxAttempts;
    this.detectDeadline = detectDeadline != null ? detectDeadline : DEFAULT_DEADLINE_DETECT;
  }

  @Override
  public DetectionReportList detect(
      TargetInfo target, ImmutableList<NetworkService> matchedServices) {
    try {
      if (checkHealthWithBackoffs()) {
        var runRequest =
            RunRequest.newBuilder().setTarget(target).addAllPlugins(pluginsToRun).build();
        logger.atInfo().log("Detecting with language server plugins...");
        RunResponse runResponse;
        if (this.wantCompactRunRequest) {
          var runCompactRequest = CompactRunRequestHelper.compress(runRequest);
          runResponse =
              service
                  .runCompactWithDeadline(
                      runCompactRequest, Deadline.after(detectDeadline.toSeconds(), SECONDS))
                  .get();
        } else {
          runResponse =
              service
                  .runWithDeadline(runRequest, Deadline.after(detectDeadline.toSeconds(), SECONDS))
                  .get();
        }
        return runResponse.getReports();
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new LanguageServerException("Failed to get response from language server.", e);
    }
    return DetectionReportList.getDefaultInstance();
  }

  @Override
  public ImmutableList<Vulnerability> getAdvisories() {
    // TODO: b/422968545 - The remote detectors also need to support getAdvisories().
    return ImmutableList.of();
  }

  @Override
  public ImmutableList<PluginDefinition> getAllPlugins() {
    try {
      if (checkHealthWithBackoffs()) {
        logger.atInfo().log("Getting language server plugins...");
        var listPluginsResponse =
            service
                .listPluginsWithDeadline(
                    ListPluginsRequest.getDefaultInstance(),
                    Deadline.after(DEFAULT_DEADLINE.toSeconds(), SECONDS))
                .get();
        // Note: each plugin service client has a dedicated RemoteVulnDetectorImpl instance,
        // so we can safely set this flag here.
        this.wantCompactRunRequest = listPluginsResponse.getWantCompactRunRequest();
        return ImmutableList.copyOf(listPluginsResponse.getPluginsList());
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
    while (attempt < maxAttempts) {
      try {
        var healthy =
            service
                .checkHealthWithDeadline(
                    HealthCheckRequest.getDefaultInstance(),
                    Deadline.after(DEFAULT_DEADLINE.toSeconds(), SECONDS))
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
        if (attempt == maxAttempts) {
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
