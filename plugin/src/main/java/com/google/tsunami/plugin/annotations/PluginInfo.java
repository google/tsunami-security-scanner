/*
 * Copyright 2019 Google LLC
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
package com.google.tsunami.plugin.annotations;

import com.google.tsunami.plugin.PluginBootstrapModule;
import com.google.tsunami.plugin.PluginType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for adding related information about a Tsunami plugin.
 *
 * Example usage:
 *
 * <pre>{@code
 * {@literal @}PluginInfo(
 *   type = PluginType.VULN_DETECTOR,
 *   name = "example_plugin",
 *   version = "1.0",
 *   description = "An example plugin that demonstrates the usage of the PluginInfo annotation",
 *   author = "Author A (a@example.com)",
 *   bootstrapModule = ExamplePluginBootstrapModule.class})
 * public class ExamplePlugin implements VulnDetector {
 *   // ...
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PluginInfo {
  /**
   * The type of this plugin.
   *
   * @return Tsunami plugin type.
   */
  PluginType type();

  /**
   * The short name of this plugin.
   *
   * <p><b>Convention:</b> use {@code lower_snake_case} style (lowercase words separated by
   * underscores) for this name.
   *
   * @return Tsunami plugin name.
   */
  // TODO(magl): add compile time style validation.
  String name();

  /**
   * The version of this plugin.
   *
   * <p><b>Convention:</b> follow the {@code major.minor} version scheme.
   *
   * @return Tsunami plugin version.
   */
  // TODO(magl): add compile time style validation.
  String version();

  /**
   * Detailed description of this plugin.
   *
   * <p>In general, the description of a plugin should tell the purpose of this plugin. For example,
   * for a {@code VULN_DETECTOR}, the description should mention the affected software,
   * vulnerability, affected version ranges, etc.
   *
   * @return Tsunami plugin description.
   */
  String description();

  /**
   * Author of this plugin.
   *
   * <p><b>Convention:</b> "Name (contact email)". For example, {@code Alice (Alice@example.com)}.
   *
   * @return Tsunami plugin author.
   */
  // TODO(magl): add compile time style validation.
  String author();

  /**
   * Module for bootstrapping this plugin.
   *
   * @return Tsunami plugin's bootstrap module class.
   */
  Class<? extends PluginBootstrapModule> bootstrapModule();
}
