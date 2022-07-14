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

import com.google.common.collect.ImmutableList;
import com.google.tsunami.proto.MatchedPlugin;
import com.google.tsunami.proto.PluginDefinition;

/**
 * A special {@link VulnDetector} to execute vulnerability detector plugins from their specified
 * language server.
 */
public interface RemoteVulnDetector extends VulnDetector {

  /**
   * Retrieve all plugins from the language server through an RPC call.
   *
   * @return List of all plugin definitions. If the language server throws an error, an empty list
   *     will be returned.
   */
  ImmutableList<PluginDefinition> getAllPlugins();

  /**
   * Add a {@link MatchedPlugin} to allow this {@link RemoteVulnDetector} to run detection for this
   * plugin through the language server.
   *
   * @param pluginToRun The plugin to allow this {@link RemoteVulnDetector} to run.
   */
  void addMatchedPluginToDetect(MatchedPlugin pluginToRun);
}
