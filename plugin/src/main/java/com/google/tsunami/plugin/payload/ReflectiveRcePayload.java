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

import com.google.tsunami.plugin.TcsClient;
import com.google.tsunami.proto.PayloadGeneratorConfig;

/** Returns a detection payload for reflective remote code execution vulnerabilities. */
public final class ReflectiveRcePayload extends Payload {

  ReflectiveRcePayload(
      PayloadGeneratorConfig config, TcsClient client, PayloadSecretGenerator secretGenerator) {
    super(config, client, secretGenerator);
  }

  @Override
  void initialize() throws NotImplementedException {
    BasePayload ret;

    switch (getConfig().getInterpretationEnvironment()) {
      case LINUX_SHELL:
        ret = getLinuxPayload();
        break;
      default:
        throw new NotImplementedException(
                "ReflectiveRCE does not have a payload implemented for %s interpretation"
                    + " environment",
                getConfig().getInterpretationEnvironment());
    }

    setBasePayload(ret);
  }

  private BasePayload getLinuxPayload() throws NotImplementedException {
    switch (getConfig().getExecutionEnvironment()) {
      case EXEC_INTERPRETATION_ENVIRONMENT:
        if (getCallbackServerClient().isCallbackServerEnabled()
            && getConfig().getUseCallbackServer()) {
          return PayloadLibrary.getLinuxExecutionCallbackServerPayload(
              getSecretGenerator(), getCallbackServerClient());
        }

        return PayloadLibrary.getLinuxExecutionPrintfPayload(getSecretGenerator());

      default:
        throw new NotImplementedException(
                "ReflectiveRCE does not have a payload implemented for %s execution environment"
                    + " environment",
                getConfig().getExecutionEnvironment());
    }
  }
}
