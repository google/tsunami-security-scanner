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

import com.google.tsunami.common.config.annotations.ConfigProperties;

/** Configuration properties for {@link HttpClient}. */
@ConfigProperties("common.net.http")
public final class HttpClientConfigProperties {
  /** Whether the HTTP client should trust all certificates on HTTPS traffic. */
  Boolean trustAllCertificates;

  /**
   * The timeout in seconds for complete HTTP calls. See
   * https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/call-timeout/ for
   * more details.
   */
  Integer callTimeoutSeconds;

  /**
   * The timeout in seconds for new HTTP connections. See
   * https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/connect-timeout/
   * for more details.
   */
  Integer connectTimeoutSeconds;

  /**
   * The timeout in seconds for the read operations for HTTP connections. See
   * https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/read-timeout/ for
   * more details.
   */
  Integer readTimeoutSeconds;

  /**
   * The timeout in seconds for the write operations for HTTP connections. See
   * https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/write-timeout/ for
   * more details.
   */
  Integer writeTimeoutSeconds;
}
