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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The segment of a version number, separated by the semantic separators from the original version
 * string.
 *
 * <p>For example, there are 2 segments in version string "2.1.1-alpha.1": "2.1.1" and "alpha.1".
 *
 * <p>The first element of a Segment should always be a {@link KnownQualifier} token. If no {@link
 * KnownQualifier} is found in the segment, an ABSENT qualifier is added.
 */
@AutoValue
@Immutable
abstract class Segment implements Comparable<Segment> {
  static final Segment NULL = Segment.fromTokenList(ImmutableList.of(Token.EMPTY));

  private static final ImmutableSet<String> TOKENIZER_DELIMITERS =
      ImmutableSet.of("\\.", "\\+", "-", ":", "_", "~");
  private static final Pattern TOKENIZER_SPLIT_REGEX =
      Pattern.compile(
          // Additional split on boundaries between number and text.
          "(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)|"
              // We keep the delimiter for comparison.
              + TOKENIZER_DELIMITERS.stream()
                  .map(delimiter -> String.format("((?<=%1$s)|(?=%1$s))", delimiter))
                  .collect(Collectors.joining("|")));
  private static final ImmutableSet<String> EXCLUDED_TOKENS = ImmutableSet.of(".", "gg", "N/A");

  /** All the tokens within this segment. */
  abstract ImmutableList<Token> tokens();

  static Segment fromTokenList(ImmutableList<Token> tokens) {
    ImmutableList.Builder<Token> finalTokensBuilder = new ImmutableList.Builder<>();

    // Make sure Segment always starts with a KnownQualifier. See http://b/135912609 for details.
    if (tokens.isEmpty() || !tokens.get(0).isKnownQualifier()) {
      finalTokensBuilder.add(Token.fromKnownQualifier(KnownQualifier.ABSENT));
    }
    finalTokensBuilder.addAll(tokens);

    return new AutoValue_Segment(finalTokensBuilder.build());
  }

  static Segment fromString(String segmentString) {
    return parseFromString(segmentString);
  }

  private static Segment parseFromString(String segmentString) {
    ImmutableList<String> rawTokens =
        Arrays.stream(TOKENIZER_SPLIT_REGEX.split(segmentString))
            .filter(token -> !token.isEmpty())
            .filter(token -> !EXCLUDED_TOKENS.contains(token))
            .collect(ImmutableList.toImmutableList());

    if (rawTokens.isEmpty()) {
      return Segment.NULL;
    }

    ImmutableList.Builder<Token> tokensBuilder = new ImmutableList.Builder<>();

    // Make sure Segment always starts with a KnownQualifier. See http://b/135912609 for details.
    if (!KnownQualifier.isKnownQualifier(rawTokens.get(0))) {
      tokensBuilder.add(Token.fromKnownQualifier(KnownQualifier.ABSENT));
    }

    // Parses token.
    for (String rawToken : rawTokens) {
      try {
        long numericToken = Long.parseLong(rawToken);
        tokensBuilder.add(Token.fromNumeric(numericToken));
      } catch (NumberFormatException e) {
        tokensBuilder.add(Token.fromText(rawToken));
      }
    }

    return Segment.fromTokenList(tokensBuilder.build());
  }

  @Override
  public int compareTo(Segment other) {
    return ComparisonUtility.compareListWithFillValue(this.tokens(), other.tokens(), Token.EMPTY);
  }
}
