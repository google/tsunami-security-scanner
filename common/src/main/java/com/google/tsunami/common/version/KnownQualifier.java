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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Ascii;
import java.util.Arrays;

/**
 * A list of all the known text token qualifiers from our vulnerability feeds and the order here
 * represents the precedence of these identifiers.
 */
enum KnownQualifier implements Comparable<KnownQualifier> {
  ALPHA("alpha"),
  BETA("beta"),
  PRE("pre"),
  R("r"),
  RC("rc"),
  ABSENT(""),
  P("p"),
  PATCH("patch"),
  PATCHED("patched");

  private final String qualifierText;

  KnownQualifier(String qualifierText) {
    this.qualifierText = qualifierText;
  }

  String getQualifierText() {
    return this.qualifierText;
  }

  static boolean isKnownQualifier(String string) {
    checkNotNull(string);
    return Arrays.stream(KnownQualifier.values())
        .anyMatch(knownQualifier -> Ascii.equalsIgnoreCase(knownQualifier.qualifierText, string));
  }

  static KnownQualifier fromText(String string) {
    checkNotNull(string);
    return Arrays.stream(KnownQualifier.values())
        .filter(knownQualifier -> Ascii.equalsIgnoreCase(knownQualifier.qualifierText, string))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format("%s is not a valid KnownQualifier text.", string)));
  }
}
