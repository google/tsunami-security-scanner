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
import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.ByteString;
import java.util.Optional;
import okhttp3.HttpUrl;

/** Immutable HTTP request. */
@Immutable
@AutoValue
@AutoValue.CopyAnnotations
@SuppressWarnings("Immutable")
public abstract class HttpRequest {
  public abstract HttpMethod method();
  public abstract String url();
  public abstract HttpHeaders headers();
  public abstract Optional<ByteString> requestBody();

  public abstract Builder toBuilder();

  /**
   * Creates a {@link Builder} object for configuring {@link HttpRequest}.
   *
   * @return a {@link Builder} instance for the {@link HttpRequest} object.
   */
  public static Builder builder() {
    return new AutoValue_HttpRequest.Builder();
  }

  /**
   * Create a new HTTP GET request with the given {@code url}.
   *
   * @param url the url of the GET request.
   * @return a {@link Builder} object for configuring {@link HttpRequest}.
   */
  public static Builder get(String url) {
    checkArgument(!Strings.isNullOrEmpty(url));
    return builder().setMethod(HttpMethod.GET).setUrl(url);
  }

  /**
   * Create a new HTTP GET request with the given {@code uri}.
   *
   * @param uri the url of the GET request.
   * @return a {@link Builder} object for configuring {@link HttpRequest}.
   */
  public static Builder get(HttpUrl uri) {
    checkNotNull(uri);
    return builder().setMethod(HttpMethod.GET).setUrl(uri);
  }

  /**
   * Create a new HTTP HEAD request with the given {@code url}.
   *
   * @param url the url of the HEAD request.
   * @return a {@link Builder} object for configuring {@link HttpRequest}.
   */
  public static Builder head(String url) {
    checkArgument(!Strings.isNullOrEmpty(url));
    return builder().setMethod(HttpMethod.HEAD).setUrl(url);
  }

  /**
   * Create a new HTTP HEAD request with the given {@code uri}.
   *
   * @param uri the url of the HEAD request.
   * @return a {@link Builder} object for configuring {@link HttpRequest}.
   */
  public static Builder head(HttpUrl uri) {
    checkNotNull(uri);
    return builder().setMethod(HttpMethod.HEAD).setUrl(uri);
  }

  /**
   * Create a new HTTP POST request with the given {@code url}.
   *
   * @param url the url of the POST request.
   * @return a {@link Builder} object for configuring {@link HttpRequest}.
   */
  public static Builder post(String url) {
    checkArgument(!Strings.isNullOrEmpty(url));
    return builder().setMethod(HttpMethod.POST).setUrl(url);
  }

  /**
   * Create a new HTTP POST request with the given {@code uri}.
   *
   * @param uri the url of the POST request.
   * @return a {@link Builder} object for configuring {@link HttpRequest}.
   */
  public static Builder post(HttpUrl uri) {
    checkNotNull(uri);
    return builder().setMethod(HttpMethod.POST).setUrl(uri);
  }

  /**
   * Create a new HTTP PUT request with the given {@code url}.
   *
   * @param url the url of the PUT request.
   * @return a {@link Builder} object for configuring {@link HttpRequest}.
   */
  public static Builder put(String url) {
    checkArgument(!Strings.isNullOrEmpty(url));
    return put(HttpUrl.parse(url));
  }

  /**
   * Create a new HTTP PUT request with the given {@code uri}.
   *
   * @param uri the url of the PUT request.
   * @return a {@link Builder} object for configuring {@link HttpRequest}.
   */
  public static Builder put(HttpUrl uri) {
    checkNotNull(uri);
    return builder().setMethod(HttpMethod.PUT).setUrl(uri);
  }

  /**
   * Create a new HTTP DELETE request with the given {@code url}.
   *
   * @param url the url of the DELETE request.
   * @return a {@link Builder} object for configuring {@link HttpRequest}.
   */
  public static Builder delete(String url) {
    checkArgument(!Strings.isNullOrEmpty(url));
    return builder().setMethod(HttpMethod.DELETE).setUrl(url);
  }

  /**
   * Create a new HTTP DELETE request with the given {@code uri}.
   *
   * @param uri the url of the DELETE request.
   * @return a {@link Builder} object for configuring {@link HttpRequest}.
   */
  public static Builder delete(HttpUrl uri) {
    checkNotNull(uri);
    return builder().setMethod(HttpMethod.DELETE).setUrl(uri);
  }

  /** Builder for {@link HttpRequest}. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setMethod(HttpMethod method);
    public abstract Builder setUrl(String url);
    public Builder setUrl(HttpUrl url) {
      setUrl(url.toString());
      return this;
    }
    public abstract Builder setHeaders(HttpHeaders httpHeaders);
    public abstract Builder setRequestBody(ByteString requestBody);
    public abstract Builder setRequestBody(Optional<ByteString> requestBody);

    public Builder withEmptyHeaders() {
      setHeaders(HttpHeaders.builder().build());
      return this;
    }

    abstract HttpRequest autoBuild();
    public HttpRequest build() {
      HttpRequest httpRequest = autoBuild();

      switch (httpRequest.method()) {
        case GET:
        case HEAD:
          checkState(
              !httpRequest.requestBody().isPresent(),
              "A request body is not allowed for HTTP GET/HEAD request");
          break;
        case POST:
        case PUT:
        case DELETE:
          break;
      }

      return httpRequest;
    }
  }
}
