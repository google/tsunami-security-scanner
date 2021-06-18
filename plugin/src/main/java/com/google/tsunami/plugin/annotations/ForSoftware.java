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
package com.google.tsunami.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for marking the target software and target version for a Tsunami {@link
 * com.google.tsunami.plugin.VulnDetector} plugin.
 *
 * <p>If annotated by this annotation, the {@link com.google.tsunami.plugin.VulnDetector} will only
 * be executed by the scanner when the scan target is running the matching software behind a network
 * service.
 *
 * Example usage:
 *
 * <pre>{@code
 * {@literal @}ForSoftware(
 *   name = "WordPress",
 *   versions = {
 *     "0.8",
 *     "0.9",
 *     "[1.3,2.0)"
 *   }
 * )
 * public class ExamplePlugin implements VulnDetector {
 *   // ...
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ForSoftware {

  /**
   * Name of the target software, case insensitive.
   *
   * @return target software name.
   */
  // TODO(b/145315535): handle name conflicts, include other properties that uniquely identify
  // software.
  String name();

  /**
   * Array of versions and version ranges of the target software.
   *
   * <p>Some version and version range examples are:
   *
   * <ul>
   *   <li><code>1.0</code> Version 1.0.
   *   <li><code>[1.0,2.0)</code> Version 1.0 (inclusive) to 2.0 (exclusive).
   *   <li><code>[1.0,2.0]</code> Version 1.0 to 2.0 (both inclusive).
   *   <li><code>[1.0,)</code> Version 1.0 (inclusive) and higher.
   *   <li><code>(,1.0]</code> Version 1.0 (inclusive) and lower.
   * </ul>
   *
   * Example value for this field:
   *
   * <ul>
   *   <li><code>[ 1.0 ]</code>
   *   <li><code>[ 1.0, [1.1, 1.5) ]</code>
   *   <li><code>[ 1.0, [1.1, 1.5), 1.7, 1.8 ]</code>
   *   <li><code>[ (,1.0], [1.1, 1.3), [1.4,) ]</code>
   * </ul>
   *
   * @return target software versions.
   */
  String[] versions() default {};
}
