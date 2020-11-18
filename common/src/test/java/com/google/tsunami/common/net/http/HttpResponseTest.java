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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.gson.JsonSyntaxException;
import com.google.protobuf.ByteString;
import okhttp3.HttpUrl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link HttpResponse}. */
@RunWith(JUnit4.class)
public final class HttpResponseTest {

  private static final HttpUrl TEST_URL = HttpUrl.parse("https://example.com/");

  @Test
  public void bodyJson_whenValidResponseBody_returnsParsedJson() {
    HttpResponse httpResponse =
        HttpResponse.builder()
            .setStatus(HttpStatus.OK)
            .setHeaders(HttpHeaders.builder().build())
            .setBodyBytes(ByteString.copyFromUtf8("{ \"test_value\": 1 }"))
            .setResponseUrl(TEST_URL)
            .build();

    assertThat(httpResponse.bodyJson()).isPresent();
    assertThat(httpResponse.bodyJson().get().isJsonObject()).isTrue();
    assertThat(
            httpResponse
                .bodyJson()
                .get()
                .getAsJsonObject()
                .getAsJsonPrimitive("test_value")
                .getAsInt())
        .isEqualTo(1);
  }

  @Test
  public void bodyJson_whenEmptyResponseBody_returnsEmptyOptional() {
    HttpResponse httpResponse =
        HttpResponse.builder()
            .setStatus(HttpStatus.OK)
            .setHeaders(HttpHeaders.builder().build())
            .setResponseUrl(TEST_URL)
            .build();

    assertThat(httpResponse.bodyJson()).isEmpty();
  }

  @Test
  public void bodyJson_whenNonJsonResponseBody_throwsJsonSyntaxException() {
    HttpResponse httpResponse =
        HttpResponse.builder()
            .setStatus(HttpStatus.OK)
            .setHeaders(HttpHeaders.builder().build())
            .setBodyBytes(ByteString.copyFromUtf8("not a json"))
            .setResponseUrl(TEST_URL)
            .build();

    assertThrows(JsonSyntaxException.class, httpResponse::bodyJson);
  }
}
