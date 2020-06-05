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
package com.google.tsunami.plugin.testing;

import com.google.tsunami.plugin.PluginType;
import com.google.tsunami.plugin.ServiceFingerprinter;
import com.google.tsunami.plugin.annotations.ForServiceName;
import com.google.tsunami.plugin.annotations.PluginInfo;
import com.google.tsunami.proto.FingerprintingReport;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.Software;
import com.google.tsunami.proto.TargetInfo;

/** A fake ServiceFingerprinter plugin for testing purpose only. */
@PluginInfo(
    type = PluginType.SERVICE_FINGERPRINT,
    name = "FakeServiceFingerprinter",
    version = "v0.1",
    description = "A fake ServiceFingerprinter.",
    author = "fake",
    bootstrapModule = FakeServiceFingerprinterBootstrapModule.class)
@ForServiceName("http")
public class FakeServiceFingerprinter implements ServiceFingerprinter {
  public static final Software IDENTIFIED_SOFTWARE =
      Software.newBuilder().setName("Jenkins").build();

  @Override
  public FingerprintingReport fingerprint(TargetInfo targetInfo, NetworkService networkService) {
    return FingerprintingReport.newBuilder()
        .addNetworkServices(networkService.toBuilder().setSoftware(IDENTIFIED_SOFTWARE))
        .build();
  }
}
