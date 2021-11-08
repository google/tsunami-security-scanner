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

import com.google.common.collect.ImmutableListMultimap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link HttpHeaders}. */
@RunWith(JUnit4.class)
public class HttpHeadersTest {

  @Test
  public void builderAddHeader_always_putsInHeadersMap() {
    HttpHeaders httpHeaders = HttpHeaders.builder().addHeader("test_header", "test_value").build();
    assertThat(httpHeaders.rawHeaders())
        .containsExactlyEntriesIn(ImmutableListMultimap.of("test_header", "test_value"));
  }

  @Test
  public void builderAddHeader_withKnownHeader_canonicalizesHeaderName() {
    HttpHeaders httpHeaders =
        HttpHeaders.builder()
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT.toUpperCase(), "test_value")
            .build();
    assertThat(httpHeaders.rawHeaders())
        .containsExactlyEntriesIn(
            ImmutableListMultimap.of(com.google.common.net.HttpHeaders.ACCEPT, "test_value"));
  }

  @Test
  public void builderAddHeader_whenEnableCanonicalization_canonicalizesHeaderName() {
    HttpHeaders httpHeaders =
        HttpHeaders.builder()
            .addHeader("TEST_Header", "test_value", true)
            .build();
    assertThat(httpHeaders.rawHeaders())
        .containsExactlyEntriesIn(
            ImmutableListMultimap.of("test_header", "test_value"));
  }

  @Test
  public void builderAddHeader_whenDisableCanonicalization_addsHeaderNameAsIs() {
    HttpHeaders httpHeaders =
        HttpHeaders.builder()
            .addHeader("TEST_Header", "test_value", false)
            .build();
    assertThat(httpHeaders.rawHeaders())
        .containsExactlyEntriesIn(
            ImmutableListMultimap.of("TEST_Header", "test_value"));
  }

  @Test
  public void builderAddHeader_withNullName_throwsNullPointerException() {
    assertThrows(
        NullPointerException.class, () -> HttpHeaders.builder().addHeader(null, "test_value"));
  }

  @Test
  public void builderAddHeader_withNullValue_throwsNullPointerException() {
    assertThrows(
        NullPointerException.class, () -> HttpHeaders.builder().addHeader("test_header", null));
  }

  @Test
  public void builderAddHeader_withIllegalHeaderName_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class, () -> HttpHeaders.builder().addHeader(":::", "test_value"));
  }

  @Test
  public void builderAddHeader_withIllegalHeaderValue_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> HttpHeaders.builder().addHeader("test_header", String.valueOf((char) 11)));
  }

  @Test
  public void names_always_returnsAllHeaderNames() {
    HttpHeaders httpHeaders =
        HttpHeaders.builder()
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "*/*")
            .addHeader(com.google.common.net.HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "text/html")
            .build();

    assertThat(httpHeaders.names())
        .containsExactly(
            com.google.common.net.HttpHeaders.ACCEPT,
            com.google.common.net.HttpHeaders.CONTENT_TYPE);
  }

  @Test
  public void get_whenRequestedHeaderExists_returnsRequestedHeader() {
    HttpHeaders httpHeaders =
        HttpHeaders.builder()
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "*/*")
            .addHeader(com.google.common.net.HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
            .build();

    assertThat(httpHeaders.get(com.google.common.net.HttpHeaders.ACCEPT)).hasValue("*/*");
  }

  @Test
  public void get_whenMultipleValuesExist_returnsFirstValue() {
    HttpHeaders httpHeaders =
        HttpHeaders.builder()
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "*/*")
            .addHeader(com.google.common.net.HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "text/html")
            .build();

    assertThat(httpHeaders.get(com.google.common.net.HttpHeaders.ACCEPT)).hasValue("*/*");
  }

  @Test
  public void get_whenRequestedHeaderDoesNotExist_returnsEmpty() {
    HttpHeaders httpHeaders =
        HttpHeaders.builder()
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "*/*")
            .addHeader(com.google.common.net.HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "text/html")
            .build();

    assertThat(httpHeaders.get(com.google.common.net.HttpHeaders.COOKIE)).isEmpty();
  }

  @Test
  public void get_withNullHeaderName_throwsNullPointerException() {
    HttpHeaders httpHeaders =
        HttpHeaders.builder()
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "*/*")
            .addHeader(com.google.common.net.HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "text/html")
            .build();

    assertThrows(NullPointerException.class, () -> httpHeaders.get(null));
  }

  @Test
  public void getAll_always_returnsAllRequestedValues() {
    HttpHeaders httpHeaders =
        HttpHeaders.builder()
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "*/*")
            .addHeader(com.google.common.net.HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "text/html")
            .build();

    assertThat(httpHeaders.getAll(com.google.common.net.HttpHeaders.ACCEPT))
        .containsExactly("*/*", "text/html");
  }

  @Test
  public void getAll_withKnownHeaderValue_canonicalizesRequestedHeader() {
    HttpHeaders httpHeaders =
        HttpHeaders.builder()
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "*/*")
            .addHeader(com.google.common.net.HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "text/html")
            .build();

    assertThat(httpHeaders.getAll(com.google.common.net.HttpHeaders.ACCEPT.toUpperCase()))
        .containsExactly("*/*", "text/html");
  }

  @Test
  public void getAll_whenRequestValueDoesNotExist_returnsEmptyList() {
    HttpHeaders httpHeaders =
        HttpHeaders.builder()
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "*/*")
            .addHeader(com.google.common.net.HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
            .addHeader(com.google.common.net.HttpHeaders.ACCEPT, "text/html")
            .build();

    assertThat(httpHeaders.getAll(com.google.common.net.HttpHeaders.COOKIE)).isEmpty();
  }
}
