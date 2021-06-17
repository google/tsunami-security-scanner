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
package com.google.tsunami.common.version;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.auto.value.AutoValue;
import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.Immutable;

/** Immutable version range, e.g. [1.0, 2.0), (,3.0), etc. */
@AutoValue
@Immutable
public abstract class VersionRange {
  /** The inclusiveness of the range endpoint. */
  public enum Inclusiveness {
    INCLUSIVE,
    EXCLUSIVE
  }

  public abstract Version minVersion();
  public abstract Inclusiveness minVersionInclusiveness();
  public abstract Version maxVersion();
  public abstract Inclusiveness maxVersionInclusiveness();

  public static Builder builder() {
    return new AutoValue_VersionRange.Builder();
  }

  /** Builder for {@link VersionRange}. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setMinVersion(Version value);
    public abstract Builder setMinVersionInclusiveness(Inclusiveness value);
    public abstract Builder setMaxVersion(Version value);
    public abstract Builder setMaxVersionInclusiveness(Inclusiveness value);

    public abstract VersionRange build();
  }

  /**
   * Parses the given {@code rangeString} and generates a {@link VersionRange} object.
   *
   * <p>Valid strings for a version range are like:
   *
   * <ul>
   *   <li>(,1.0]: from negative infinity to version 1.0 (inclusive).
   *   <li>(,1.0): from negative infinity to version 1.0 (exclusive).
   *   <li>[1.0,): from version 1.0 (inclusive) to positive infinity.
   *   <li>(1.0,): from version 1.0 (exclusive) to positive infinity.
   *   <li>[1.0,2.0): from version 1.0 (inclusive) to version 2.0 (exclusive).
   * </ul>
   *
   * @param rangeString the string representation of a version range.
   * @return the parsed {@link VersionRange} object from the given string.
   */
  public static VersionRange parse(String rangeString) {
    validateRangeString(rangeString);

    Inclusiveness minVersionInclusiveness =
        rangeString.startsWith("[") ? Inclusiveness.INCLUSIVE : Inclusiveness.EXCLUSIVE;
    Inclusiveness maxVersionInclusiveness =
        rangeString.endsWith("]") ? Inclusiveness.INCLUSIVE : Inclusiveness.EXCLUSIVE;

    int commaIndex = rangeString.indexOf(',');

    String minVersionString = rangeString.substring(1, commaIndex).trim();
    Version minVersion;
    if (minVersionString.isEmpty()) {
      minVersionInclusiveness = Inclusiveness.EXCLUSIVE;
      minVersion = Version.minimum();
    } else {
      minVersion = Version.fromString(minVersionString);
    }

    String maxVersionString =
        rangeString.substring(commaIndex + 1, rangeString.length() - 1).trim();
    Version maxVersion;
    if (maxVersionString.isEmpty()) {
      maxVersionInclusiveness = Inclusiveness.EXCLUSIVE;
      maxVersion = Version.maximum();
    } else {
      maxVersion = Version.fromString(maxVersionString);
    }

    if (!minVersion.isLessThan(maxVersion)) {
      throw new IllegalArgumentException(
          String.format(
              "Min version in range must be less than max version in range, got '%s'",
              rangeString));
    }

    return builder()
        .setMinVersion(minVersion)
        .setMinVersionInclusiveness(minVersionInclusiveness)
        .setMaxVersion(maxVersion)
        .setMaxVersionInclusiveness(maxVersionInclusiveness)
        .build();
  }

  public static boolean isValidVersionRange(String rangeString) {
    try {
      parse(rangeString);
      return true;
    } catch (Throwable t) {
      return false;
    }
  }

  private static void validateRangeString(String rangeString) {
    checkArgument(!Strings.isNullOrEmpty(rangeString), "Range string cannot be empty.");

    // Version range string must start with '[' or '('.
    if (!rangeString.startsWith("[") && !rangeString.startsWith("(")) {
      throw new IllegalArgumentException(
          String.format("Version range must start with '[' or '(', got '%s'", rangeString));
    }

    // Version range string must end with ']' or ')'.
    if (!rangeString.endsWith("]") && !rangeString.endsWith(")")) {
      throw new IllegalArgumentException(
          String.format("Version range must end with ']' or ')', got '%s'", rangeString));
    }

    // Remove the leading and ending parenthesis and brackets.
    String trimmedRange = rangeString.substring(1, rangeString.length() - 1).trim();

    // No more parenthesis and brackets in the string.
    if (CharMatcher.anyOf("[()]").matchesAnyOf(trimmedRange)) {
      throw new IllegalArgumentException(
          String.format(
              "Parenthesis and/or brackets not allowed within version range, got '%s'",
              rangeString));
    }

    // Only one comma that separates the minimum and maximum.
    if (CharMatcher.is(',').countIn(trimmedRange) != 1) {
      throw new IllegalArgumentException(
          String.format("Invalid range of versions, got '%s'", rangeString));
    }

    // Version range of minimum to maximum is not supported.
    if (trimmedRange.equals(",")) {
      throw new IllegalArgumentException(
          String.format("Infinity range is not supported, got '%s'", rangeString));
    }
  }
}
