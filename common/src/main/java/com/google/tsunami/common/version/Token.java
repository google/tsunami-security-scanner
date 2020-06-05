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

import com.google.auto.value.AutoOneOf;
import com.google.common.base.Ascii;
import com.google.errorprone.annotations.Immutable;

/**
 * Represents a token from a version string.
 *
 * <p>A token is the smallest meaningful piece of a version string. For example, there are 3 tokens
 * in version 2.1.1: [token(2), token(1), token(1)].
 */
@Immutable
@AutoOneOf(Token.Kind.class)
abstract class Token implements Comparable<Token> {
  static final Token EMPTY = Token.fromKnownQualifier(KnownQualifier.ABSENT);

  /** All types of a token, required by the AutoOneOf annotation. */
  public enum Kind {
    NUMERIC,
    TEXT
  }

  abstract Kind getKind();

  abstract long getNumeric();
  static Token fromNumeric(long numeric) {
    return AutoOneOf_Token.numeric(numeric);
  }
  boolean isNumeric() {
    return getKind().equals(Kind.NUMERIC);
  }

  abstract String getText();
  static Token fromText(String string) {
    return AutoOneOf_Token.text(Ascii.toLowerCase(string));
  }
  boolean isText() {
    return getKind().equals(Kind.TEXT);
  }

  static Token fromKnownQualifier(KnownQualifier knownQualifier) {
    return AutoOneOf_Token.text(knownQualifier.getQualifierText());
  }
  boolean isKnownQualifier() {
    return isText() && KnownQualifier.isKnownQualifier(getText());
  }
  boolean isEmptyToken() {
    return isText() && getText().isEmpty();
  }

  @Override
  public int compareTo(Token other) {
    // Empty tokens are always the same.
    if (this.isEmptyToken() && other.isEmptyToken()) {
      return 0;
    }

    /*
     * If the tokens under comparison are one Empty token and one Numeric token, then Empty token
     * should always be less than Numeric token, e.g. version 2.1 is less than 2.1.1 (i.e.
     * 2.1.empty < 2.1.1, empty < 1).
     */
    if (this.isEmptyToken() && other.isNumeric()) {
      return -1;
    }
    if (this.isNumeric() && other.isEmptyToken()) {
      return 1;
    }

    /*
     * For Numeric and Text tokens, we follow the specification from http://semver.org#spec-item-11:
     * 1. Numeric tokens are compared numerically.
     * 2. Text tokens are compared lexically, case insensitively.
     * 3. Numeric identifiers always have lower precedence than non-numeric identifiers.
     *
     * For all the known qualifiers, we apply the comparison rules defined by KnownQualifier.
     */
    if (this.isNumeric() && other.isNumeric()) {
      return Long.compare(this.getNumeric(), other.getNumeric());
    }

    if (this.isKnownQualifier() && other.isKnownQualifier()) {
      return KnownQualifier.fromText(this.getText())
          .compareTo(KnownQualifier.fromText(other.getText()));
    }

    if (this.isText() && other.isText()) {
      return this.getText().compareToIgnoreCase(other.getText());
    }

    // Cross type comparison, Numeric token is always less than Text tokens.
    return this.getKind().compareTo(other.getKind());
  }
}
