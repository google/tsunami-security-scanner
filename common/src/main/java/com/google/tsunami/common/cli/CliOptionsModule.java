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
package com.google.tsunami.common.cli;

import static com.google.common.base.Preconditions.checkNotNull;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.GoogleLogger;
import com.google.inject.AbstractModule;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import java.lang.reflect.Constructor;

/**
 * A Guice module that parses CLI arguments for all {@link CliOption} implementations at runtime.
 *
 * <p>This module relies on the {@link io.github.classgraph.ClassGraph} scan results to identify all
 * {@link CliOption} implementations at runtime. Each implementation is bound to a singleton object
 * of that impl and registered to JCommander for CLI parsing.
 */
public final class CliOptionsModule extends AbstractModule {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  private static final String CLI_OPTION_INTERFACE = "com.google.tsunami.common.cli.CliOption";

  private final ScanResult scanResult;
  private final String[] args;
  private final JCommander jCommander;

  public CliOptionsModule(ScanResult scanResult, String programName, String[] args) {
    this.scanResult = checkNotNull(scanResult);
    this.args = checkNotNull(args);
    this.jCommander = new JCommander();

    jCommander.setProgramName(programName);
  }

  @Override
  protected void configure() {
    // For each CliOption installed at runtime, bind a singleton instance and register the instance
    // to JCommander for parsing.
    ImmutableList.Builder<CliOption> cliOptions = ImmutableList.builder();
    for (ClassInfo classInfo :
        scanResult
            .getClassesImplementing(CLI_OPTION_INTERFACE)
            .filter(classInfo -> !classInfo.isInterface())) {
      logger.atInfo().log("Found CliOption: %s", classInfo.getName());

      CliOption cliOption = bindCliOption(classInfo.loadClass(CliOption.class));
      jCommander.addObject(cliOption);
      cliOptions.add(cliOption);
    }

    // Parse command arguments or die.
    try {
      jCommander.parse(args);
      cliOptions.build().forEach(CliOption::validate);
    } catch (ParameterException e) {
      jCommander.usage();
      throw e;
    }
  }

  private <T> T bindCliOption(Class<T> cliOptionClass) {
    try {
      Constructor<T> cliOptionCtor = cliOptionClass.getDeclaredConstructor();
      // Always create an instance of the CliOption regardless of scope.
      cliOptionCtor.setAccessible(true);
      T cliOption = cliOptionCtor.newInstance();
      bind(cliOptionClass).toInstance(cliOption);
      return cliOption;
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(
          String.format(
              "CliOption '%s' must be constructable via a no-argument constructor",
              cliOptionClass.getTypeName()),
          e);
    }
  }
}
