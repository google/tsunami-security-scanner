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
package com.google.tsunami.common.net;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.tsunami.common.net.UrlUtils.allSubPaths;
import static com.google.tsunami.common.net.UrlUtils.removeLeadingSlashes;
import static com.google.tsunami.common.net.UrlUtils.removeTrailingSlashes;
import static com.google.tsunami.common.net.UrlUtils.urlEncode;

import okhttp3.HttpUrl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link UrlUtils}. */
@RunWith(JUnit4.class)
public final class UrlUtilsTest {

  @Test
  public void allSubPaths_whenInvalidUrl_returnsEmptyList() {
    assertThat(allSubPaths("invalid_url")).isEmpty();
  }

  @Test
  public void allSubPaths_whenNoSubPathNoTrailingSlash_returnsSingleUrl() {
    assertThat(allSubPaths("http://localhost")).containsExactly(HttpUrl.parse("http://localhost/"));
  }

  @Test
  public void allSubPaths_whenNoSubPathWithTrailingSlash_returnsSingleUrl() {
    assertThat(allSubPaths("http://localhost/"))
        .containsExactly(HttpUrl.parse("http://localhost/"));
  }

  @Test
  public void allSubPaths_whenValidQueryParamsAndFragments_removesParamsAndFragments() {
    assertThat(allSubPaths("http://localhost/?param=value&param2=value2#abc"))
        .containsExactly(HttpUrl.parse("http://localhost/"));
  }

  @Test
  public void allSubPaths_whenSingleSubPathsNoTrailingSlash_returnsExpectedUrl() {
    assertThat(allSubPaths("http://localhost/a"))
        .containsExactly(HttpUrl.parse("http://localhost/"), HttpUrl.parse("http://localhost/a/"));
  }

  @Test
  public void allSubPaths_whenSingleSubPathsWithTrailingSlash_returnsExpectedUrl() {
    assertThat(allSubPaths("http://localhost/a/"))
        .containsExactly(HttpUrl.parse("http://localhost/"), HttpUrl.parse("http://localhost/a/"));
  }

  @Test
  public void allSubPaths_whenMultipleSubPathsNoTrailingSlash_returnsExpectedUrl() {
    assertThat(allSubPaths("http://localhost/a/b/c"))
        .containsExactly(
            HttpUrl.parse("http://localhost/"),
            HttpUrl.parse("http://localhost/a/"),
            HttpUrl.parse("http://localhost/a/b/"),
            HttpUrl.parse("http://localhost/a/b/c/"));
  }

  @Test
  public void allSubPaths_whenMultipleSubPathsWithTrailingSlash_returnsExpectedUrl() {
    assertThat(allSubPaths("http://localhost/a/b/c/"))
        .containsExactly(
            HttpUrl.parse("http://localhost/"),
            HttpUrl.parse("http://localhost/a/"),
            HttpUrl.parse("http://localhost/a/b/"),
            HttpUrl.parse("http://localhost/a/b/c/"));
  }

  @Test
  public void allSubPaths_whenMultipleSubPathsWithParamsAndFragments_returnsExpectedUrl() {
    assertThat(allSubPaths("http://localhost/a/b/c/?param=value&param2=value2#abc"))
        .containsExactly(
            HttpUrl.parse("http://localhost/"),
            HttpUrl.parse("http://localhost/a/"),
            HttpUrl.parse("http://localhost/a/b/"),
            HttpUrl.parse("http://localhost/a/b/c/"));
  }

  @Test
  public void removeLeadingSlashes_whenNoLeadingSlashes_returnsOriginal() {
    assertThat(removeLeadingSlashes("a/b/c/")).isEqualTo("a/b/c/");
  }

  @Test
  public void removeLeadingSlashes_whenSingleLeadingSlash_removesLeadingSlashes() {
    assertThat(removeLeadingSlashes("/a/b/c/")).isEqualTo("a/b/c/");
  }

  @Test
  public void removeLeadingSlashes_whenMultipleLeadingSlashes_removesLeadingSlashes() {
    assertThat(removeLeadingSlashes("/////a/b/c/")).isEqualTo("a/b/c/");
  }

  @Test
  public void removeTrailingSlashes_whenNoTrailingSlashes_returnsOriginal() {
    assertThat(removeTrailingSlashes("/a/b/c")).isEqualTo("/a/b/c");
  }

  @Test
  public void removeTrailingSlashes_whenSingleTrailingSlash_removesTrailingSlashes() {
    assertThat(removeTrailingSlashes("/a/b/c/")).isEqualTo("/a/b/c");
  }

  @Test
  public void removeTrailingSlashes_whenMultipleTrailingSlashes_removesTrailingSlashes() {
    assertThat(removeTrailingSlashes("/a/b/c/////")).isEqualTo("/a/b/c");
  }

  @Test
  public void urlEncode_whenEmptyString_returnsOriginal() {
    assertThat(urlEncode("")).hasValue("");
  }

  @Test
  public void urlEncode_whenNothingToEncode_returnsOriginal() {
    assertThat(urlEncode("abcdefghijklmnopqrstuvwxyz")).hasValue("abcdefghijklmnopqrstuvwxyz");
    assertThat(urlEncode("ABCDEFGHIJKLMNOPQRSTUVWXYZ")).hasValue("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    assertThat(urlEncode("0123456789")).hasValue("0123456789");
    assertThat(urlEncode("-_.*")).hasValue("-_.*");
  }

  @Test
  public void urlEncode_whenNotEncoded_returnsEncoded() {
    assertThat(urlEncode(" ")).hasValue("+");
    assertThat(urlEncode("()[]{}<>")).hasValue("%28%29%5B%5D%7B%7D%3C%3E");
    assertThat(urlEncode("?!@#$%^&=+,;:'\"`/\\|~"))
        .hasValue("%3F%21%40%23%24%25%5E%26%3D%2B%2C%3B%3A%27%22%60%2F%5C%7C%7E");
  }

  @Test
  public void urlEncode_whenAlreadyEncoded_encodesAgain() {
    assertThat(urlEncode("%2F")).hasValue("%252F");
    assertThat(urlEncode("%252F")).hasValue("%25252F");
  }

  @Test
  public void urlEncode_whenComplexEncoding_encodesCorrectly() {
    assertThat(urlEncode("£")).hasValue("%C2%A3");
    assertThat(urlEncode("つ")).hasValue("%E3%81%A4");
    assertThat(urlEncode("äëïöüÿ")).hasValue("%C3%A4%C3%AB%C3%AF%C3%B6%C3%BC%C3%BF");
    assertThat(urlEncode("ÄËÏÖÜŸ")).hasValue("%C3%84%C3%8B%C3%8F%C3%96%C3%9C%C5%B8");
  }

  @Test
  public void urlEncode_whenUnicode_encodesOriginal() {
    // EURO sign
    assertThat(urlEncode("\u20AC")).hasValue("%E2%82%AC");
  }
}
