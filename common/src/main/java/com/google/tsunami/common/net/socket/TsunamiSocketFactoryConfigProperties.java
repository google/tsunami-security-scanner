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

import com.google.tsunami.common.config.annotations.ConfigProperties;

/**
 * Configuration properties for {@link TsunamiSocketFactory}.
 *
 * <p>These properties can be set in the Tsunami configuration file (e.g., tsunami.yaml) under the
 * {@code common.net.socket} prefix:
 *
 * <pre>{@code
 * common:
 *   net:
 *     socket:
 *       connect_timeout_seconds: 10
 *       read_timeout_seconds: 30
 *       trust_all_certificates: true
 *       disable_timeouts: false
 * }</pre>
 */
@ConfigProperties("common.net.socket")
public final class TsunamiSocketFactoryConfigProperties {

  /**
   * The timeout in seconds for establishing TCP connections.
   *
   * <p>This timeout applies to the socket connect operation. If the connection cannot be
   * established within this time, a {@link java.net.SocketTimeoutException} will be thrown.
   *
   * <p>Default value is 10 seconds if not specified.
   */
  Integer connectTimeoutSeconds;

  /**
   * The timeout in seconds for read operations on sockets.
   *
   * <p>This timeout is set as the socket's SO_TIMEOUT option. If no data is received within this
   * time, a {@link java.net.SocketTimeoutException} will be thrown.
   *
   * <p>Default value is 30 seconds if not specified. This is intentionally longer than the connect
   * timeout to allow for slow servers or large data transfers.
   */
  Integer readTimeoutSeconds;

  /**
   * Whether SSL/TLS connections should trust all certificates.
   *
   * <p>When set to true, the socket factory will accept any SSL certificate without validation.
   * This is useful for security scanning where targets may have self-signed or expired
   * certificates.
   *
   * <p><strong>Warning:</strong> This should only be used in controlled environments. Setting this
   * to true disables certificate validation which could expose the scanner to man-in-the-middle
   * attacks.
   *
   * <p>Default value is true for security scanning purposes.
   */
  Boolean trustAllCertificates;

  /**
   * Whether to disable all socket timeouts.
   *
   * <p>When set to true, sockets will wait indefinitely for connections and data. This means
   * plugins may hang forever if a server does not respond.
   *
   * <p><strong>Warning:</strong> Disabling timeouts is dangerous and can cause the scanner to
   * hang indefinitely. Only use this option if you have a specific need to wait for slow or
   * unresponsive servers.
   *
   * <p>Default value is false (timeouts are enforced).
   */
  Boolean disableTimeouts;
}
