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

import com.google.auto.value.AutoValue;
import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import java.lang.reflect.Field;
import java.util.Optional;

/** Immutable HTTP headers. */
@Immutable
@AutoValue
public abstract class HttpHeaders {
  private static final ImmutableBiMap<String, String> LOWER_TO_KNOWN = createKnownHeaders();
  private static final ImmutableSet<String> KNOWN = LOWER_TO_KNOWN.values();

  /** Canonicalize a header name. */
  private static String canonicalize(String headerName) {
    if (KNOWN.contains(headerName)) {
      return headerName;
    }
    String lower = Ascii.toLowerCase(headerName);
    String known = LOWER_TO_KNOWN.get(lower);
    return MoreObjects.firstNonNull(known, lower);
  }

  private static ImmutableBiMap<String, String> createKnownHeaders() {
    ImmutableBiMap.Builder<String, String> builder = ImmutableBiMap.builder();
    addFields(builder, com.google.common.net.HttpHeaders.class);
    return builder.build();
  }

  /**
   * Loops over all of the public String fields in the given class and puts them into the BiMap
   * (lower case to original string value).
   */
  private static void addFields(ImmutableBiMap.Builder<String, String> builder, Class<?> clazz) {
    try {
      for (Field field : clazz.getFields()) {
        if (field.getType().equals(String.class)) {
          String known = (String) field.get(null);
          String lower = Ascii.toLowerCase(known);
          builder.put(lower, known);
        }
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  abstract ImmutableListMultimap<String, String> rawHeaders();

  /**
   * Gets a set of all HTTP header names.
   *
   * @return all HTTP header names.
   */
  public ImmutableSet<String> names() {
    return rawHeaders().keySet();
  }

  /**
   * Returns the first value for the header with the given name, or empty Optional if none exists.
   *
   * @param name case-insensitive header name
   * @return the first value for the given header name.
   */
  public Optional<String> get(String name) {
    checkNotNull(name, "Name cannot be null.");

    ImmutableList<String> values = getAll(name);
    return values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
  }

  /**
   * Returns all the values for the header with the given name. Values are in the same order they
   * were added to the builder.
   *
   * @param name case-insensitive header name
   * @return All values for the given header name.
   */
  public ImmutableList<String> getAll(String name) {
    checkNotNull(name, "Name cannot be null.");

    // We first check the multimap using whatever string is passed in. Usually
    // this will be a constant from HttpHeaders, which is pre-canonicalized.
    // Only if the lookup fails do we then canonicalize and try again.
    ImmutableList<String> values = rawHeaders().get(name);
    if (!values.isEmpty()) {
      return values;
    }
    String fixedName = canonicalize(name);
    if (fixedName.equals(name)) {
      return values; // Name was already canonicalized, so return the empty list.
    }
    return rawHeaders().get(fixedName);
  }

  public static Builder builder() {
    return new AutoValue_HttpHeaders.Builder();
  }

  /** Builder for {@link HttpHeaders}. */
  @AutoValue.Builder
  public abstract static class Builder {
    /** RFC 2616 section 4.2. */
    private static final CharMatcher HEADER_NAME_MATCHER =
        CharMatcher.inRange('!', '~').and(CharMatcher.isNot(':'));
    /** RFC 2616 section 4.2. */
    private static final CharMatcher HEADER_VALUE_MATCHER =
        CharMatcher.inRange((char) 0, (char) 31) // No control characters
            .or(CharMatcher.is((char) 127)) // or DEL
            .negate()
            .or(CharMatcher.is('\t')); // except horizontal-tab

    abstract ImmutableListMultimap.Builder<String, String> rawHeadersBuilder();

    public Builder addHeader(String name, String value) {
      checkNotNull(name, "Name cannot be null.");
      checkNotNull(value, "Value cannot be null.");
      checkArgument(isLegalHeaderName(name), "Illegal header name %s", name);
      checkArgument(isLegalHeaderValue(value), "Illegal header value %s", value);
      rawHeadersBuilder().put(canonicalize(name), value);
      return this;
    }

    public Builder addHeader(String name, String value, boolean canonicalize) {
      checkNotNull(name, "Name cannot be null.");
      checkNotNull(value, "Value cannot be null.");
      if (canonicalize) {
        return addHeader(name, value);
      } else {
        rawHeadersBuilder().put(name, value);
        return this;
      }
    }

    public abstract HttpHeaders build();

    private static boolean isLegalHeaderName(String str) {
      return HEADER_NAME_MATCHER.matchesAllOf(str);
    }

    private static boolean isLegalHeaderValue(String value) {
      return HEADER_VALUE_MATCHER.matchesAllOf(value);
    }
  }
}
