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

/**
 * Command line arguments for the PluginManager.
 *
 * <p>Which detectors to include/exclude? Matching is executed against the name of the detector
 * (e.g. RsyncRceDetector).
 *
 * <p>To execute one single detector, override `detectors-include`, e.g.:
 * `--detectors-include=RsyncRceDetector`.
 *
 * <p>To disable a single detector, override `detectors-exclude`, e.g.
 * `--detectors-exclude=RsyncRceDetector`.
 */
@Parameters(separators = "=")
public final class PluginManagerCliOptions implements CliOption {
  @Parameter(
      names = "--detectors-include",
      description =
          "Comma separated list of detector names to include in the scan. By default, all detectors"
              + " are included.")
  public String detectorsInclude;

  @Parameter(
      names = "--detectors-exclude",
      description =
          "Comma separated list of detector names to exclude from the scan. By default no detectors"
              + " are skipped.")
  public String detectorsExclude;

  // Validations are done in {@link PayloadGeneratorModule}.
  @Override
  public void validate() {}
}
