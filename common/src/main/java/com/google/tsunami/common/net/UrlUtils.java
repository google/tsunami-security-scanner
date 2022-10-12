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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import okhttp3.HttpUrl;

/** Utilities for dealing with URLs. */
public final class UrlUtils {
  private static final Joiner PATH_JOINER = Joiner.on("/");
  private static final Pattern SLASH_PREFIX_PATTERN = Pattern.compile("^/+");
  private static final Pattern TRAILING_SLASH_PATTERN = Pattern.compile("/+$");

  /**
   * Enumerates all sub-paths for a given URL. All query parameters and fragments are removed.
   *
   * <p>For example:
   *
   * <ul>
   *   <li>given <code>"http://localhost/"</code>, it returns <code>["http://localhost/"]</code>
   *   <li>given <code>"http://localhost/a/b/"</code>, it returns <code>
   *       ["http://localhost/", "http://localhost/a/", "http://localhost/a/b/"]</code>
   * </ul>
   *
   * @param url the URL to be enumerated.
   * @return all sub-paths URLs for the given URL.
   */
  public static ImmutableSet<HttpUrl> allSubPaths(String url) {
    return allSubPaths(HttpUrl.parse(url));
  }

  /**
   * Enumerates all sub-paths for a given URL. All query parameters and fragments are removed.
   *
   * <p>For example:
   *
   * <ul>
   *   <li>given <code>"http://localhost/"</code>, it returns <code>["http://localhost/"]</code>
   *   <li>given <code>"http://localhost/a/b/"</code>, it returns <code>
   *       ["http://localhost/", "http://localhost/a/", "http://localhost/a/b/"]</code>
   * </ul>
   *
   * @param url the URL to be enumerated.
   * @return all sub-paths URLs for the given URL.
   */
  public static ImmutableSet<HttpUrl> allSubPaths(HttpUrl url) {
    if (url == null) {
      return ImmutableSet.of();
    }

    // Url at root.
    List<String> pathSegments = url.encodedPathSegments();
    if (pathSegments.size() == 1 && pathSegments.get(0).isEmpty()) {
      return ImmutableSet.of(url.newBuilder().query(null).fragment(null).build());
    }

    // Url has sub-paths.
    ImmutableSet.Builder<HttpUrl> allSubUrlsBuilder = ImmutableSet.builder();
    for (int pathEnd = 0; pathEnd <= pathSegments.size(); pathEnd++) {
      List<String> subPathSegments = Lists.newArrayList(pathSegments.subList(0, pathEnd));
      // Ensure sub-path has leading slash.
      if (subPathSegments.isEmpty() || !subPathSegments.get(0).isEmpty()) {
        subPathSegments.add(0, "");
      }
      // Ensure sub-path has trailing slash.
      if (subPathSegments.size() == 1 || !Iterables.getLast(subPathSegments).isEmpty()) {
        subPathSegments.add("");
      }
      allSubUrlsBuilder.add(
          url.newBuilder()
              .encodedPath(PATH_JOINER.join(subPathSegments))
              .query(null)
              .fragment(null)
              .build());
    }
    return allSubUrlsBuilder.build();
  }

  /**
   * Removes the leading slashes of a URL path.
   *
   * @param path the URL path to be transformed.
   * @return a URL path without leading slash.
   */
  public static String removeLeadingSlashes(String path) {
    return SLASH_PREFIX_PATTERN.matcher(path).replaceFirst("");
  }

  /**
   * Removes the trailing slashes of a URL path.
   *
   * @param path the URL path to be transformed.
   * @return a URL path without leading slash.
   */
  public static String removeTrailingSlashes(String path) {
    return TRAILING_SLASH_PATTERN.matcher(path).replaceFirst("");
  }

  /**
   * Encodes the given String using URL-encoding.
   *
   * @param raw the raw String to be encoded.
   * @return the URL-encoded version of the provided String if it was valid UTF-8.
   */
  public static Optional<String> urlEncode(String raw) {
    try {
      return Optional.of(URLEncoder.encode(raw, UTF_8.toString()));
    } catch (UnsupportedEncodingException e) {
      return Optional.empty();
    }
  }

  private UrlUtils() {}
}
