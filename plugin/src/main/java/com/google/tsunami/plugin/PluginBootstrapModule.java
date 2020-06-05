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

import com.google.common.flogger.GoogleLogger;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

/**
 * Base class for bootstrapping a {@link TsunamiPlugin}.
 *
 * <p>A valid {@link PluginBootstrapModule} subclass must be defined for each {@link TsunamiPlugin}.
 * This is enforced by the {@link com.google.tsunami.plugin.annotations.PluginInfo} annotation.
 */
public abstract class PluginBootstrapModule extends AbstractModule {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private MapBinder<PluginDefinition, TsunamiPlugin> tsunamiPluginBinder;

  @Override
  protected final void configure() {
    tsunamiPluginBinder =
        MapBinder.newMapBinder(binder(), PluginDefinition.class, TsunamiPlugin.class);
    configurePlugin();
  }

  /**
   * All bootstrapping logic for a {@link TsunamiPlugin} should be implemented in this method.
   * {@link PluginBootstrapModule#registerPlugin(Class)} must also be called in order to register
   * the plugin to Tsunami.
   */
  protected abstract void configurePlugin();

  /**
   * Register a {@link TsunamiPlugin} to Tsunami's plugin module using Guice's multibinding feature.
   *
   * @param tsunamiPluginClazz the {@link Class} for the {@link TsunamiPlugin} to be registered.
   */
  protected final void registerPlugin(Class<? extends TsunamiPlugin> tsunamiPluginClazz) {
    checkNotNull(tsunamiPluginClazz);

    tsunamiPluginBinder
        .addBinding(PluginDefinition.forPlugin(tsunamiPluginClazz))
        .to(tsunamiPluginClazz);
    logger.atInfo().log("Plugin %s is registered.", tsunamiPluginClazz);
  }
}
