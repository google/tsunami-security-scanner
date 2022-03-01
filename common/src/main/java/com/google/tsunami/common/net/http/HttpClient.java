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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.tsunami.proto.NetworkService;
import java.io.IOException;
import java.time.Duration;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A client library that communicates with remote servers via the HTTP protocol. */
public abstract class HttpClient {
  public static final String TSUNAMI_USER_AGENT = "TsunamiSecurityScanner";

  /**
   * Gets log id.
   *
   * @return log id string.
   */
  public abstract String getLogId();

  /**
   * NOTE: This is a temporary hack to workaround OkHttp's hardcoded URL canonicalization algorithm.
   * We should rewrite the entire library using a more flexible backend.
   *
   * <p>Sends the given HTTP request as is, blocking until full response is received.
   *
   * @param httpRequest the HTTP request to be sent by this client.
   * @return the response returned from the HTTP server.
   * @throws IOException if an I/O error occurs during the HTTP request.
   */
  public abstract HttpResponse sendAsIs(HttpRequest httpRequest) throws IOException;

  /**
   * Sends the given HTTP request using this client, blocking until full response is received.
   *
   * @param httpRequest the HTTP request to be sent by this client.
   * @return the response returned from the HTTP server.
   * @throws IOException if an I/O error occurs during the HTTP request.
   */
  public abstract HttpResponse send(HttpRequest httpRequest) throws IOException;

  /**
   * Sends the given HTTP request using this client blocking until full response is received. If
   * {@code networkService} is not null, the host header is set according to the service's header
   * field even if it resolves to a different ip.
   *
   * @param httpRequest the HTTP request to be sent by this client.
   * @param networkService the {@link NetworkService} proto to be used for the HOST header.
   * @return the response returned from the HTTP server.
   * @throws IOException if an I/O error occurs during the HTTP request.
   */
  public abstract HttpResponse send(
      HttpRequest httpRequest, @Nullable NetworkService networkService) throws IOException;

  /**
   * Sends the given HTTP request using this client asynchronously.
   *
   * @param httpRequest the HTTP request to be sent by this client.
   * @return the future for the response to be returned from the HTTP server.
   */
  public abstract ListenableFuture<HttpResponse> sendAsync(HttpRequest httpRequest);

  /**
   * Sends the given HTTP request using this client asynchronously. If {@code networkService} is not
   * null, the host header is set according to the service's header field even if it resolves to a
   * different ip.
   *
   * @param httpRequest the HTTP request to be sent by this client.
   * @param networkService the {@link NetworkService} proto to be used for the HOST header.
   * @return the future for the response to be returned from the HTTP server.
   */
  public abstract ListenableFuture<HttpResponse> sendAsync(
      HttpRequest httpRequest, @Nullable NetworkService networkService);

  public abstract <T extends HttpClient> Builder<T> modify();

  /** Base builder for implementations of HttpClient */
  public abstract static class Builder<T extends HttpClient> {

    public abstract Builder<T> setFollowRedirects(boolean followRedirects);

    public abstract Builder<T> setTrustAllCertificates(boolean trustAllCertificates);

    public abstract Builder<T> setLogId(String logId);

    public abstract Builder<T> setConnectTimeout(Duration connectionTimeout);

    public abstract T build();
  }
}
