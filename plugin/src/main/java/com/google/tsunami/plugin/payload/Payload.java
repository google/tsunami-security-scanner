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
import com.google.tsunami.plugin.TcsClient;
import com.google.tsunami.proto.PayloadAttributes;
import com.google.tsunami.proto.PayloadGeneratorConfig;
import java.util.Optional;

/**
 * Skeletal implementation of a payload. Each exploit type should have its own payload type.
 * PayloadGenerator should only return subclasss of this class.
 */
public abstract class Payload {

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private final PayloadGeneratorConfig config;
  private final TcsClient callbackServerClient;
  private final PayloadSecretGenerator secretGenerator;
  private BasePayload payload;

  /** Each payload subclass should receive the config and the callback server client */
  public Payload(
      PayloadGeneratorConfig config,
      TcsClient callbackServerClient,
      PayloadSecretGenerator secretGenerator) {
    this.config = config;
    this.callbackServerClient = callbackServerClient;
    this.secretGenerator = secretGenerator;
  }

  /**
   * Initializes the payload, selecting the actual payload command. This is only meant to be called
   * by {@link PayloadGenerator}.
   */
  abstract void initialize() throws NotImplementedException;

  /** Returns the actual payload command string */
  public final String getPayload() {
    logger.atInfo().log(
        "%s generated payload `%s`, %s use the callback server",
        this.config,
        this.payload.getPayloadString(),
        this.payload.getPayloadAttributes().getUsesCallbackServer() ? "does" : "does not");
    return this.payload.getPayloadString();
  }

  /**
   * Checks if the supplied payload was executed based on an input string. Some payloads may not
   * need an input for validation e.g. if it uses the callback server.
   */
  public final boolean checkIfExecuted(Optional<ByteString> input)
      throws NoCallbackServerException {
    boolean result = this.payload.getValidator().isExecuted(input);
    logger.atInfo().log("Input: %s, output: %s", input, result);
    return result;
  }

  /** Returns additional information about the paylaod to the caller. */
  public final PayloadAttributes getPayloadAttributes() {
    return this.payload.getPayloadAttributes();
  }

  /** Internal API for getting the configuration originally passsed to {@link PayloadGenerator}. */
  protected final PayloadGeneratorConfig getConfig() {
    return this.config;
  }

  /** Internal API for interacting with the callback server. */
  protected final TcsClient getCallbackServerClient() {
    return this.callbackServerClient;
  }

  /** Internal API for interacting with the RNG. */
  protected final PayloadSecretGenerator getSecretGenerator() {
    return this.secretGenerator;
  }

  /** Internal API for setting the BasePayload. */
  protected final void setBasePayload(BasePayload p) {
    this.payload = p;
  }
}
