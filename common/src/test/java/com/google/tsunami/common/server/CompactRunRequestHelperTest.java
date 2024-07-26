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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.tsunami.proto.Hostname;
import com.google.tsunami.proto.MatchedPlugin;
import com.google.tsunami.proto.NetworkEndpoint;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.PluginDefinition;
import com.google.tsunami.proto.PluginInfo;
import com.google.tsunami.proto.RunCompactRequest;
import com.google.tsunami.proto.RunCompactRequest.PluginNetworkServiceTarget;
import com.google.tsunami.proto.RunRequest;
import com.google.tsunami.proto.TargetInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class CompactRunRequestHelperTest {

  @Test
  public void compressingRunRequest_isMoreCompact() {
    NetworkService service1 = NetworkService.newBuilder().setServiceName("service1").build();
    NetworkService service2 = NetworkService.newBuilder().setServiceName("service2").build();
    PluginDefinition plugin1 =
        PluginDefinition.newBuilder()
            .setInfo(PluginInfo.newBuilder().setName("plugin1").build())
            .build();
    PluginDefinition plugin2 =
        PluginDefinition.newBuilder()
            .setInfo(PluginInfo.newBuilder().setName("plugin2").build())
            .build();
    PluginDefinition plugin3 =
        PluginDefinition.newBuilder()
            .setInfo(PluginInfo.newBuilder().setName("plugin3").build())
            .build();
    MatchedPlugin matchedPlugin1 =
        MatchedPlugin.newBuilder().addServices(service1).setPlugin(plugin1).build();
    MatchedPlugin matchedPlugin2 =
        MatchedPlugin.newBuilder().addServices(service2).setPlugin(plugin2).build();
    MatchedPlugin matchedPlugin3 =
        MatchedPlugin.newBuilder().addServices(service1).setPlugin(plugin3).build();
    ImmutableList<MatchedPlugin> expectedMatchedPlugins =
        ImmutableList.of(matchedPlugin1, matchedPlugin2, matchedPlugin3);
    TargetInfo expectedTargetInfo =
        TargetInfo.newBuilder()
            .addNetworkEndpoints(
                NetworkEndpoint.newBuilder()
                    .setHostname(Hostname.newBuilder().setName("example.com").build())
                    .build())
            .build();
    RunRequest expectedUncompressedRunRequest =
        RunRequest.newBuilder()
            .setTarget(expectedTargetInfo)
            .addAllPlugins(expectedMatchedPlugins)
            .build();
    var actualCompressedRunRequest =
        CompactRunRequestHelper.compress(expectedUncompressedRunRequest);

    var expectedCompressedRunRequest =
        RunCompactRequest.newBuilder()
            .setTarget(expectedTargetInfo)
            .addServices(service1)
            .addServices(service2)
            .addPlugins(plugin1)
            .addPlugins(plugin2)
            .addPlugins(plugin3)
            .addScanTargets(
                PluginNetworkServiceTarget.newBuilder()
                    .setPluginIndex(0)
                    .setServiceIndex(0)
                    .build())
            .addScanTargets(
                PluginNetworkServiceTarget.newBuilder()
                    .setPluginIndex(1)
                    .setServiceIndex(1)
                    .build())
            .addScanTargets(
                PluginNetworkServiceTarget.newBuilder()
                    .setPluginIndex(2)
                    .setServiceIndex(0)
                    .build())
            .build();
    assertThat(actualCompressedRunRequest).isEqualTo(expectedCompressedRunRequest);

    // And now uncompressing it again:
    var actualUncompressedRunRequest =
        CompactRunRequestHelper.uncompress(actualCompressedRunRequest);

    // It should match the original setup
    assertThat(actualUncompressedRunRequest).isEqualTo(expectedUncompressedRunRequest);
  }
}
