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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.tsunami.proto.PluginServiceGrpc;
import com.google.tsunami.proto.PluginServiceGrpc.PluginServiceFutureStub;
import com.google.tsunami.proto.RunRequest;
import com.google.tsunami.proto.RunResponse;
import io.grpc.Channel;
import java.time.Duration;

/**
 * Client side gRPC handler for the PluginService RPC protocol. Main handler for all gRPC calls to
 * the language-specific servers.
 */
public final class PluginServiceClient {

  private final PluginServiceFutureStub pluginService;
  private final ListeningScheduledExecutorService scheduledExecutorService;

  PluginServiceClient(Channel channel, ListeningScheduledExecutorService service) {
    this.pluginService = PluginServiceGrpc.newFutureStub(checkNotNull(channel));
    this.scheduledExecutorService = checkNotNull(service);
  }

  /**
   * Sends a run request to the gRPC language server with a specified deadline.
   *
   * @param request The main request containing plugins to run.
   * @param deadline The timeout of the service.
   * @return The future of the run response.
   */
  public ListenableFuture<RunResponse> runWithDeadline(RunRequest request, Duration deadline) {
    return Futures.withTimeout(pluginService.run(request), deadline, scheduledExecutorService);
  }
}
