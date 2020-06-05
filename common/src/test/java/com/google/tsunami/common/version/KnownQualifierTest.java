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
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/** Tests for {@link KnownQualifier}. */
@RunWith(Theories.class)
public final class KnownQualifierTest {

  @DataPoints("ValidKnownQualifierText")
  public static ImmutableList<String> validTextsForKnownQualifiers() {
    return Arrays.stream(KnownQualifier.values())
        .map(KnownQualifier::getQualifierText)
        .collect(ImmutableList.toImmutableList());
  }

  @Test
  public void compareTo_always_hasTheCorrectOrder() {
    // KnownQualifier should promise the following order to callers.
    assertThat(EnumSet.allOf(KnownQualifier.class))
        .containsExactly(
            KnownQualifier.ALPHA,
            KnownQualifier.BETA,
            KnownQualifier.PRE,
            KnownQualifier.R,
            KnownQualifier.RC,
            KnownQualifier.ABSENT,
            KnownQualifier.P,
            KnownQualifier.PATCH,
            KnownQualifier.PATCHED)
        .inOrder();
  }

  @Theory
  public void isKnownQualifier_validText_returnsTrue(
      @FromDataPoints("ValidKnownQualifierText") String validText) {
    assertThat(KnownQualifier.isKnownQualifier(validText)).isTrue();
  }

  @Theory
  public void isKnownQualifier_invalidText_returnsFalse() {
    assertThat(KnownQualifier.isKnownQualifier("random")).isFalse();
  }

  @Theory
  public void fromText_validText_returnsKnownQualifier(
      @FromDataPoints("ValidKnownQualifierText") String validText) {
    assertThat(KnownQualifier.fromText(validText)).isIn(EnumSet.allOf(KnownQualifier.class));
  }

  @Test
  public void fromText_invalidText_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> KnownQualifier.fromText("random"));
  }

  @Test
  public void fromText_invalidTextUsingComposedValidTokens_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> KnownQualifier.fromText("alpha-beta"));
  }
}
