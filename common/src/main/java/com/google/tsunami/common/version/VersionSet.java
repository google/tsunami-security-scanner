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
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;

/** Immutable set of discrete versions and version ranges. */
@AutoValue
@Immutable
public abstract class VersionSet {
  public abstract ImmutableList<Version> versions();
  public abstract ImmutableList<VersionRange> versionRanges();

  public static Builder builder() {
    return new AutoValue_VersionSet.Builder();
  }

  /** Builder for {@link VersionSet}. */
  @AutoValue.Builder
  public abstract static class Builder {
    abstract ImmutableList.Builder<Version> versionsBuilder();
    public Builder addVersion(Version version) {
      versionsBuilder().add(version);
      return this;
    }

    abstract ImmutableList.Builder<VersionRange> versionRangesBuilder();
    public Builder addVersionRange(VersionRange versionRange) {
      versionRangesBuilder().add(versionRange);
      return this;
    }

    public abstract VersionSet build();
  }

  public static VersionSet parse(ImmutableList<String> versionAndRangesList) {
    checkNotNull(versionAndRangesList);
    checkArgument(!versionAndRangesList.isEmpty(), "Versions and ranges list cannot be empty.");

    VersionSet.Builder versionSetBuilder = VersionSet.builder();

    for (String versionOrRangeString : versionAndRangesList) {
      if (isDiscreteVersion(versionOrRangeString)) {
        versionSetBuilder.addVersion(Version.fromString(versionOrRangeString));
      } else if (VersionRange.isValidVersionRange(versionOrRangeString)) {
        versionSetBuilder.addVersionRange(VersionRange.parse(versionOrRangeString));
      } else {
        throw new IllegalArgumentException(
            String.format(
                "String '%s' is neither a discrete string nor a version range.",
                versionOrRangeString));
      }
    }

    return versionSetBuilder.build();
  }

  private static boolean isDiscreteVersion(String versionOrRangeString) {
    return CharMatcher.anyOf("[()], ").matchesNoneOf(versionOrRangeString);
  }
}
