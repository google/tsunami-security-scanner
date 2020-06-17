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
import static com.google.tsunami.common.net.UrlUtils.allSubPaths;
import static com.google.tsunami.common.net.UrlUtils.removeLeadingSlashes;
import static com.google.tsunami.common.net.UrlUtils.removeTrailingSlashes;

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
}
