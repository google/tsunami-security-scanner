/*
 * Copyright 2020 Google LLC
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
import static org.junit.Assert.assertThrows;

import com.google.tsunami.plugin.annotations.PluginInfo;
import com.google.tsunami.plugin.testing.FakeRemoteVulnDetector;
import com.google.tsunami.plugin.testing.FakeVulnDetector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link PluginDefinition}. */
@RunWith(JUnit4.class)
public class PluginDefinitionTest {

  @Test
  public void id_always_generatesCorrectPluginId() {
    PluginDefinition pluginDefinition = PluginDefinition.forPlugin(FakeVulnDetector.class);

    assertThat(pluginDefinition.id()).isEqualTo("/fake/VULN_DETECTION/FakeVulnDetector/v0.1");
  }

  @Test
  public void forPlugin_whenPluginHasNoAnnotation_throwsException() {
    assertThrows(
        IllegalStateException.class, () -> PluginDefinition.forPlugin(NoAnnotationPlugin.class));
  }

  @Test
  public void forRemotePlugin_always_generatesCorrectDefinition() {
    PluginInfo pluginInfo = FakeRemoteVulnDetector.class.getAnnotation(PluginInfo.class);
    PluginDefinition pluginDefinition = PluginDefinition.forRemotePlugin(pluginInfo);

    assertThat(pluginDefinition.pluginInfo()).isEqualTo(pluginInfo);
    assertThat(pluginDefinition.id())
        .isEqualTo("/fake/REMOTE_VULN_DETECTION/FakeRemoteVulnDetector/v0.1");
  }

  @Test
  public void forRemotePlugin_whenPassedNull_throwsException() {
    assertThrows(NullPointerException.class, () -> PluginDefinition.forRemotePlugin(null));
  }

  private static final class NoAnnotationPlugin implements TsunamiPlugin {}
}
