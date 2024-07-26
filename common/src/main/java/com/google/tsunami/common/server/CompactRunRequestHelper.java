/*
 * Copyright 2024 Google LLC
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
package com.google.tsunami.common.server;

import com.google.common.collect.ImmutableList;
import com.google.tsunami.proto.MatchedPlugin;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.RunCompactRequest;
import com.google.tsunami.proto.RunCompactRequest.PluginNetworkServiceTarget;
import com.google.tsunami.proto.RunRequest;
import java.util.HashMap;

/**
 * CompactRunRequestHelper is a helper class to compress/uncompress the RunRequest into/from the
 * compact representation.
 */
public final class CompactRunRequestHelper {

  private CompactRunRequestHelper() {}

  public static RunCompactRequest compress(RunRequest runRequest) {
    var builder = RunCompactRequest.newBuilder().setTarget(runRequest.getTarget());
    HashMap<NetworkService, Integer> serviceIndexMap = new HashMap<>();
    int pluginIndex = -1;
    for (MatchedPlugin matchedPlugin : runRequest.getPluginsList()) {
      pluginIndex++;
      builder.addPlugins(matchedPlugin.getPlugin());
      for (NetworkService service : matchedPlugin.getServicesList()) {
        Integer serviceIndex = serviceIndexMap.get(service);
        if (serviceIndex == null) {
          serviceIndex = serviceIndexMap.size();
          serviceIndexMap.put(service, serviceIndex);
          builder.addServices(service);
        }

        builder.addScanTargets(
            PluginNetworkServiceTarget.newBuilder()
                .setPluginIndex(pluginIndex)
                .setServiceIndex(serviceIndex)
                .build());
      }
    }
    return builder.build();
  }

  public static RunRequest uncompress(RunCompactRequest runCompactRequest) {
    ImmutableList.Builder<MatchedPlugin> matchedPlugins = ImmutableList.builder();
    for (var target : runCompactRequest.getScanTargetsList()) {
      var plugin = runCompactRequest.getPlugins(target.getPluginIndex());
      var networkService = runCompactRequest.getServices(target.getServiceIndex());
      matchedPlugins.add(
          MatchedPlugin.newBuilder().setPlugin(plugin).addServices(networkService).build());
    }

    return RunRequest.newBuilder()
        .setTarget(runCompactRequest.getTarget())
        .addAllPlugins(matchedPlugins.build())
        .build();
  }
}
