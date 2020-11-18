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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.USER_AGENT;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.base.Ascii;
import com.google.common.flogger.GoogleLogger;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;
import com.google.tsunami.proto.NetworkService;
import java.io.IOException;
import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dns;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A client library that communicates with remote servers via the HTTP protocol. */
public final class HttpClient {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  public static final String TSUNAMI_USER_AGENT = "TsunamiSecurityScanner";

  private final OkHttpClient okHttpClient;

  @Inject
  HttpClient(OkHttpClient okHttpClient) {
    this.okHttpClient = checkNotNull(okHttpClient);
  }

  /** Sends the given HTTP request using this client, blocking until full response is received. */
  public HttpResponse send(HttpRequest httpRequest) throws IOException {
    return send(httpRequest, null);
  }

  /**
   * Sends the given HTTP request using this client blocking until full response is received. If
   * {@code networkService} is not null, the host header is set according to the service's header
   * field even if it resolves to a different ip.
   */
  public HttpResponse send(HttpRequest httpRequest, @Nullable NetworkService networkService)
      throws IOException {
    logger.atInfo().log(
        "Sending HTTP '%s' request to '%s'.", httpRequest.method(), httpRequest.url().toString());

    OkHttpClient callHttpClient = clientWithHostnameAsProxy(networkService);
    try (Response okHttpResponse =
        callHttpClient.newCall(buildOkHttpRequest(httpRequest)).execute()) {
      return parseResponse(okHttpResponse);
    }
  }

  /** Sends the given HTTP request using this client asynchronously. */
  public ListenableFuture<HttpResponse> sendAsync(HttpRequest httpRequest) {
    return sendAsync(httpRequest, null);
  }

  /**
   * Sends the given HTTP request using this client asynchronously. If {@code networkService} is not
   * null, the host header is set according to the service's header field even if it resolves to a
   * different ip.
   */
  public ListenableFuture<HttpResponse> sendAsync(
      HttpRequest httpRequest, @Nullable NetworkService networkService) {
    logger.atInfo().log(
        "Sending async HTTP '%s' request to '%s'.",
        httpRequest.method(), httpRequest.url().toString());
    OkHttpClient callHttpClient = clientWithHostnameAsProxy(networkService);
    SettableFuture<HttpResponse> responseFuture = SettableFuture.create();
    Call requestCall = callHttpClient.newCall(buildOkHttpRequest(httpRequest));

    try {
      requestCall.enqueue(
          new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
              responseFuture.setException(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
              try (ResponseBody unused = response.body()) {
                responseFuture.set(parseResponse(response));
              } catch (Throwable t) {
                responseFuture.setException(t);
              }
            }
          });
    } catch (Throwable t) {
      responseFuture.setException(t);
    }

    // Makes sure cancellation state is propagated to OkHttp.
    responseFuture.addListener(
        () -> {
          if (responseFuture.isCancelled()) {
            requestCall.cancel();
          }
        },
        directExecutor());
    return responseFuture;
  }

  /**
   * Returns a modified HTTP client that's configured to connect to the {@code networkService}'s IP
   * and use its hostname in the host header, when both a hostname and an IP address is specified.
   * Returns an unmodified HTTP client otherwise.
   */
  private OkHttpClient clientWithHostnameAsProxy(NetworkService networkService) {
    if (networkService == null) {
      return this.okHttpClient;
    }
    String serviceIp = networkService.getNetworkEndpoint().getIpAddress().getAddress();
    String serviceHostname = networkService.getNetworkEndpoint().getHostname().getName();
    return this.okHttpClient
        .newBuilder()
        .dns(
            hostname -> {
              if (hostname.equals(serviceHostname)) {
                hostname = serviceIp;
              }
              return Dns.SYSTEM.lookup(hostname);
            })
        .hostnameVerifier(
            (hostname, session) -> {
              if (hostname.equals(serviceHostname)) {
                return true;
              }
              return HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session);
            })
        .build();
  }

  private static Request buildOkHttpRequest(HttpRequest httpRequest) {
    Request.Builder okRequestBuilder = new Request.Builder().url(httpRequest.url());

    httpRequest.headers().names().stream()
        .filter(headerName -> !Ascii.equalsIgnoreCase(headerName, USER_AGENT))
        .forEach(
            headerName ->
                httpRequest
                    .headers()
                    .getAll(headerName)
                    .forEach(headerValue -> okRequestBuilder.addHeader(headerName, headerValue)));
    okRequestBuilder.addHeader(USER_AGENT, TSUNAMI_USER_AGENT);

    switch (httpRequest.method()) {
      case GET:
        okRequestBuilder.get();
        break;
      case HEAD:
        okRequestBuilder.head();
        break;
      case POST:
        okRequestBuilder.post(buildRequestBody(httpRequest));
        break;
      case DELETE:
        okRequestBuilder.delete(buildRequestBody(httpRequest));
        break;
    }

    return okRequestBuilder.build();
  }

  private static RequestBody buildRequestBody(HttpRequest httpRequest) {
    MediaType mediaType =
        MediaType.parse(
            httpRequest.headers().get(com.google.common.net.HttpHeaders.CONTENT_TYPE).orElse(""));
    return RequestBody.create(
        mediaType, httpRequest.requestBody().orElse(ByteString.EMPTY).toByteArray());
  }

  private static HttpResponse parseResponse(Response okResponse) throws IOException {
    logger.atInfo().log(
        "Received HTTP response with code '%d' for request to '%s'.",
        okResponse.code(), okResponse.request().url());

    HttpResponse.Builder httpResponseBuilder =
        HttpResponse.builder()
            .setStatus(HttpStatus.fromCode(okResponse.code()))
            .setHeaders(convertHeaders(okResponse.headers()))
            .setResponseUrl(okResponse.request().url());
    if (!okResponse.request().method().equals(HttpMethod.HEAD.name())
        && okResponse.body() != null) {
      httpResponseBuilder.setBodyBytes(ByteString.copyFrom(okResponse.body().bytes()));
    }
    return httpResponseBuilder.build();
  }

  private static HttpHeaders convertHeaders(Headers headers) {
    HttpHeaders.Builder headersBuilder = HttpHeaders.builder();
    for (int i = 0; i < headers.size(); i++) {
      headersBuilder.addHeader(headers.name(i), headers.value(i));
    }
    return headersBuilder.build();
  }

  /**
   * Returns a {@link Builder} that allows client code to modify the configurations of the internal
   * http client.
   */
  public Builder modify() {
    return new Builder(this);
  }

  /** Builder for {@link HttpClient}. */
  // TODO(b/145315535): add more configurable options into the builder.
  public static class Builder {
    private final OkHttpClient okHttpClient;
    private boolean followRedirects;

    private Builder(HttpClient httpClient) {
      this.okHttpClient = httpClient.okHttpClient;
      this.followRedirects = okHttpClient.followRedirects();
    }

    public Builder setFollowRedirects(boolean followRedirects) {
      this.followRedirects = followRedirects;
      return this;
    }

    public HttpClient build() {
      return new HttpClient(okHttpClient.newBuilder().followRedirects(followRedirects).build());
    }
  }
}
