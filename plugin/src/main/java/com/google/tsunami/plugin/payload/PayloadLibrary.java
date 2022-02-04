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

import com.google.protobuf.ByteString;
import com.google.tsunami.plugin.TcsClient;
import com.google.tsunami.proto.PayloadAttributes;
import java.util.Optional;

/**
 * Static class that holds all the {@link BasePayload}s. All payloads returned by {@link
 * PayloadGenerator} should come from here.
 */
final class PayloadLibrary {
  private static final int SECRET_LENGTH = 8;
  private static final String STRING_CONCAT_PREFIX = "TSUNAMI_PAYLOAD_START";
  private static final String STRING_CONCAT_POSTFIX = "TSUNAMI_PAYLOAD_END";

  /** Private constructor to prevent users from instantiating this class */
  private PayloadLibrary() {}

  /**
   * Interpretation Environment: Linux | Execution Environment: Linux | Uses callback server: True
   */
  static BasePayload getLinuxExecutionCallbackServerPayload(
      PayloadSecretGenerator secretGenerator, TcsClient c) {
    String secret = secretGenerator.generate(SECRET_LENGTH);
    return BasePayload.create(
        String.format("curl %s", c.getCallbackUri(secret)),
        (Validator) (unused) -> c.hasOobLog(secret),
        PayloadAttributes.newBuilder().setUsesCallbackServer(true).build());
  }

  /**
   * Interpretation Environment: Linux | Execution Environment: Linux | Uses callback server: False
   */
  static BasePayload getLinuxExecutionPrintfPayload(PayloadSecretGenerator secretGenerator) {
    String secret = secretGenerator.generate(SECRET_LENGTH);

    return BasePayload.create(
        String.format(
            "printf %s %s %s %s",
            "%s%s%s", STRING_CONCAT_PREFIX, secret, STRING_CONCAT_POSTFIX),
        (Validator)
            (Optional<ByteString> input) ->
                input
                    .map(
                        i ->
                            i.toStringUtf8()
                                .contains(
                                    STRING_CONCAT_PREFIX
                                        + secret
                                        + STRING_CONCAT_POSTFIX))
                    .orElse(false),
        PayloadAttributes.newBuilder().setUsesCallbackServer(false).build());
  }
}
