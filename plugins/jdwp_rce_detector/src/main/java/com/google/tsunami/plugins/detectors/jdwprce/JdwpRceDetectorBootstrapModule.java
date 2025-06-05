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
package com.google.tsunami.plugins.detectors.jdwprce;

import com.google.tsunami.plugin.PluginBootstrapModule;

/** A Guice module that bootstraps the {@link JdwpRceDetector}. */
public final class JdwpRceDetectorBootstrapModule extends PluginBootstrapModule {

  @Override
  protected void configurePlugin() {
    // Tsunami relies heavily on Guice (https://github.com/google/guice). All Guice bindings for
    // your plugin should be implemented here.

    // registerPlugin method is required in order for the Tsunami scanner to identify your plugin.
    registerPlugin(JdwpRceDetector.class);
  }
}
