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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.util.Types;
import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class RemoteVulnDetectorLoadingModuleTest {

  @SuppressWarnings("unchecked")
  private static final Key<Map<PluginDefinition, TsunamiPlugin>> PLUGIN_BINDING_KEY =
      (Key<Map<PluginDefinition, TsunamiPlugin>>)
          Key.get(Types.mapOf(PluginDefinition.class, TsunamiPlugin.class));

  private static String generateServerName() {
    return InProcessServerBuilder.generateName();
  }

  private static Channel generateChannel(String serverName) {
    return InProcessChannelBuilder.forName(serverName).directExecutor().build();
  }

  @Test
  public void configure_whenNoChannelsRegistered_loadsNoRemotePlugins() {
    Map<PluginDefinition, TsunamiPlugin> remotePlugins =
        Guice.createInjector(new RemoteVulnDetectorLoadingModule(ImmutableList.of()))
            .getInstance(PLUGIN_BINDING_KEY);

    assertThat(remotePlugins).isEmpty();
  }

  @Test
  public void configure_always_loadsAllRemotePlugins() {
    var channelName0 = generateChannel(generateServerName());
    var channelName1 = generateChannel(generateServerName());
    Map<PluginDefinition, TsunamiPlugin> remotePlugins =
        Guice.createInjector(
                new RemoteVulnDetectorLoadingModule(ImmutableList.of(channelName0, channelName1)))
            .getInstance(PLUGIN_BINDING_KEY);

    assertThat(remotePlugins).hasSize(2);
    assertThat(remotePlugins.keySet())
        .containsExactly(
            RemoteVulnDetectorLoadingModule.getRemoteVulnDetectorPluginDefinition(
                channelName0.hashCode()),
            RemoteVulnDetectorLoadingModule.getRemoteVulnDetectorPluginDefinition(
                channelName1.hashCode()));
  }
}
