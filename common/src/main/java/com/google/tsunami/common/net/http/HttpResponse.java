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

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.errorprone.annotations.Immutable;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import java.util.Optional;
import okhttp3.HttpUrl;

/** Immutable HTTP response. */
@Immutable
@AutoValue
@AutoValue.CopyAnnotations
// HttpUrl is immutable even if not marked as such.
@SuppressWarnings("Immutable")
public abstract class HttpResponse {
  private static final JsonParser JSON_PARSER = new JsonParser();

  public abstract HttpStatus status();
  public abstract HttpHeaders headers();
  public abstract Optional<ByteString> bodyBytes();
  // The URL that produced this response.
  // TODO(b/173574468): Provide the full redirection request not just the Url.
  public abstract Optional<HttpUrl> responseUrl();

  /** Returns the body of the HTTP response as a UTF-8 encoded String. */
  @Memoized
  public Optional<String> bodyString() {
    return bodyBytes().map(ByteString::toStringUtf8);
  }

  /**
   * Tries to parse the response body as json and returns the parsing result as {@link JsonElement}.
   * If parsing failed, {@link com.google.gson.JsonSyntaxException} will be thrown.
   */
  @Memoized
  public Optional<JsonElement> bodyJson() {
    return bodyString().map(JSON_PARSER::parse);
  }

  public static Builder builder() {
    return new AutoValue_HttpResponse.Builder();
  }

  /** Builder for {@link HttpResponse}. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setStatus(HttpStatus httpStatus);
    public abstract Builder setHeaders(HttpHeaders httpHeaders);
    public abstract Builder setBodyBytes(ByteString bodyBytes);
    public abstract Builder setBodyBytes(Optional<ByteString> bodyBytes);
    public abstract Builder setResponseUrl(HttpUrl url);
    public abstract Builder setResponseUrl(Optional<HttpUrl> url);

    public abstract HttpResponse build();
  }
}
