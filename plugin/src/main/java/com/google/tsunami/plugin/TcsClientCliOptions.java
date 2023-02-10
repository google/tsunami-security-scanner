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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.tsunami.common.cli.CliOption;

/** Command line arguments for the Tsunami callbackserver. */
@Parameters(separators = "=")
public final class TcsClientCliOptions implements CliOption {
  @Parameter(
      names = "--callback-address",
      description = "Address (ip or domain) of TCS http service.")
  public String callbackAddress;

  @Parameter(names = "--callback-port", description = "Port of TCS http service.")
  public Integer callbackPort;

  @Parameter(
      names = "--callback-polling-uri",
      description = "Uri (ip/domain + port) of TCS polling service.")
  public String pollingUri;

  // Validations are done in {@link PayloadGeneratorModule}.
  @Override
  public void validate() {}
}
