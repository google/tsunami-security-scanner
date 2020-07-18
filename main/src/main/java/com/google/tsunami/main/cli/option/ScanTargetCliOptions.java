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
package com.google.tsunami.main.cli.option;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.common.base.Joiner;
import com.google.tsunami.common.cli.CliOption;
import com.google.tsunami.main.cli.option.validator.IpV4Validator;
import com.google.tsunami.main.cli.option.validator.IpV6Validator;
import java.util.ArrayList;
import java.util.List;

/** Command line arguments for Tsunami scan target. */
@Parameters(separators = "=")
public final class ScanTargetCliOptions implements CliOption {

  @Parameter(
          names = "--ip-v4-subnet",
          description = "The address of a target IP v4 subnet.",
          validateWith = IpV4Validator.class)
  public String ipV4SubnetTarget;

  @Parameter(
          names = "--ip-v6-subnet",
          description = "The address of a target IP v6 subnet.",
          validateWith = IpV6Validator.class)
  public String ipV6SubnetTarget;

  @Parameter(
      names = "--ip-v4-target",
      description = "The IP v4 address of the scanning target.",
      validateWith = IpV4Validator.class)
  public String ipV4Target;

  @Parameter(
      names = "--ip-v6-target",
      description = "The IP v6 address of the scanning target.",
      validateWith = IpV6Validator.class)
  public String ipV6Target;

  @Parameter(names = "--hostname-target", description = "The hostname of the scanning target.")
  public String hostnameTarget;

  @Override
  public void validate() {
    List<String> nonEmptyTargets = new ArrayList<>();
    if (ipV4Target != null) {
      nonEmptyTargets.add("--ip-v4-target");
    }
    if (ipV6Target != null) {
      nonEmptyTargets.add("--ip-v6-target");
    }
    if (hostnameTarget != null) {
      nonEmptyTargets.add("--hostname-target");
    }
    if (ipV4SubnetTarget != null) {
      nonEmptyTargets.add("--ip-v4-subnet");
    }
    if (ipV6SubnetTarget != null) {
      nonEmptyTargets.add("--ip-v4-subnet");
    }

    if (nonEmptyTargets.isEmpty()) {
      throw new ParameterException(
          "One of the following parameters is expected: --ip-v4-target, --ip-v6-target,"
              + " --hostname-target, --ip-v4-subnet, --ip-v6-subnet");
    }
    if (nonEmptyTargets.size() != 1) {
      throw new ParameterException(
          String.format(
              "Only one of the following parameter is expected: --ip-v4-target, --ip-v6-target,"
                  + " --hostname-target, --ip-v4-subnet, --ip-v6-subnet, got %d: %s",
              nonEmptyTargets.size(), Joiner.on(",").join(nonEmptyTargets)));
    }
  }
}
