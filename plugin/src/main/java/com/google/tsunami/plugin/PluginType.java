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
package com.google.tsunami.plugin;

/** The types of a Tsunami plugin. */
public enum PluginType {
  /** A plugin that identifies the open ports of the scanning target. */
  PORT_SCAN,

  /**
   * A plugin that performs service specific fingerprints and identifies running software and
   * versions.
   */
  SERVICE_FINGERPRINT,

  /** A plugin that detects certain vulnerabilities on an exposed network service. */
  VULN_DETECTION,

  /** A plugin that contains vulnerability detectors from language servers. */
  REMOTE_VULN_DETECTION
}
