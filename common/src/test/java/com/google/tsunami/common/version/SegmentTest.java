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

import com.google.common.collect.ImmutableList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/** Tests for {@link Segment}. */
@RunWith(Theories.class)
public final class SegmentTest {

  @Test
  public void fromTokenList_startsWithKnownQualifier_buildsFromInput() {
    ImmutableList<Token> tokens =
        ImmutableList.of(Token.fromKnownQualifier(KnownQualifier.ALPHA), Token.fromNumeric(1L));
    Segment segment = Segment.fromTokenList(tokens);

    assertThat(segment.tokens()).containsExactlyElementsIn(tokens);
  }

  @Test
  public void fromTokenList_noKnownQualifier_addsKnownQualifier() {
    ImmutableList<Token> tokens = ImmutableList.of(Token.fromText("abc"), Token.fromNumeric(1L));
    Segment segment = Segment.fromTokenList(tokens);

    assertThat(segment.tokens())
        .containsExactlyElementsIn(
            Stream.concat(
                    Stream.of(Token.fromKnownQualifier(KnownQualifier.ABSENT)), tokens.stream())
                .collect(Collectors.toList()));
  }

  @Test
  public void fromString_emptyString_returnsNullSegment() {
    assertThat(Segment.fromString("")).isEqualTo(Segment.NULL);
  }

  @Test
  public void fromString_allExcludedTokens_returnsNullSegment() {
    assertThat(Segment.fromString("gg.N/A")).isEqualTo(Segment.NULL);
  }

  @Test
  public void fromString_allEmptyTokens_returnsNullSegment() {
    assertThat(Segment.fromString("...")).isEqualTo(Segment.NULL);
  }

  @Test
  public void fromString_textAndNumeric_returnsSeparatedTextAndNumber() {
    assertThat(Segment.fromString("abc1.0").tokens())
        .containsExactly(
            Token.fromKnownQualifier(KnownQualifier.ABSENT),
            Token.fromText("abc"),
            Token.fromNumeric(1L),
            Token.fromNumeric(0L));

    assertThat(Segment.fromString("gg1.0").tokens())
        .containsExactly(
            Token.fromKnownQualifier(KnownQualifier.ABSENT),
            Token.fromNumeric(1L),
            Token.fromNumeric(0L));
  }

  @Test
  public void fromString_noKnownQualifier_addsKnownQualifier() {
    assertThat(Segment.fromString("2.1.1").tokens())
        .containsExactly(
            Token.fromKnownQualifier(KnownQualifier.ABSENT),
            Token.fromNumeric(2L),
            Token.fromNumeric(1L),
            Token.fromNumeric(1L));
  }

  @Test
  public void fromString_startsWithKnownQualifier_parsesNumericAndTextTokens() {
    assertThat(Segment.fromString("alpha.1~text").tokens())
        .containsExactly(
            Token.fromKnownQualifier(KnownQualifier.ALPHA),
            Token.fromNumeric(1L),
            Token.fromText("~"),
            Token.fromText("text"));
  }

  @DataPoints("Equals")
  public static ImmutableList<EqualsTestCase<Segment>> equalTestCases() {
    return ImmutableList.of(
        EqualsTestCase.create(Segment.fromString(""), Segment.fromString("")),
        EqualsTestCase.create(Segment.fromString(""), Segment.fromString("gg.N/A")),
        EqualsTestCase.create(Segment.fromString("1.1"), Segment.fromString("1.1")),
        EqualsTestCase.create(Segment.fromString("1.1-"), Segment.fromString("1.1-gg")));
  }

  @Theory
  public void compareTo_equalTestCase_returnsZero(
      @FromDataPoints("Equals") EqualsTestCase<Segment> testCase) {
    assertThat(testCase.first()).isEquivalentAccordingToCompareTo(testCase.second());
  }

  @DataPoints("LessThan")
  public static ImmutableList<LessThanTestCase<Segment>> lessThanTestCases() {
    return ImmutableList.of(
        // Null token.
        LessThanTestCase.create(Segment.fromString("2.1"), Segment.fromString("2.1.1")),
        LessThanTestCase.create(Segment.fromString("alpha"), Segment.fromString("")),

        // Numeric token.
        LessThanTestCase.create(Segment.fromString("2.1"), Segment.fromString("2.2")),
        LessThanTestCase.create(Segment.fromString("0.9"), Segment.fromString("1.0")),

        // Known qualifiers.
        LessThanTestCase.create(Segment.fromString("alpha"), Segment.fromString("beta")),
        LessThanTestCase.create(Segment.fromString("alpha.beta"), Segment.fromString("alpha")),
        LessThanTestCase.create(Segment.fromString("alpha.beta"), Segment.fromString("alpha.rc")),

        // Text token.
        LessThanTestCase.create(Segment.fromString("abc"), Segment.fromString("def")),
        LessThanTestCase.create(Segment.fromString("abc.def"), Segment.fromString("abc.ghi")),
        LessThanTestCase.create(Segment.fromString("abc"), Segment.fromString("DEF")),

        // Mixed type.
        LessThanTestCase.create(Segment.fromString("2.1.1"), Segment.fromString("2.1.abc")));
  }

  @Theory
  public void compareTo_lessThanTestCase_hasCorrectSymmetryResult(
      @FromDataPoints("LessThan") LessThanTestCase<Segment> testCase) {
    assertThat(testCase.smaller()).isLessThan(testCase.larger());
    assertThat(testCase.larger()).isGreaterThan(testCase.smaller());
  }
}
