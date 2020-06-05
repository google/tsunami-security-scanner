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

import com.google.tsunami.proto.PortScanningReport;
import com.google.tsunami.proto.ScanTarget;

/**
 * A {@link TsunamiPlugin} that performs the port scanning tasks.
 *
 * <p>A port scanner in general should perform network probing to identify network services (active
 * ports) running on a target and/or gather any interesting information about the scanning target
 * like its architecture, operating systems, etc.
 */
public interface PortScanner extends TsunamiPlugin {

  /**
   * Performs the port scan on the given {@code scanTarget}.
   *
   * @param scanTarget the target to be scanned.
   * @return a {@link PortScanningReport} that captures the full port scanning results.
   */
  PortScanningReport scan(ScanTarget scanTarget);
}
