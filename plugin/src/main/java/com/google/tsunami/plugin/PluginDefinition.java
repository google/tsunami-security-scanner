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

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.tsunami.plugin.annotations.ForServiceName;
import com.google.tsunami.plugin.annotations.ForSoftware;
import com.google.tsunami.plugin.annotations.ForWebService;
import com.google.tsunami.plugin.annotations.PluginInfo;
import java.util.Optional;

/** A data class that captures all the definition details about a {@link TsunamiPlugin}. */
@AutoValue
abstract class PluginDefinition {
  abstract PluginInfo pluginInfo();
  abstract Optional<ForServiceName> targetServiceName();
  abstract Optional<ForSoftware> targetSoftware();
  abstract boolean isForWebService();

  /**
   * Unique identifier for the plugin.
   *
   * @return a string representation of the plugin identifier.
   */
  @Memoized
  public String id() {
    return String.format("/%s/%s/%s/%s", author(), type(), name(), version());
  }

  public PluginType type() {
    return pluginInfo().type();
  }

  public String name() {
    return pluginInfo().name();
  }

  public String author() {
    return pluginInfo().author();
  }

  public String version() {
    return pluginInfo().version();
  }

  /**
   * Factory method for creating a {@link PluginDefinition} from the {@link TsunamiPlugin} class.
   *
   * @param pluginClazz the {@link Class} of the Tsunami plugin
   * @return a {@link PluginDefinition} built from all the definition details about the plugin.
   */
  public static PluginDefinition forPlugin(Class<? extends TsunamiPlugin> pluginClazz) {
    Optional<PluginInfo> pluginInfo =
        Optional.ofNullable(pluginClazz.getAnnotation(PluginInfo.class));
    Optional<ForServiceName> targetServiceName =
        Optional.ofNullable(pluginClazz.getAnnotation(ForServiceName.class));
    Optional<ForSoftware> targetSoftware =
        Optional.ofNullable(pluginClazz.getAnnotation(ForSoftware.class));
    boolean isForWebService = pluginClazz.isAnnotationPresent(ForWebService.class);

    checkState(
        pluginInfo.isPresent(),
        "A @PluginInfo annotation is required when creating a PluginDefinition for plugin: %s",
        pluginClazz);

    return new AutoValue_PluginDefinition(
        pluginInfo.get(), targetServiceName, targetSoftware, isForWebService);
  }
}
