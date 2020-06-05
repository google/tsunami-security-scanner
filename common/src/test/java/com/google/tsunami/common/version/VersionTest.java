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
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/** Tests for {@link Version}. */
@RunWith(Theories.class)
public final class VersionTest {

  @Test
  public void create_whenNormalVersionAndValueIsNull_throwsException() {
    assertThrows(IllegalArgumentException.class, () -> Version.fromString(null));
  }

  @Test
  public void create_whnNormalVersionAndValueIsEmpty_throwsExceptionIfStringIsNull() {
    assertThrows(IllegalArgumentException.class, () -> Version.fromString(""));
  }

  @Test
  public void create_whenNormalVersion_returnsTypeNormal() {
    Version version = Version.fromString("1.1");

    assertThat(version.versionType()).isEqualTo(Version.Type.NORMAL);
  }

  @Test
  public void createMaximum_always_returnsTypeMaximum() {
    Version version = Version.maximum();

    assertThat(version.versionType()).isEqualTo(Version.Type.MAXIMUM);
  }

  @Test
  public void createMaximum_always_returnsTypeMinimum() {
    Version version = Version.minimum();

    assertThat(version.versionType()).isEqualTo(Version.Type.MINIMUM);
  }

  @DataPoints("InvalidVersion")
  public static ImmutableList<String> invalidVersionTestCases() {
    return ImmutableList.of("", "N/A", "...", "-");
  }

  @Theory
  public void fromString_invalidVersionString_throwsIllegalArgumentException(
      @FromDataPoints("InvalidVersion") String version) {
    assertThrows(IllegalArgumentException.class, () -> Version.fromString(version));
  }

  @Test
  public void fromString_validString_storesInputAsRawString() {
    assertThat(Version.fromString("1.0").versionString()).isEqualTo("1.0");
  }

  @Test
  public void fromString_noEpoch_appendsZeroEpoch() {
    assertThat(Version.fromString("1.0").segments())
        .containsExactly(Segment.fromString("0"), Segment.fromString("1.0"));
  }

  @Test
  public void fromString_withEpoch_epochIsParsed() {
    assertThat(Version.fromString("1:1.0").segments())
        .containsExactly(Segment.fromString("1"), Segment.fromString("1.0"));
  }

  @Test
  public void fromString_withMultipleSegments_segmentsParsedCorrectly() {
    assertThat(Version.fromString("1:9.7.0.dfsg.P1-gg3.0").segments())
        .containsExactly(
            Segment.fromString("1"),
            Segment.fromString("9.7.0.dfsg.P1"),
            Segment.fromString("3.0"));
  }

  @DataPoints("Equals")
  public static ImmutableList<EqualsTestCase<Version>> equalsTestCases() {
    return ImmutableList.of(
        EqualsTestCase.create(Version.fromString("1.0"), Version.fromString("1.0")),
        EqualsTestCase.create(Version.fromString("1.0"), Version.fromString("0:1.0")),
        EqualsTestCase.create(Version.fromString("1.0-"), Version.fromString("1.0-gg")),
        EqualsTestCase.create(Version.maximum(), Version.maximum()),
        EqualsTestCase.create(Version.minimum(), Version.minimum()));
  }

  @Theory
  public void compareTo_equalsTestCase_returnsZero(
      @FromDataPoints("Equals") EqualsTestCase<Version> testCase) {
    assertThat(testCase.first()).isEquivalentAccordingToCompareTo(testCase.second());
  }

  @DataPoints("LessThan")
  public static ImmutableList<LessThanTestCase<Version>> lessThanTestCases() {
    return ImmutableList.of(
        LessThanTestCase.create(Version.minimum(), Version.fromString("1.0")),
        LessThanTestCase.create(Version.minimum(), Version.maximum()),
        LessThanTestCase.create(Version.fromString("1.0"), Version.maximum()),
        LessThanTestCase.create(Version.fromString("0.9"), Version.fromString("1.0")),
        LessThanTestCase.create(Version.fromString("1.0-0"), Version.fromString("1.0-110313082")),
        LessThanTestCase.create(
            Version.fromString("0.161-gg2.0"), Version.fromString("0.165-gg1.0")),
        LessThanTestCase.create(Version.fromString("0.87-gg1.2"), Version.fromString("0.87-gg1.3")),
        LessThanTestCase.create(
            Version.fromString("2017b-gg1.0"), Version.fromString("2018b-gg0.")),
        LessThanTestCase.create(Version.fromString("18-4"), Version.fromString("19-1")),
        LessThanTestCase.create(
            Version.fromString("1:9.7.0.dfsg.P1-gg3.0"),
            Version.fromString("1:9.8.0.dfsg.P0-gg1.0")),
        LessThanTestCase.create(Version.fromString("5.7-gg2.0"), Version.fromString("5.8-gg1.0")),
        LessThanTestCase.create(
            Version.fromString("2.4.6-12"), Version.fromString("2.4.6-12+patched1")),
        LessThanTestCase.create(Version.fromString("20170727-1"), Version.fromString("20170801-1")),
        LessThanTestCase.create(
            Version.fromString("3.12-443-ga51ea6dc8202-gg2.2"),
            Version.fromString("3.13-440-ga51eadfe472-gg1.0")),
        LessThanTestCase.create(Version.fromString("1.0a"), Version.fromString("1.0b")),
        LessThanTestCase.create(Version.fromString("1a"), Version.fromString("2")),
        LessThanTestCase.create(
            Version.fromString("4d01146f1679dd90bba45adb60d24ad11fe1155e"),
            Version.fromString("e40b437401966fe06b0c4d5430c35e4494675c90")),
        LessThanTestCase.create(Version.fromString("7.10"), Version.fromString("1_7.9")),
        LessThanTestCase.create(Version.fromString("9.10"), Version.fromString("1_9.11")),
        LessThanTestCase.create(
            Version.fromString("1.0.0-alpha"), Version.fromString("1.0.0-alpha.1")),
        LessThanTestCase.create(
            Version.fromString("1.0.0-alpha.1"), Version.fromString("1.0.0-alpha.beta")),
        LessThanTestCase.create(
            Version.fromString("1.0.0-alpha.beta"), Version.fromString("1.0.0-beta")),
        LessThanTestCase.create(
            Version.fromString("1.0.0-beta"), Version.fromString("1.0.0-beta.2")),
        LessThanTestCase.create(
            Version.fromString("1.0.0-beta.2"), Version.fromString("1.0.0-beta.11")),
        LessThanTestCase.create(
            Version.fromString("1.0.0-beta.11"), Version.fromString("1.0.0-rc.1")),
        LessThanTestCase.create(Version.fromString("1.0.0-rc.1"), Version.fromString("1.0.0")),
        LessThanTestCase.create(Version.fromString("1.0.0-alpha"), Version.fromString("1.0.0")),
        LessThanTestCase.create(Version.fromString("1.0.0-alpha.1"), Version.fromString("1.0.0")),
        LessThanTestCase.create(
            Version.fromString("1.0.0-alpha.beta"), Version.fromString("1.0.0")),
        LessThanTestCase.create(Version.fromString("1.0.0-beta"), Version.fromString("1.0.0")),
        LessThanTestCase.create(Version.fromString("1.0.0-beta.2"), Version.fromString("1.0.0")),
        LessThanTestCase.create(Version.fromString("1.0.0-beta.11"), Version.fromString("1.0.0")),
        LessThanTestCase.create(Version.fromString("7.6-0"), Version.fromString("7.6p2-4")),
        LessThanTestCase.create(Version.fromString("1.0-1"), Version.fromString("1.0.3-3")),
        LessThanTestCase.create(Version.fromString("1.2.2-2"), Version.fromString("1.3")),
        LessThanTestCase.create(Version.fromString("1.2.2"), Version.fromString("1.3")),
        LessThanTestCase.create(Version.fromString("0-pre"), Version.fromString("0-pree")),
        LessThanTestCase.create(Version.fromString("1.1.6r-1"), Version.fromString("1.1.6r2-2")),
        LessThanTestCase.create(Version.fromString("2.6b-2"), Version.fromString("2.6b2-1")),
        LessThanTestCase.create(
            Version.fromString("98.1-pre2-b6-2"), Version.fromString("98.1p5-1")),
        LessThanTestCase.create(Version.fromString("0.4-1"), Version.fromString("0.4a6-2")),
        LessThanTestCase.create(Version.fromString("1:3.0.5-2"), Version.fromString("1:3.0.5.1")),
        LessThanTestCase.create(Version.fromString("10.3"), Version.fromString("1:0.4")),
        LessThanTestCase.create(Version.fromString("1:1.25-4"), Version.fromString("1:1.25-8")),
        LessThanTestCase.create(Version.fromString("1.18.35"), Version.fromString("1.18.36")),
        LessThanTestCase.create(Version.fromString("1.18.35"), Version.fromString("0:1.18.36")),
        LessThanTestCase.create(
            Version.fromString("9:1.18.36:5.4-20"), Version.fromString("10:0.5.1-22")),
        LessThanTestCase.create(
            Version.fromString("9:1.18.36:5.4-20"), Version.fromString("9:1.18.36:5.5-1")),
        LessThanTestCase.create(
            Version.fromString("9:1.18.36:5.4-20"), Version.fromString("9:1.18.37:4.3-22")),
        LessThanTestCase.create(
            Version.fromString("1.18.36-0.17.35-18"), Version.fromString("1.18.36-19")),
        LessThanTestCase.create(
            Version.fromString("1:1.2.13-3"), Version.fromString("1:1.2.13-3.1")),
        LessThanTestCase.create(Version.fromString("2.0.7pre1-4"), Version.fromString("2.0.7r-1")),
        LessThanTestCase.create(Version.fromString("0.2"), Version.fromString("1.0-0")),
        LessThanTestCase.create(Version.fromString("1.0"), Version.fromString("1.0-0+b1")),
        LessThanTestCase.create(Version.fromString("1.2.3"), Version.fromString("1.2.3-1")),
        LessThanTestCase.create(Version.fromString("1.2.3"), Version.fromString("1.2.4")),
        LessThanTestCase.create(Version.fromString("1.2.3"), Version.fromString("1.2.4")),
        LessThanTestCase.create(Version.fromString("1.2.3"), Version.fromString("1.2.24")),
        LessThanTestCase.create(Version.fromString("0.8.7"), Version.fromString("0.10.0")),
        LessThanTestCase.create(Version.fromString("2.3"), Version.fromString("3.2")),
        LessThanTestCase.create(Version.fromString("1.3.2"), Version.fromString("1.3.2a")),
        LessThanTestCase.create(Version.fromString("0.5.0~git"), Version.fromString("0.5.0~git2")),
        LessThanTestCase.create(Version.fromString("2a"), Version.fromString("21")),
        LessThanTestCase.create(Version.fromString("1.3.2a"), Version.fromString("1.3.2b")),
        LessThanTestCase.create(Version.fromString("1.2.4"), Version.fromString("1:1.2.3")),
        LessThanTestCase.create(Version.fromString("1:1.2.3"), Version.fromString("1:1.2.4")),
        LessThanTestCase.create(Version.fromString("1.2a+~bCd3"), Version.fromString("1.2a++")),
        LessThanTestCase.create(Version.fromString("1.2a+~"), Version.fromString("1.2a+~bCd3")),
        LessThanTestCase.create(Version.fromString("304-2"), Version.fromString("5:2")),
        LessThanTestCase.create(Version.fromString("5:2"), Version.fromString("304:2")),
        LessThanTestCase.create(Version.fromString("3:2"), Version.fromString("25:2")),
        LessThanTestCase.create(Version.fromString("1:2:123"), Version.fromString("1:12:3")),
        LessThanTestCase.create(Version.fromString("1.2-3-5"), Version.fromString("1.2-5")),
        LessThanTestCase.create(Version.fromString("5.005"), Version.fromString("5.10.0")),
        LessThanTestCase.create(Version.fromString("3.10.2"), Version.fromString("3a9.8")),
        LessThanTestCase.create(Version.fromString("3~10"), Version.fromString("3a9.8")),
        LessThanTestCase.create(
            Version.fromString("1.4+OOo3.0.0~"), Version.fromString("1.4+OOo3.0.0-4")),
        LessThanTestCase.create(Version.fromString("3.0~rc1-1"), Version.fromString("3.0-1")),
        LessThanTestCase.create(Version.fromString("2.4.7-1"), Version.fromString("2.4.7-z")),
        LessThanTestCase.create(Version.fromString("1.00"), Version.fromString("1.002-1+b2")),
        LessThanTestCase.create(Version.fromString("5.36-r0"), Version.fromString("5.36")),
        LessThanTestCase.create(Version.fromString("5.36-r0"), Version.fromString("5.36-gg1.0")),
        LessThanTestCase.create(
            Version.fromString("5.36-r0"), Version.fromString("5.36-r0-gg1.0")));
  }

  @Theory
  public void compareTo_lessThanTestCase_hasCorrectSymmetryResult(
      @FromDataPoints("LessThan") LessThanTestCase<Version> testCase) {
    assertThat(testCase.smaller()).isLessThan(testCase.larger());
    assertThat(testCase.larger()).isGreaterThan(testCase.smaller());
  }
}
