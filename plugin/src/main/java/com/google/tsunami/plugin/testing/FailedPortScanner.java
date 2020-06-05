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
import com.google.tsunami.plugin.PortScanner;
import com.google.tsunami.plugin.annotations.PluginInfo;
import com.google.tsunami.proto.PortScanningReport;
import com.google.tsunami.proto.ScanTarget;

/** A fake PortScanner plugin that instantly fails for testing purpose only. */
@PluginInfo(
    type = PluginType.PORT_SCAN,
    name = "FailedPortScanner",
    version = "v0.1",
    description = "A fake PortScanner that instantly fails.",
    author = "fake",
    bootstrapModule = FailedPortScannerBootstrapModule.class)
public final class FailedPortScanner implements PortScanner {

  @Override
  public PortScanningReport scan(ScanTarget scanTarget) {
    throw new RuntimeException("PortScanner failed");
  }
}
