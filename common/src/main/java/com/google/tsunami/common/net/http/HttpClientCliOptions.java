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
import com.beust.jcommander.Parameters;
import com.google.tsunami.common.cli.CliOption;

/** Command line argument for {@link HttpClient}. */
@Parameters(separators = "=")
public final class HttpClientCliOptions implements CliOption {

  @Parameter(
      names = "--http-client-trust-all-certificates",
      description =
          "Whether the HTTP client should trust all certificates on HTTPS traffic. Default false.")
  boolean trustAllCertificates = false;

  @Override
  public void validate() {}
}
