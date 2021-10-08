/*
 * Copyright 2021 Google LLC
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
package com.google.tsunami.common.net.http.javanet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Default implementation of {@link ConnectionFactory}, which simply attempts to open the connection
 * and optionally allows trusting all certifications.
 */
public class DefaultConnectionFactory implements ConnectionFactory {
  private final boolean trustAllCertificates;
  private final SSLSocketFactory trustAllCertsSocketFactory;
  private final Duration connectTimeout;
  private final Duration readTimeout;

  public DefaultConnectionFactory(
      boolean trustAllCertificates,
      SSLSocketFactory trustAllCertsSocketFactory,
      Duration connectTimeout,
      Duration readTimeout) {
    this.trustAllCertificates = trustAllCertificates;
    this.trustAllCertsSocketFactory = checkNotNull(trustAllCertsSocketFactory);
    this.connectTimeout = checkNotNull(connectTimeout);
    this.readTimeout = checkNotNull(readTimeout);
  }

  @Override
  public HttpURLConnection openConnection(String url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setConnectTimeout((int) connectTimeout.toMillis());
    connection.setReadTimeout((int) readTimeout.toMillis());

    if (connection instanceof HttpsURLConnection && trustAllCertificates) {
      HttpsURLConnection secureConnection = (HttpsURLConnection) connection;
      secureConnection.setSSLSocketFactory(trustAllCertsSocketFactory);
      secureConnection.setHostnameVerifier((hostname, session) -> true);
    }
    return connection;
  }
}
