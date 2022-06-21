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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.tsunami.proto.PluginServiceGrpc;
import com.google.tsunami.proto.PluginServiceGrpc.PluginServiceFutureStub;
import io.grpc.Channel;

/**
 * Client side gRPC handler for the PluginService RPC protocol. Main handler for all gRPC calls to
 * the language-specific servers.
 */
public class PluginServiceClient {

  private final PluginServiceFutureStub pluginService;
  private final ListeningScheduledExecutorService scheduledExecutorService;

  PluginServiceClient(Channel channel, ListeningScheduledExecutorService service) {
    this.pluginService = PluginServiceGrpc.newFutureStub(checkNotNull(channel));
    this.scheduledExecutorService = checkNotNull(service);
  }
}
