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
package com.google.tsunami.common.net.http;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.tsunami.common.cli.CliOption;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Command line argument for {@link HttpClient}. */
@Parameters(separators = "=")
public final class HttpClientCliOptions implements CliOption {

  @Parameter(
      names = "--http-client-trust-all-certificates",
      description = "Whether the HTTP client should trust all certificates on HTTPS traffic.")
  Boolean trustAllCertificates;

  @Parameter(
      names = "--http-client-call-timeout-seconds",
      description =
          "[Depreciated] Set to be the same as the timeout specified by"
              + " --http-client-connect-timeout-seconds.")
  Integer callTimeoutSeconds;

  @Parameter(
      names = "--http-client-connect-timeout-seconds",
      description =
          "The timeout in seconds for new HTTP connections. See"
              + " https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/connect-timeout/"
              + " for more details.")
  Integer connectTimeoutSeconds;

  @Parameter(
      names = "--http-client-read-timeout-seconds",
      description =
          "[Depreciated] Set to be the same as the timeout specified by"
              + " --http-client-connect-timeout-seconds")
  Integer readTimeoutSeconds;

  @Parameter(
      names = "--http-client-write-timeout-seconds",
      description =
          "[Depreciated] Set to be the same as the timeout specified by"
              + " --http-client-connect-timeout-seconds.")
  Integer writeTimeoutSeconds;

  @Override
  public void validate() {
    validateTimeout("--http-client-call-timeout-seconds", callTimeoutSeconds);
    validateTimeout("--http-client-connect-timeout-seconds", connectTimeoutSeconds);
    validateTimeout("--http-client-read-timeout-seconds", readTimeoutSeconds);
    validateTimeout("--http-client-write-timeout-seconds", writeTimeoutSeconds);
  }

  private static void validateTimeout(String flagName, @Nullable Integer value) {
    if (value != null && value < 0) {
      throw new ParameterException(
          String.format("%s cannot be a negative number, received %d.", flagName, value));
    }
  }
}
