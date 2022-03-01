/*
 * Copyright 2022 Google LLC
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
package com.google.tsunami.plugin.payload;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.tsunami.plugin.TcsClient;
import com.google.tsunami.proto.PayloadAttributes;
import com.google.tsunami.proto.PayloadDefinition;
import com.google.tsunami.proto.PayloadGeneratorConfig;
import java.lang.annotation.Retention;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Qualifier;

/** Holds the generate function to get a detection payload given config parameters */
public final class PayloadGenerator {

  private static final int SECRET_LENGTH = 8;

  private static final String TOKEN_CALLBACK_SERVER_URL = "$TSUNAMI_PAYLOAD_TOKEN_URL";
  private static final String TOKEN_RANDOM_STRING = "$TSUNAMI_PAYLOAD_TOKEN_RANDOM";

  private final TcsClient tcsClient;

  private final PayloadSecretGenerator secretGenerator;

  private final ImmutableList<PayloadDefinition> payloads;

  @Inject
  PayloadGenerator(
      TcsClient tcsClient,
      PayloadSecretGenerator secretGenerator,
      @Payloads ImmutableList<PayloadDefinition> payloads) {
    this.tcsClient = checkNotNull(tcsClient);
    this.secretGenerator = checkNotNull(secretGenerator);
    this.payloads = checkNotNull(payloads);
  }

  public boolean isCallbackServerEnabled() {
    return tcsClient.isCallbackServerEnabled();
  }

  public Payload generate(PayloadGeneratorConfig config) {
    PayloadDefinition p = null;

    // If a payload that uses callback server is requested, prioritize finding
    // one. If there's none, fallback to any payload that matches.
    if (tcsClient.isCallbackServerEnabled() && config.getUseCallbackServer()) {
      for (PayloadDefinition candidate : payloads) {
        if (isMatchingPayload(candidate, config)
            && candidate.getUsesCallbackServer().getValue()) {
          p = candidate;
          break;
        }
      }
    }

    if (p == null) {
      for (PayloadDefinition candidate : payloads) {
        if (isMatchingPayload(candidate, config)
            && !candidate.getUsesCallbackServer().getValue()) {
          p = candidate;
          break;
        }
      }
    }

    if (p == null) {
      throw new NotImplementedException(
          "No payload implemented for %s vulnerability type, %s interpretation environment, %s"
              + " execution environment",
          config.getVulnerabilityType(),
          config.getInterpretationEnvironment(),
          config.getExecutionEnvironment());
    }

    return convertParsedPayload(p, config);
  }

  private boolean isMatchingPayload(PayloadDefinition p, PayloadGeneratorConfig c) {
    return p.getVulnerabilityTypeList().contains(c.getVulnerabilityType())
        && p.getInterpretationEnvironment() == c.getInterpretationEnvironment()
        && p.getExecutionEnvironment() == c.getExecutionEnvironment();
  }

  private Payload convertParsedPayload(PayloadDefinition p, PayloadGeneratorConfig c) {
    String secret = secretGenerator.generate(SECRET_LENGTH);
    if (p.getUsesCallbackServer().getValue()) {
      return new Payload(
          p.getPayloadString()
              .getValue()
              .replace(TOKEN_CALLBACK_SERVER_URL, tcsClient.getCallbackUri(secret)),
          (Validator) (unused) -> tcsClient.hasOobLog(secret),
          PayloadAttributes.newBuilder().setUsesCallbackServer(true).build(),
          c);
    } else {
      String payloadString = p.getPayloadString().getValue().replace(TOKEN_RANDOM_STRING, secret);
      Validator v;
      switch (p.getValidationType()) {
        case VALIDATION_REGEX:
          String processedRegex =
              p.getValidationRegex().getValue().replace(TOKEN_RANDOM_STRING, secret);
          v =
              (Validator)
                  (Optional<ByteString> input) ->
                      input.map(i -> i.toStringUtf8().matches(processedRegex)).orElse(false);
          return new Payload(
              payloadString,
              v,
              PayloadAttributes.newBuilder().setUsesCallbackServer(false).build(),
              c);
        default:
          throw new NotImplementedException(
              "Validation type %s not implemented.", p.getValidationType());
      }
    }
  }

  /** Guice interface for injecting parsed payloads from payload_definitions.yaml */
  @Qualifier
  @Retention(RUNTIME)
  public @interface Payloads {}
}
