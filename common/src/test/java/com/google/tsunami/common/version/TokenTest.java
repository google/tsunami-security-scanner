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
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/** Tests for {@link Token}. */
@RunWith(Theories.class)
public final class TokenTest {

  @Test
  public void fromNumeric_always_returnsNumericToken() {
    Token numericToken = Token.fromNumeric(123L);
    assertThat(numericToken.isNumeric()).isTrue();
    assertThat(numericToken.getNumeric()).isEqualTo(123L);
  }

  @Test
  public void fromText_always_returnsTextToken() {
    Token textToken = Token.fromText("abc");
    assertThat(textToken.isText()).isTrue();
    assertThat(textToken.isKnownQualifier()).isFalse();
    assertThat(textToken.getText()).isEqualTo("abc");
  }

  @Test
  public void fromKnownQualifier_always_returnsTextToken() {
    Token textToken = Token.fromKnownQualifier(KnownQualifier.ALPHA);
    assertThat(textToken.isText()).isTrue();
    assertThat(textToken.isKnownQualifier()).isTrue();
    assertThat(textToken.getText()).isEqualTo(KnownQualifier.ALPHA.getQualifierText());
  }

  @DataPoints("Equal")
  public static ImmutableList<EqualsTestCase<Token>> equalTestCases() {
    return ImmutableList.of(
        EqualsTestCase.create(Token.EMPTY, Token.fromKnownQualifier(KnownQualifier.ABSENT)),
        EqualsTestCase.create(Token.EMPTY, Token.fromText("")),
        EqualsTestCase.create(Token.fromNumeric(1L), Token.fromNumeric(1L)),
        EqualsTestCase.create(
            Token.fromKnownQualifier(KnownQualifier.P), Token.fromKnownQualifier(KnownQualifier.P)),
        EqualsTestCase.create(Token.fromKnownQualifier(KnownQualifier.P), Token.fromText("p")),
        EqualsTestCase.create(Token.fromKnownQualifier(KnownQualifier.P), Token.fromText("P")),
        EqualsTestCase.create(Token.fromText("abc"), Token.fromText("ABC")));
  }

  @Theory
  public void isEqualTo_equalTestCase_returnsZero(
      @FromDataPoints("Equal") EqualsTestCase<Token> testCase) {
    assertThat(testCase.first()).isEquivalentAccordingToCompareTo(testCase.second());
  }

  @DataPoints("LessThan")
  public static ImmutableList<LessThanTestCase<Token>> lessThanTestCases() {
    return ImmutableList.of(
        // Empty token and Numeric token test cases.
        LessThanTestCase.create(Token.EMPTY, Token.fromNumeric(1L)),
        LessThanTestCase.create(Token.EMPTY, Token.fromNumeric(0L)),

        // Empty token and KnownQualifier test cases.
        LessThanTestCase.create(Token.fromKnownQualifier(KnownQualifier.ALPHA), Token.EMPTY),
        LessThanTestCase.create(Token.fromKnownQualifier(KnownQualifier.RC), Token.EMPTY),
        LessThanTestCase.create(Token.EMPTY, Token.fromKnownQualifier(KnownQualifier.P)),
        LessThanTestCase.create(Token.EMPTY, Token.fromKnownQualifier(KnownQualifier.PATCH)),

        // Empty token and Text token test case.
        LessThanTestCase.create(Token.EMPTY, Token.fromText("abc")),
        LessThanTestCase.create(Token.EMPTY, Token.fromText("123")),

        // Numeric token only test case.
        LessThanTestCase.create(Token.fromNumeric(0L), Token.fromNumeric(1L)),

        // KnownQualifier only test case.
        LessThanTestCase.create(
            Token.fromKnownQualifier(KnownQualifier.ALPHA),
            Token.fromKnownQualifier(KnownQualifier.ABSENT)),
        LessThanTestCase.create(
            Token.fromKnownQualifier(KnownQualifier.ABSENT),
            Token.fromKnownQualifier(KnownQualifier.PATCH)),

        // Text only test case.
        LessThanTestCase.create(Token.fromText("abc"), Token.fromText("def")),
        LessThanTestCase.create(Token.fromText("abc"), Token.fromText("dEf")),
        LessThanTestCase.create(
            Token.fromText("abc"), Token.fromKnownQualifier(KnownQualifier.ALPHA)),

        // Numeric and Text test case
        LessThanTestCase.create(Token.fromNumeric(1L), Token.fromText("abc")),
        LessThanTestCase.create(
            Token.fromNumeric(1L), Token.fromKnownQualifier(KnownQualifier.ALPHA)));
  }

  @Theory
  public void compareTo_lessThanTestCase_hasCorrectSymmetryResult(
      @FromDataPoints("LessThan") LessThanTestCase<Token> testCase) {
    // Test symmetry.
    assertThat(testCase.smaller()).isLessThan(testCase.larger());
    assertThat(testCase.larger()).isGreaterThan(testCase.smaller());
  }
}
