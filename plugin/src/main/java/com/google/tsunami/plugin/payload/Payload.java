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

import com.google.common.flogger.GoogleLogger;
import com.google.protobuf.ByteString;
import com.google.tsunami.proto.PayloadAttributes;
import com.google.tsunami.proto.PayloadGeneratorConfig;
import java.util.Optional;

/** Type returned by {@link PayloadGenerator} to be used in detectors. */
public class Payload {

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  private final String payload;
  private final Validator validator;
  private final PayloadAttributes attributes;
  private final PayloadGeneratorConfig config;

  public Payload(
      String payload,
      Validator validator,
      PayloadAttributes attributes,
      PayloadGeneratorConfig config) {
    this.payload = payload;
    this.validator = validator;
    this.attributes = attributes;
    this.config = config;
  }

  /**
   * Get the string representation of the payload.
   *
   * @return the actual payload string
   */
  public final String getPayload() {
    logger.atInfo().log(
        "%s generated payload `%s`, %s use the callback server",
        this.config, this.payload, this.attributes.getUsesCallbackServer() ? "does" : "does not");
    return this.payload;
  }

  /**
   * Checks if the supplied payload was executed based on a given input e.g. a reflective RCE.
   *
   * @param input - a UTF-8 encoded string
   * @return whether this payload is executed on the scan target.
   */
  public final boolean checkIfExecuted(String input) {
    return this.validator.isExecuted(Optional.of(ByteString.copyFromUtf8(input)));
  }

  /**
   * Checks if the supplied payload was executed based on a given input e.g. a reflective RCE.
   *
   * @param input - a sequence of bytes in the {@link ByteString} format.
   * @return whether this payload is executed on the scan target.
   */
  public final boolean checkIfExecuted(ByteString input) {
    return this.validator.isExecuted(Optional.of(input));
  }

  /**
   * Checks if the supplied payload was executed based on a given input e.g. a reflective RCE.
   *
   * @param input - an optional sequence of bytes in the {@link ByteString} format.
   * @return whether this payload is executed on the scan target.
   */
  public final boolean checkIfExecuted(Optional<ByteString> input) {
    return this.validator.isExecuted(input);
  }

  /**
   * Checks if the supplied payload was executed without supplying an input e.g. validation against
   * the callback server does not require input.
   *
   * @return whether this payload is executed on the scan target.
   */
  public final boolean checkIfExecuted() {
    return this.validator.isExecuted(Optional.empty());
  }

  /**
   * Get additional attributes about this payload.
   *
   * @return the {@link PayloadAttributes} about this payload
   */
  public final PayloadAttributes getPayloadAttributes() {
    return this.attributes;
  }
}
