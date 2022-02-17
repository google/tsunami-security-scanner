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

  /** Returns the actual payload command string */
  public final String getPayload() {
    logger.atInfo().log(
        "%s generated payload `%s`, %s use the callback server",
        this.config, this.payload, this.attributes.getUsesCallbackServer() ? "does" : "does not");
    return this.payload;
  }

  /**
   * Checks if the supplied payload was executed based on an input string. Some payloads may not
   * need an input for validation e.g. if it uses the callback server.
   */
  public final boolean checkIfExecuted(Optional<ByteString> input)
      throws NoCallbackServerException {
    boolean result = this.validator.isExecuted(input);
    logger.atInfo().log("Input: %s, output: %s", input, result);
    return result;
  }

  /** Returns additional information about the paylaod to the caller. */
  public final PayloadAttributes getPayloadAttributes() {
    return this.attributes;
  }
}
