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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.tsunami.common.net.http.javanet.ConnectionFactory;
import com.google.tsunami.common.net.http.javanet.DefaultConnectionFactory;
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
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

/** Guice module for installing {@link HttpClient} library. */
public final class HttpClientModule extends AbstractModule {
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
  // Maximum number of requests for each host (URL's host name) to execute concurrently.
  private static final int OKHTTPCLIENT_MAX_REQUESTS_PER_HOST = 5;

  // Maximum number of idle connections to each to keep in the pool.
  private final int connectionPoolMaxIdle;
  // Duration to keep the connection alive in the pool before closing it.
  private final Duration connectionPoolKeepAliveDuration;
  // Maximum number of requests to execute concurrently.
  private final int maxRequests;
  // Whether or not to follow redirect from server.
  private final boolean followRedirects;
  // A log ID to print in front of the logs.
  private final String logId;

  public HttpClientModule(Builder builder) {
    checkNotNull(builder);
    this.connectionPoolMaxIdle = builder.connectionPoolMaxIdle;
    this.connectionPoolKeepAliveDuration = builder.connectionPoolKeepAliveDuration;
    this.maxRequests = builder.maxRequests;
    this.followRedirects = builder.followRedirects;
    this.logId = builder.logId;
  }

  @Provides
  @Singleton
  ConnectionPool provideConnectionPool() {
    return new ConnectionPool(
        connectionPoolMaxIdle, connectionPoolKeepAliveDuration.toMillis(), MILLISECONDS);
  }

  @Provides
  @Singleton
  Dispatcher provideDispatcher() {
    Dispatcher dispatcher = new Dispatcher();
    dispatcher.setMaxRequests(maxRequests);
    dispatcher.setMaxRequestsPerHost(OKHTTPCLIENT_MAX_REQUESTS_PER_HOST);
    return dispatcher;
  }

  @Provides
  @Singleton
  @TrustAllCertsSocketFactory
  SSLSocketFactory provideTrustAllCertsSocketFactory() throws GeneralSecurityException {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[] {TRUST_ALL_CERTS_MANAGER}, new SecureRandom());
    return sslContext.getSocketFactory();
  }

  // Missing features:
  // 1. Custom cookie handler.
  @Provides
  @Singleton
  OkHttpClient provideOkHttpClient(
      ConnectionPool connectionPool,
      Dispatcher dispatcher,
      @TrustAllCertsSocketFactory SSLSocketFactory trustAllCertsSocketFactory,
      @TrustAllCertificates boolean trustAllCertificates,
      @ConnectTimeoutSeconds int connectTimeoutSeconds) {
    OkHttpClient.Builder clientBuilder =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(connectTimeoutSeconds))
            .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
            .readTimeout(Duration.ofSeconds(connectTimeoutSeconds))
            .writeTimeout(Duration.ofSeconds(connectTimeoutSeconds))
            .connectionPool(connectionPool)
            .dispatcher(dispatcher)
            .followRedirects(followRedirects);
    if (trustAllCertificates) {
      clientBuilder
          .sslSocketFactory(trustAllCertsSocketFactory, TRUST_ALL_CERTS_MANAGER)
          .hostnameVerifier((hostname, session) -> true);
    }
    return clientBuilder.build();
  }

  @Provides
  @Singleton
  HttpClient provideOkHttpHttpClient(
      OkHttpClient okHttpClient,
      @TrustAllCertificates boolean trustAllCertificates,
      ConnectionFactory connectionFactory,
      @LogId String logId,
      @ConnectTimeout Duration connectTimeout) {
    return new OkHttpHttpClient(
        okHttpClient, trustAllCertificates, connectionFactory, logId, connectTimeout);
  }

  @Provides
  @Singleton
  ConnectionFactory provideJavaNetConnectionFactory(
      @TrustAllCertificates boolean trustAllCertificates,
      @TrustAllCertsSocketFactory SSLSocketFactory trustAllCertsSocketFactory,
      @ConnectTimeoutSeconds int connectTimeoutSeconds,
      @ReadTimeoutSeconds int readTimeoutSeconds) {
    return new DefaultConnectionFactory(
        trustAllCertificates,
        trustAllCertsSocketFactory,
        Duration.ofSeconds(connectTimeoutSeconds),
        Duration.ofSeconds(readTimeoutSeconds));
  }

  @Provides
  @TrustAllCertificates
  boolean shouldTrustAllCertificates(
      HttpClientCliOptions httpClientCliOptions,
      HttpClientConfigProperties httpClientConfigProperties) {
    if (httpClientCliOptions.trustAllCertificates != null) {
      return httpClientCliOptions.trustAllCertificates;
    }
    if (httpClientConfigProperties.trustAllCertificates != null) {
      return httpClientConfigProperties.trustAllCertificates;
    }
    return false;
  }

  @Provides
  @LogId
  String provideLogid() {
    return logId;
  }

  @Provides
  @FollowRedirects
  boolean provideFollowRedirects() {
    return followRedirects;
  }

  @Provides
  @MaxRequests
  int provideMaxRequests() {
    return maxRequests;
  }

  @Provides
  @CallTimeoutSeconds
  int provideCallTimeoutSeconds(
      HttpClientCliOptions httpClientCliOptions,
      HttpClientConfigProperties httpClientConfigProperties) {
    if (httpClientCliOptions.callTimeoutSeconds != null) {
      return httpClientCliOptions.callTimeoutSeconds;
    }
    if (httpClientConfigProperties.callTimeoutSeconds != null) {
      return httpClientConfigProperties.callTimeoutSeconds;
    }
    // Default call timeout specified in
    // https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/call-timeout/.
    return 0;
  }

  @Provides
  @ConnectTimeoutSeconds
  int provideConnectTimeoutSeconds(
      HttpClientCliOptions httpClientCliOptions,
      HttpClientConfigProperties httpClientConfigProperties) {
    if (httpClientCliOptions.connectTimeoutSeconds != null) {
      return httpClientCliOptions.connectTimeoutSeconds;
    }
    if (httpClientConfigProperties.connectTimeoutSeconds != null) {
      return httpClientConfigProperties.connectTimeoutSeconds;
    }
    // Default connect timeout specified in
    // https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/connect-timeout/.
    return 10;
  }

  @Provides
  @ConnectTimeout
  Duration provideConnectTimeout(@ConnectTimeoutSeconds int connectionTimeoutSeconds) {
    return Duration.ofSeconds(connectionTimeoutSeconds);
  }

  @Provides
  @ReadTimeoutSeconds
  int provideReadTimeoutSeconds(
      HttpClientCliOptions httpClientCliOptions,
      HttpClientConfigProperties httpClientConfigProperties) {
    if (httpClientCliOptions.readTimeoutSeconds != null) {
      return httpClientCliOptions.readTimeoutSeconds;
    }
    if (httpClientConfigProperties.readTimeoutSeconds != null) {
      return httpClientConfigProperties.readTimeoutSeconds;
    }
    // Default read timeout specified in
    // https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/read-timeout/.
    return 10;
  }

  @Provides
  @WriteTimeoutSeconds
  int provideWriteTimeoutSeconds(
      HttpClientCliOptions httpClientCliOptions,
      HttpClientConfigProperties httpClientConfigProperties) {
    if (httpClientCliOptions.writeTimeoutSeconds != null) {
      return httpClientCliOptions.writeTimeoutSeconds;
    }
    if (httpClientConfigProperties.writeTimeoutSeconds != null) {
      return httpClientConfigProperties.writeTimeoutSeconds;
    }
    // Default write timeout specified in
    // https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/write-timeout/.
    return 10;
  }

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  @interface TrustAllCertificates {}

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  @interface TrustAllCertsSocketFactory {}

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  @interface LogId {}

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  @interface CallTimeoutSeconds {}

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  @interface ConnectTimeoutSeconds {}

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  @interface ConnectTimeout {}

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  @interface ReadTimeoutSeconds {}

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  @interface WriteTimeoutSeconds {}

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  @interface FollowRedirects {}

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  @interface MaxRequests {}

  /** Builder for {@link HttpClientModule}. */
  public static final class Builder {
    private static final int DEFAULT_CONNECTION_POOL_MAX_IDLE = 5;
    private static final Duration DEFAULT_CONNECTION_POOL_KEEP_ALIVE_DURATION =
        Duration.ofMinutes(5);
    private static final int DEFAULT_MAX_REQUESTS = 64;
    private static final boolean DEFAULT_FOLLOW_REDIRECTS = true;
    private static final String DEFAULT_LOG_ID = "";

    private int connectionPoolMaxIdle = DEFAULT_CONNECTION_POOL_MAX_IDLE;
    private Duration connectionPoolKeepAliveDuration = DEFAULT_CONNECTION_POOL_KEEP_ALIVE_DURATION;
    private int maxRequests = DEFAULT_MAX_REQUESTS;
    private boolean followRedirects = DEFAULT_FOLLOW_REDIRECTS;
    private String logId = DEFAULT_LOG_ID;

    /**
     * Sets the maximum number of idle connections to each to keep in the pool.
     *
     * @param maxIdle maximum number of idel connecteds.
     * @return the {@link Builder} instance itself.
     */
    public Builder setConnectionPoolMaxIdle(int maxIdle) {
      checkArgument(maxIdle > 0);
      this.connectionPoolMaxIdle = maxIdle;
      return this;
    }

    /**
     * Sets the duration to keep the connection alive in the connection pool before closing it.
     *
     * @param keepAliveDuration the duration to keep the connection alive.
     * @return the {@link Builder} instance itself.
     */
    public Builder setConnectionPoolKeepAliveDuration(Duration keepAliveDuration) {
      checkNotNull(keepAliveDuration);
      checkArgument(!keepAliveDuration.isNegative());
      this.connectionPoolKeepAliveDuration = keepAliveDuration;
      return this;
    }

    /**
     * Sets the maximum number of requests to execute concurrently.
     *
     * @param maxRequests the maximum number of concurrent requests.
     * @return the {@link Builder} instance itself.
     */
    public Builder setMaxRequests(int maxRequests) {
      checkArgument(maxRequests > 0);
      this.maxRequests = maxRequests;
      return this;
    }

    /**
     * Sets whether or not to follow redirect from server. If unset, by default redirects will be
     * followed.
     *
     * @param followRedirects whether the HTTP client should follow redirect responses from the
     *     server.
     * @return the {@link Builder} instance itself.
     */
    public Builder setFollowRedirects(boolean followRedirects) {
      this.followRedirects = followRedirects;
      return this;
    }

    /**
     * Sets the log ID to print in front of the logs.
     *
     * @param logId the log ID to print in front of the logs.
     * @return the {@link Builder} instance itself.
     */
    public Builder setLogId(String logId) {
      this.logId = logId;
      return this;
    }

    public HttpClientModule build() {
      return new HttpClientModule(this);
    }
  }
}
