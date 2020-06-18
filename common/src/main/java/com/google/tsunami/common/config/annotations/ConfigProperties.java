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
package com.google.tsunami.common.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for marking a Tsunami config object that can be initialized from external config
 * files, e.g. a {@code .yaml} file.
 *
 * <p>This annotation is required for any config object in order for Tsunami initialization logic to
 * identify and automatically populate config properties.
 *
 * Example usage:
 *
 * <pre>{@code
 * {@literal @}ConfigProperties("example.config.location")})
 * public class ExampleConfig {
 *   // ...
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigProperties {

  /**
   * The required prefix of the properties that should be bound to the annotated object.
   *
   * <p>A valid prefix is defined as dot separated words list (e.g. "plugin.example.abc"). Each dot
   * separated segment represents a section within the config file. For example, given a YAML file
   *
   * <pre>{@code
   * plugin:
   *   example:
   *     abc:
   *       fieldA: valueA
   *       fieldB: valueB
   *     xyz:
   *       fieldC: valueC
   * }</pre>
   *
   * value {@code "plugin.example.abc"} will select {@code fieldA} and {@code fieldB} for config
   * binding for the annotated class.
   *
   * @return the prefix of the config properties.
   */
  String value();
}
