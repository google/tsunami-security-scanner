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

import com.google.tsunami.proto.FingerprintingReport;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.TargetInfo;

/**
 * A {@link TsunamiPlugin} that performs the service fingerprinting tasks.
 *
 * <p>A fingerprinter usually performs service specific fingerprinting jobs to better understand an
 * exposed service. For example, a web fingerprinter would help the scanner identify which web
 * applications and their corresponding versions are exposed under port 80 or 443.
 *
 * <p>NOTE: ServiceFingerprinters must be annotated by the {@link
 * com.google.tsunami.plugin.annotations.ForServiceName} annotation.
 */
public interface ServiceFingerprinter extends TsunamiPlugin {

  /**
   * Performs the service fingerprinting job on the given {@code targetInfo} and {@code
   * networkService}.
   *
   * @param targetInfo information about the target to be scanned.
   * @param networkService information about the specific network service to be fingerprinted.
   * @return a {@link FingerprintingReport} that captures all the details about the targeted
   *     service.
   */
  FingerprintingReport fingerprint(TargetInfo targetInfo, NetworkService networkService);
}
