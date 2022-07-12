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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.tsunami.proto.ListPluginsRequest;
import com.google.tsunami.proto.ListPluginsResponse;
import com.google.tsunami.proto.PluginServiceGrpc;
import com.google.tsunami.proto.PluginServiceGrpc.PluginServiceFutureStub;
import com.google.tsunami.proto.RunRequest;
import com.google.tsunami.proto.RunResponse;
import io.grpc.Channel;
import io.grpc.Deadline;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.health.v1.HealthGrpc.HealthFutureStub;

/**
 * Client side gRPC handler for the PluginService RPC protocol. Main handler for all gRPC calls to
 * the language-specific servers.
 */
public final class PluginServiceClient {

  private final HealthFutureStub healthService;
  private final PluginServiceFutureStub pluginService;

  PluginServiceClient(Channel channel) {
    this.healthService = HealthGrpc.newFutureStub(checkNotNull(channel));
    this.pluginService = PluginServiceGrpc.newFutureStub(checkNotNull(channel));
  }

  /**
   * Sends a run request to the gRPC language server with a specified deadline.
   *
   * @param request The main request containing plugins to run.
   * @param deadline The timeout of the service.
   * @return The future of the run response.
   */
  public ListenableFuture<RunResponse> runWithDeadline(RunRequest request, Deadline deadline) {
    return pluginService.withDeadline(deadline).run(request);
  }

  /**
   * Sends a list plugins request to the gRPC language server with a specified deadline.
   *
   * @param request The main request to notify the language server to send their plugins.
   * @param deadline The timeout of the service.
   * @return The future of the run response.
   */
  public ListenableFuture<ListPluginsResponse> listPluginsWithDeadline(
      ListPluginsRequest request, Deadline deadline) {
    return pluginService.withDeadline(deadline).listPlugins(request);
  }

  /**
   * Sends a health check request to retrieve the status of the language server.
   *
   * @param request The health check request to send to the language server.
   * @param deadline The maximum time to keep this call alive.
   * @return The language server's status via {@link HealthCheckResponse}.
   */
  public ListenableFuture<HealthCheckResponse> checkHealthWithDeadline(
      HealthCheckRequest request, Deadline deadline) {
    return healthService.withDeadline(deadline).check(request);
  }
}
