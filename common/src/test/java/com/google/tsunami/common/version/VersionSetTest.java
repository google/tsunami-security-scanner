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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link VersionSet}. */
@RunWith(JUnit4.class)
public class VersionSetTest {

  @Test
  public void parse_withValidVersionsAndVersionRanges_returnsParsedVersionSet() {
    VersionSet versionSet =
        VersionSet.parse(ImmutableList.of("1.0", "1.3", "(1.4, 1.7]", "1.9", "[2.0,)"));

    assertThat(versionSet.versions())
        .containsExactly(
            Version.fromString("1.0"), Version.fromString("1.3"), Version.fromString("1.9"));
    assertThat(versionSet.versionRanges())
        .containsExactly(VersionRange.parse("(1.4,1.7]"), VersionRange.parse("[2.0,)"));
  }

  @Test
  public void parse_withEmptyInputList_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> VersionSet.parse(ImmutableList.of()));
    assertThat(exception).hasMessageThat().isEqualTo("Versions and ranges list cannot be empty.");
  }

  @Test
  public void parse_withInvalidVersion_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> VersionSet.parse(ImmutableList.of("1,0", "abc")));
    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("String '1,0' is neither a discrete string nor a version range.");
  }
}
