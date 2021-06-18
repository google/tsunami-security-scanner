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
 * An annotation for marking the target service name(s) for a Tsunami {@link
 * com.google.tsunami.plugin.VulnDetector} plugin.
 *
 * <p>If annotated by this annotation, the {@link com.google.tsunami.plugin.VulnDetector} will only
 * be executed by the scanner when the scan target exposed a network service whose name matches the
 * listed service name in the annotation. Network service names should genuinely follow those listed
 * at RFC6335.
 *
 * Example usage:
 *
 * <pre>{@code
 * {@literal @}ForServiceName({"http", "https"})
 * public class ExamplePlugin implements VulnDetector {
 *   // ...
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ForServiceName {
  /**
   * Array of target network service names for a Tsunami {@link
   * com.google.tsunami.plugin.VulnDetector} plugin. The values for application layer protocols
   * should genuinely follow naming conventions listed at RFC6335.
   *
   * @return the targeted network service names for a Tsunami plugin.
   */
  String[] value();
}
