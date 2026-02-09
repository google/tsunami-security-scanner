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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Guice module for installing {@link TsunamiSocketFactory}.
 *
 * <p>This module provides a {@link TsunamiSocketFactory} instance that creates sockets with
 * enforced timeout configurations. It integrates with Tsunami's configuration system to allow
 * customization of default timeouts through both configuration files and command-line options.
 *
 * <p>Example usage in a plugin:
 *
 * <pre>{@code
 * public class MyPlugin implements VulnDetector {
 *   private final TsunamiSocketFactory socketFactory;
 *
 *   @Inject
 *   MyPlugin(TsunamiSocketFactory socketFactory) {
 *     this.socketFactory = socketFactory;
 *   }
 *
 *   public void doSomething() throws IOException {
 *     // Socket will have enforced timeouts
 *     Socket socket = socketFactory.createSocket("example.com", 80);
 *     // ...
 *   }
 * }
 * }</pre>
 */
public final class TsunamiSocketFactoryModule extends AbstractModule {

  // Default timeout values
  private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
  private static final int DEFAULT_READ_TIMEOUT_SECONDS = 30;
  private static final boolean DEFAULT_TRUST_ALL_CERTIFICATES = true;
  private static final boolean DEFAULT_DISABLE_TIMEOUTS = false;

  // This TrustManager does NOT validate certificate chains.
  private static final X509TrustManager TRUST_ALL_CERTS_MANAGER =
      new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }
      };

  @Override
  protected void configure() {
    // Module configuration is handled by @Provides methods
  }

  @Provides
  @Singleton
  TsunamiSocketFactory provideTsunamiSocketFactory(
      @TrustAllCertificates boolean trustAllCertificates,
      @DisableTimeouts boolean disableTimeouts,
      @ConnectTimeoutSeconds int connectTimeoutSeconds,
      @ReadTimeoutSeconds int readTimeoutSeconds)
      throws GeneralSecurityException {
    SocketFactory socketFactory = SocketFactory.getDefault();
    SSLSocketFactory sslSocketFactory;

    if (trustAllCertificates) {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, new TrustManager[] {TRUST_ALL_CERTS_MANAGER}, new SecureRandom());
      sslSocketFactory = sslContext.getSocketFactory();
    } else {
      sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    // When timeouts are disabled, use 0 which means infinite timeout in Java
    Duration connectTimeout =
        disableTimeouts ? Duration.ZERO : Duration.ofSeconds(connectTimeoutSeconds);
    Duration readTimeout = disableTimeouts ? Duration.ZERO : Duration.ofSeconds(readTimeoutSeconds);

    return new DefaultTsunamiSocketFactory(
        socketFactory, sslSocketFactory, connectTimeout, readTimeout);
  }

  @Provides
  @DisableTimeouts
  boolean provideDisableTimeouts(
      TsunamiSocketFactoryCliOptions cliOptions,
      TsunamiSocketFactoryConfigProperties configProperties) {
    if (cliOptions.disableTimeouts != null) {
      return cliOptions.disableTimeouts;
    }
    if (configProperties.disableTimeouts != null) {
      return configProperties.disableTimeouts;
    }
    return DEFAULT_DISABLE_TIMEOUTS;
  }

  @Provides
  @TrustAllCertificates
  boolean provideTrustAllCertificates(
      TsunamiSocketFactoryCliOptions cliOptions,
      TsunamiSocketFactoryConfigProperties configProperties) {
    if (cliOptions.trustAllCertificates != null) {
      return cliOptions.trustAllCertificates;
    }
    if (configProperties.trustAllCertificates != null) {
      return configProperties.trustAllCertificates;
    }
    return DEFAULT_TRUST_ALL_CERTIFICATES;
  }

  @Provides
  @ConnectTimeoutSeconds
  int provideConnectTimeoutSeconds(
      TsunamiSocketFactoryCliOptions cliOptions,
      TsunamiSocketFactoryConfigProperties configProperties) {
    if (cliOptions.connectTimeoutSeconds != null) {
      return cliOptions.connectTimeoutSeconds;
    }
    if (configProperties.connectTimeoutSeconds != null) {
      return configProperties.connectTimeoutSeconds;
    }
    return DEFAULT_CONNECT_TIMEOUT_SECONDS;
  }

  @Provides
  @ReadTimeoutSeconds
  int provideReadTimeoutSeconds(
      TsunamiSocketFactoryCliOptions cliOptions,
      TsunamiSocketFactoryConfigProperties configProperties) {
    if (cliOptions.readTimeoutSeconds != null) {
      return cliOptions.readTimeoutSeconds;
    }
    if (configProperties.readTimeoutSeconds != null) {
      return configProperties.readTimeoutSeconds;
    }
    return DEFAULT_READ_TIMEOUT_SECONDS;
  }

  /** Qualifier for whether to disable all socket timeouts. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  public @interface DisableTimeouts {}

  /** Qualifier for whether to trust all SSL certificates. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  public @interface TrustAllCertificates {}

  /** Qualifier for the connect timeout in seconds. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  public @interface ConnectTimeoutSeconds {}

  /** Qualifier for the read timeout in seconds. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  public @interface ReadTimeoutSeconds {}
}
