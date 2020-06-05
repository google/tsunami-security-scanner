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

import com.google.common.collect.ImmutableList;
import com.google.tsunami.proto.DetectionReportList;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.TargetInfo;

/**
 * A {@link TsunamiPlugin} that detects potential vulnerabilities on the target.
 *
 * <p>Usually a vulnerability detector takes the information about an exposed network service,
 * detects whether the service is vulnerable to a specific vulnerability, and reports the detection
 * results.
 */
public interface VulnDetector extends TsunamiPlugin {

  /**
   * Performs the detection task for specific vulnerabilities.
   *
   * @param targetInfo information about the scanning target itself
   * @param matchedServices a list of network services whose vulnerabilities could be detected by
   *     this plugin
   * @return a {@link DetectionReportList} for all the vulnerabilities of the scanning target.
   */
  DetectionReportList detect(
      TargetInfo targetInfo, ImmutableList<NetworkService> matchedServices);
}
