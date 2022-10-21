/*
 * Copyright 2022 Google LLC
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
package com.google.tsunami.common.net;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.tsunami.common.net.http.HttpRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class FuzzingUtilsTest {
  private static final HttpRequest REQUEST_WITHOUT_GET_PARAMETERS =
      HttpRequest.get("https://google.com").withEmptyHeaders().build();
  private static final HttpRequest REQUEST_WITH_GET_PARAMETERS =
      HttpRequest.get("https://google.com?key=value&other=test").withEmptyHeaders().build();

  @Test
  public void fuzzGetParametersWithDefaultParameter_whenNoGetParameters_addsDefaultParameter() {
    HttpRequest requestWithDefaultParameter =
        HttpRequest.get("https://google.com?default=<payload>").withEmptyHeaders().build();

    assertThat(
            FuzzingUtils.fuzzGetParametersWithDefaultParameter(
                REQUEST_WITHOUT_GET_PARAMETERS, "<payload>", "default"))
        .contains(requestWithDefaultParameter);
  }

  @Test
  public void fuzzGetParametersWithDefaultParameter_whenGetParameters_doesNotAddDefaultParameter() {
    HttpRequest requestWithDefaultParameter =
        HttpRequest.get("https://google.com?default=<payload>").withEmptyHeaders().build();

    assertThat(
            FuzzingUtils.fuzzGetParametersWithDefaultParameter(
                REQUEST_WITH_GET_PARAMETERS, "<payload>", "default"))
        .doesNotContain(requestWithDefaultParameter);
  }

  @Test
  public void fuzzGetParametersWithDefaultParameter_whenGetParameters_fuzzesAllParameters() {
    ImmutableList<HttpRequest> requestsWithFuzzedGetParameters =
        ImmutableList.of(
            HttpRequest.get("https://google.com?key=<payload>&other=test")
                .withEmptyHeaders()
                .build(),
            HttpRequest.get("https://google.com?key=value&other=<payload>")
                .withEmptyHeaders()
                .build());

    assertThat(
            FuzzingUtils.fuzzGetParametersWithDefaultParameter(
                REQUEST_WITH_GET_PARAMETERS, "<payload>", "default"))
        .containsAtLeastElementsIn(requestsWithFuzzedGetParameters);
  }

  @Test
  public void
      fuzzGetParametersExpectingPathValues_whenGetParameterValueHasFileExtension_appendsFileExtensionToPayload() {
    HttpRequest requestWithFileExtension =
        HttpRequest.get("https://google.com?key=value.jpg").withEmptyHeaders().build();
    HttpRequest requestWithFuzzedGetParameterWithFileExtension =
        HttpRequest.get("https://google.com?key=<payload>%00.jpg").withEmptyHeaders().build();

    assertThat(
            FuzzingUtils.fuzzGetParametersExpectingPathValues(
                requestWithFileExtension, "<payload>"))
        .contains(requestWithFuzzedGetParameterWithFileExtension);
  }

  @Test
  public void
      fuzzGetParametersExpectingPathValues_whenGetParameterValueHasPathPrefix_prefixesPayload() {
    HttpRequest requestWithPathPrefix =
        HttpRequest.get("https://google.com?key=resources/value").withEmptyHeaders().build();
    HttpRequest requestWithFuzzedGetParameterWithPathPrefix =
        HttpRequest.get("https://google.com?key=resources/<payload>").withEmptyHeaders().build();

    assertThat(
            FuzzingUtils.fuzzGetParametersExpectingPathValues(requestWithPathPrefix, "<payload>"))
        .contains(requestWithFuzzedGetParameterWithPathPrefix);
  }

  @Test
  public void
      fuzzGetParametersExpectingPathValues_whenGetParameterValueHasPathPrefixAndFileExtension_prefixesPayloadAndAppendsFileExtension() {
    HttpRequest requestWithPathPrefixAndFileExtension =
        HttpRequest.get("https://google.com?key=resources/value.jpg").withEmptyHeaders().build();
    HttpRequest requestWithFuzzedGetParameterWithPathPrefixAndFileExtension =
        HttpRequest.get("https://google.com?key=resources/<payload>%00.jpg")
            .withEmptyHeaders()
            .build();

    assertThat(
            FuzzingUtils.fuzzGetParametersExpectingPathValues(
                requestWithPathPrefixAndFileExtension, "<payload>"))
        .contains(requestWithFuzzedGetParameterWithPathPrefixAndFileExtension);
  }

  @Test
  public void
      fuzzGetParametersExpectingPathValues_whenGetParameterValueHasPathPrefixOrFileExtension_prefixesPayloadOrAppendsFileExtension() {
    HttpRequest requestWithPathPrefixOrFileExtension =
        HttpRequest.get("https://google.com?key=resources./value").withEmptyHeaders().build();
    HttpRequest requestWithFuzzedGetParameterWithPathPrefixAndFileExtension =
        HttpRequest.get("https://google.com?key=resources./<payload>%00./value")
            .withEmptyHeaders()
            .build();

    assertThat(
            FuzzingUtils.fuzzGetParametersExpectingPathValues(
                requestWithPathPrefixOrFileExtension, "<payload>"))
        .doesNotContain(requestWithFuzzedGetParameterWithPathPrefixAndFileExtension);
  }

  @Test
  public void fuzzGetParameters_whenNoGetParameters_returnsEmptyList() {
    assertThat(FuzzingUtils.fuzzGetParameters(REQUEST_WITHOUT_GET_PARAMETERS, "<payload>"))
        .isEmpty();
  }

  @Test
  public void fuzzGetParameters_whenGetParameters_fuzzesAllParameters() {
    ImmutableList<HttpRequest> requestsWithFuzzedGetParameters =
        ImmutableList.of(
            HttpRequest.get("https://google.com?key=<payload>&other=test")
                .withEmptyHeaders()
                .build(),
            HttpRequest.get("https://google.com?key=value&other=<payload>")
                .withEmptyHeaders()
                .build());

    assertThat(FuzzingUtils.fuzzGetParameters(REQUEST_WITH_GET_PARAMETERS, "<payload>"))
        .containsAtLeastElementsIn(requestsWithFuzzedGetParameters);
  }
}
