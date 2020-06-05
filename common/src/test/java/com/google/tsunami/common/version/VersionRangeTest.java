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

import com.google.tsunami.common.version.VersionRange.Inclusiveness;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link VersionRange}. */
@RunWith(JUnit4.class)
public class VersionRangeTest {

  @Test
  public void parse_withNegativeInfinityRange_returnsCorrectVersionRange() {
    VersionRange versionRange = VersionRange.parse("(,1.0]");

    assertThat(versionRange)
        .isEqualTo(
            VersionRange.builder()
                .setMinVersion(Version.minimum())
                .setMinVersionInclusiveness(Inclusiveness.EXCLUSIVE)
                .setMaxVersion(Version.fromString("1.0"))
                .setMaxVersionInclusiveness(Inclusiveness.INCLUSIVE)
                .build());
  }

  @Test
  public void parse_withPositiveInfinityRange_returnsCorrectVersionRange() {
    VersionRange versionRange = VersionRange.parse("(1.0,)");

    assertThat(versionRange)
        .isEqualTo(
            VersionRange.builder()
                .setMinVersion(Version.fromString("1.0"))
                .setMinVersionInclusiveness(Inclusiveness.EXCLUSIVE)
                .setMaxVersion(Version.maximum())
                .setMaxVersionInclusiveness(Inclusiveness.EXCLUSIVE)
                .build());
  }

  @Test
  public void parse_withRegularRange_returnsCorrectVersionRange() {
    VersionRange versionRange = VersionRange.parse("(1.0,2.0]");

    assertThat(versionRange)
        .isEqualTo(
            VersionRange.builder()
                .setMinVersion(Version.fromString("1.0"))
                .setMinVersionInclusiveness(Inclusiveness.EXCLUSIVE)
                .setMaxVersion(Version.fromString("2.0"))
                .setMaxVersionInclusiveness(Inclusiveness.INCLUSIVE)
                .build());
  }

  @Test
  public void parse_withEmptyRangeString_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse(""));
    assertThat(exception).hasMessageThat().isEqualTo("Range string cannot be empty.");
  }

  @Test
  public void parse_withRangeNotStartingWithParenthesis_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse(",1.0]"));
    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Version range must start with '[' or '(', got ',1.0]'");
  }

  @Test
  public void parse_withRangeNotEndingWithParenthesis_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("(,1.0"));
    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Version range must end with ']' or ')', got '(,1.0'");
  }

  @Test
  public void parse_withTooManyParenthesis_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("(,1.0]]"));
    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Parenthesis and/or brackets not allowed within version range, got '(,1.0]]'");
  }

  @Test
  public void parse_withTooManyCommas_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("(,,,1.0]"));
    assertThat(exception).hasMessageThat().isEqualTo("Invalid range of versions, got '(,,,1.0]'");
  }

  @Test
  public void parse_withoutComma_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("[1.0]"));
    assertThat(exception).hasMessageThat().isEqualTo("Invalid range of versions, got '[1.0]'");
  }

  @Test
  public void parse_withMinimalToMaximalRange_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("(,)"));
    assertThat(exception).hasMessageThat().isEqualTo("Infinity range is not supported, got '(,)'");
  }

  @Test
  public void parse_withTheSameRangeEnds_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("[1.0,1.0]"));
    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Min version in range must be less than max version in range, got '[1.0,1.0]'");
  }
}
