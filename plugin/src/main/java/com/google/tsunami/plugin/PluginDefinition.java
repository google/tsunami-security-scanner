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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.tsunami.plugin.annotations.ForOperatingSystemClass;
import com.google.tsunami.plugin.annotations.ForServiceName;
import com.google.tsunami.plugin.annotations.ForSoftware;
import com.google.tsunami.plugin.annotations.ForWebService;
import com.google.tsunami.plugin.annotations.PluginInfo;
import com.google.tsunami.plugin.annotations.RequiresCallbackServer;
import java.util.Optional;

/** A data class that captures all the definition details about a {@link TsunamiPlugin}. */
@AutoValue
abstract class PluginDefinition {
  abstract PluginType type();

  abstract String name();

  abstract String author();

  abstract String version();

  abstract Optional<ForServiceName> targetServiceName();

  abstract Optional<ForSoftware> targetSoftware();

  abstract boolean isForWebService();

  abstract Optional<ForOperatingSystemClass> targetOperatingSystemClass();

  abstract boolean requiresCallbackServer();

  /**
   * Unique identifier for the plugin.
   *
   * @return a string representation of the plugin identifier.
   */
  @Memoized
  public String id() {
    return String.format("/%s/%s/%s/%s", author(), type(), name(), version());
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
    Optional<ForOperatingSystemClass> targetOperatingSystemClass =
        Optional.ofNullable(pluginClazz.getAnnotation(ForOperatingSystemClass.class));
    boolean requiresCallbackServer = pluginClazz.isAnnotationPresent(RequiresCallbackServer.class);

    checkState(
        pluginInfo.isPresent(),
        "A @PluginInfo annotation is required when creating a PluginDefinition for plugin: %s",
        pluginClazz);

    return new AutoValue_PluginDefinition(
        pluginInfo.get().type(),
        pluginInfo.get().name(),
        pluginInfo.get().author(),
        pluginInfo.get().version(),
        targetServiceName,
        targetSoftware,
        isForWebService,
        targetOperatingSystemClass,
        requiresCallbackServer);
  }

  /**
   * Factory method for creating a {@link PluginDefinition} for {@link RemoteVulnDetector}
   * implementations using the {@link PluginInfo} class.
   *
   * @param remotePluginInfo the {@link PluginInfo} of the remote Tsunami plugin
   * @return a {@link PluginDefinition} built from the plugin info.
   */
  public static PluginDefinition forRemotePlugin(PluginInfo remotePluginInfo) {
    checkNotNull(remotePluginInfo);
    return new AutoValue_PluginDefinition(
        remotePluginInfo.type(),
        remotePluginInfo.name(),
        remotePluginInfo.author(),
        remotePluginInfo.version(),
        Optional.empty(),
        Optional.empty(),
        false,
        Optional.empty(),
        false);
  }

  /**
   * Factory method for creating a {@link PluginDefinition} for dynamic plugins. A dynamic plugin is
   * a plugin that is created at runtime and for which we cannot rely on the PluginInfo annotation.
   *
   * <p>Note that for dynamic plugins, we drop the notion of version and forces it to be 1.0.
   *
   * @param pluginName the name of the plugin
   * @param pluginAuthor the author of the plugin
   * @param isForWebService whether the plugin is for web service
   * @param requiresCallbackServer whether the plugin requires a callback server
   * @return a {@link PluginDefinition} built from the plugin info.
   */
  public static PluginDefinition forDynamicPlugin(
      PluginType pluginType,
      String pluginName,
      String pluginAuthor,
      boolean isForWebService,
      boolean requiresCallbackServer) {
    return new AutoValue_PluginDefinition(
        pluginType,
        pluginName,
        pluginAuthor,
        "1.0",
        Optional.empty(),
        Optional.empty(),
        isForWebService,
        Optional.empty(),
        requiresCallbackServer);
  }
}
