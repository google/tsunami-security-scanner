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
import static org.junit.Assert.assertThrows;

import com.google.protobuf.ByteString;
import okhttp3.HttpUrl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link HttpRequest}. */
@RunWith(JUnit4.class)
public class HttpRequestTest {

  @Test
  public void get_always_buildsHttpGetRequest() {
    HttpRequest httpRequest = HttpRequest.get("http://localhost/url").withEmptyHeaders().build();

    assertThat(httpRequest.method()).isEqualTo(HttpMethod.GET);
    assertThat(httpRequest.url()).isEqualTo(HttpUrl.parse("http://localhost/url"));
  }

  @Test
  public void head_always_buildsHttpHeadRequest() {
    HttpRequest httpRequest = HttpRequest.head("http://localhost/url").withEmptyHeaders().build();

    assertThat(httpRequest.method()).isEqualTo(HttpMethod.HEAD);
    assertThat(httpRequest.url()).isEqualTo(HttpUrl.parse("http://localhost/url"));
  }

  @Test
  public void post_always_buildsHttpPostRequest() {
    HttpRequest httpRequest = HttpRequest.post("http://localhost/url").withEmptyHeaders().build();

    assertThat(httpRequest.method()).isEqualTo(HttpMethod.POST);
    assertThat(httpRequest.url()).isEqualTo(HttpUrl.parse("http://localhost/url"));
  }

  @Test
  public void delete_always_buildsHttpDeleteRequest() {
    HttpRequest httpRequest = HttpRequest.delete("http://localhost/url").withEmptyHeaders().build();

    assertThat(httpRequest.method()).isEqualTo(HttpMethod.DELETE);
    assertThat(httpRequest.url()).isEqualTo(HttpUrl.parse("http://localhost/url"));
  }

  @Test
  public void build_whenGetRequestHasRequestBody_throwsIllegalStateException() {
    assertThrows(
        IllegalStateException.class,
        () ->
            HttpRequest.builder()
                .setMethod(HttpMethod.GET)
                .setUrl(HttpUrl.parse("http://localhost"))
                .setHeaders(HttpHeaders.builder().build())
                .setRequestBody(ByteString.EMPTY)
                .build());
  }
}
