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
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Software version class that support 3 types. Type {@link Type#NORMAL} with a version number, type
 * {@link Type#MAXIMUM} and {@link Type#MINIMUM} for use in version range to indicate unbounded
 * ranges.
 *
 * <p>Version is suitable for unstructured version string and supports comparison operations using
 * extra logic that detects semantic qualifier.
 *
 * <p>The current logic support comparison of versions that respect order, like:
 *
 * <pre>
 *   10
 *   1.1
 *   1.1a
 *   1.1a1
 *   20170101
 *   2017.01.01
 *   1.1rc1
 *   1.1-1p1
 *   1.1patch1
 *   1.1gg1.0
 *   1-1
 *   1:1.1
 *   1.1.alpha
 *   1.1.beta.1
 *   1.1.alpha.beta
 * </pre>
 *
 * Known limitation of this approach are versions with no order, like commit hashes.
 */
@AutoValue
@Immutable
public abstract class Version implements Comparable<Version> {
  private static final Pattern EPOCH_PATTERN = Pattern.compile("\\d+[:|_].*");
  private static final Pattern SEMANTIC_SEGMENT_SEPARATORS = Pattern.compile("[-:_~]");
  private static final Version MAXIMUM =
      builder().setVersionType(Type.MAXIMUM).setVersionString("").build();
  private static final Version MINIMUM =
      builder().setVersionType(Type.MINIMUM).setVersionString("").build();

  /**
   * Software version class that support 3 types. Type {@link Type#NORMAL} with a version number,
   * type {@link Type#MAXIMUM} and {@link Type#MINIMUM} for use in version range to indicate
   * unbounded ranges.
   */
  public enum Type {
    NORMAL,
    MINIMUM,
    MAXIMUM
  }

  abstract Type versionType();
  public abstract String versionString();

  @Memoized
  ImmutableList<Segment> segments() {
    String normalizedString = versionString();
    // Add a default epoch of 0 if one is missing.
    if (!EPOCH_PATTERN.matcher(normalizedString).matches()) {
      normalizedString = "0:" + normalizedString;
    }

    return Arrays.stream(SEMANTIC_SEGMENT_SEPARATORS.split(normalizedString))
        .filter(segment -> !segment.isEmpty())
        .map(Segment::fromString)
        .filter(segment -> !segment.equals(Segment.NULL))
        .collect(ImmutableList.toImmutableList());
  }

  public static Builder builder() {
    return new AutoValue_Version.Builder();
  }

  public static Version fromString(String versionString) {
    checkArgument(!Strings.isNullOrEmpty(versionString));
    Version version = builder().setVersionType(Type.NORMAL).setVersionString(versionString).build();
    if (!EPOCH_PATTERN.matcher(versionString).matches()) {
      versionString = "0:" + versionString;
    }

    boolean isValid =
        version.segments().stream()
            .flatMap(segment -> segment.tokens().stream())
            .anyMatch(
                token ->
                    (token.isNumeric() && token.getNumeric() != 0)
                        || (token.isText() && !token.getText().isEmpty()));
    if (!isValid) {
      throw new IllegalArgumentException(
          String.format(
              "Input version string %s is not valid, it should contain at least one non-empty"
                  + " field.",
              versionString));
    }
    return version;
  }

  public static Version maximum() {
    return MAXIMUM;
  }

  public boolean isMaximum() {
    return versionType().equals(Type.MAXIMUM);
  }

  public static Version minimum() {
    return MINIMUM;
  }

  public boolean isMinimum() {
    return versionType().equals(Type.MINIMUM);
  }

  /**
   * Compare this Version object with the other one using their meaningful segments.
   *
   * <p>IMPORTANT: This compareTo implementation is NOT consistent with {@link
   * Version#equals(Object)} method, i.e. this.compareTo(that) == 0 does not imply
   * this.equals(that). The reason is that the raw version string is tokenized and certain tokens
   * are ignored. Tokenized strings are used for {@link #compareTo} comparison while raw version
   * string is used for {@link #equals} comparison. Be careful when using {@link Version} object in
   * collections like {@code HashMap} or {@code TreeMap}.
   *
   * @param other the other {@link Version} object to be compared with.
   * @return 0 if the segments of the two {@link Version} objects are the same, -1 if this {@link
   *     Version} is less than {@code other}, 1 if this {@link Version} is greater than {@code
   *     other}.
   */
  @Override
  public int compareTo(Version other) {
    if ((this.isMinimum() && other.isMinimum()) || (this.isMaximum() && other.isMaximum())) {
      return 0;
    }

    if (this.isMinimum() || other.isMaximum()) {
      return -1;
    }

    if (this.isMaximum() || other.isMinimum()) {
      return 1;
    }

    return ComparisonUtility.compareListWithFillValue(
        this.segments(), other.segments(), Segment.NULL);
  }

  public boolean isLessThan(Version version) {
    return this.compareTo(version) < 0;
  }

  /** Builder for {@link Version}. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setVersionType(Type value);
    public abstract Builder setVersionString(String value);

    public abstract Version build();
  }
}
