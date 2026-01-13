/*
 * Copyright 2025 Google LLC
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
package com.google.tsunami.common.net.socket;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.tsunami.common.cli.CliOption;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Command line arguments for {@link TsunamiSocketFactory}.
 *
 * <p>These options allow users to override socket timeout settings from the command line. CLI
 * options take precedence over configuration file settings.
 */
@Parameters(separators = "=")
public final class TsunamiSocketFactoryCliOptions implements CliOption {

  @Parameter(
      names = "--socket-connect-timeout-seconds",
      description =
          "The timeout in seconds for establishing TCP connections. This timeout applies to the"
              + " socket connect operation. Default is 10 seconds.")
  public Integer connectTimeoutSeconds;

  @Parameter(
      names = "--socket-read-timeout-seconds",
      description =
          "The timeout in seconds for read operations on sockets (SO_TIMEOUT). If no data is"
              + " received within this time, a SocketTimeoutException will be thrown. Default is 30"
              + " seconds.")
  public Integer readTimeoutSeconds;

  @Parameter(
      names = "--socket-trust-all-certificates",
      arity = 1,
      description =
          "Whether SSL/TLS connections should trust all certificates. When true, accepts any SSL"
              + " certificate without validation. Useful for scanning targets with self-signed"
              + " certificates. Default is true.")
  public Boolean trustAllCertificates;

  @Parameter(
      names = "--socket-disable-timeouts",
      arity = 1,
      description =
          "Disable all socket timeouts, allowing connections to wait indefinitely. WARNING: This"
              + " can cause plugins to hang forever if servers do not respond. Use with caution."
              + " Default is false.")
  public Boolean disableTimeouts;

  @Override
  public void validate() {
    // Skip timeout validation if timeouts are disabled
    if (disableTimeouts != null && disableTimeouts) {
      return;
    }
    validateTimeout("--socket-connect-timeout-seconds", connectTimeoutSeconds);
    validateTimeout("--socket-read-timeout-seconds", readTimeoutSeconds);
  }

  private static void validateTimeout(String flagName, @Nullable Integer value) {
    if (value != null && value < 0) {
      throw new ParameterException(
          String.format("%s cannot be a negative number, received %d.", flagName, value));
    }
    if (value != null && value == 0) {
      throw new ParameterException(
          String.format(
              "%s cannot be zero. Use a positive value for timeouts or pass the --socket-disable-timeouts flag to disable timeouts entirely. Received %d.",
              flagName, value));
    }
  }
}
