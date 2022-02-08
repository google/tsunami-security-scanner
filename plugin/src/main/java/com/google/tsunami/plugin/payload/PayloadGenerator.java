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

import com.google.tsunami.plugin.TcsClient;
import com.google.tsunami.proto.PayloadGeneratorConfig;
import javax.inject.Inject;

/** Holds the generate function to get a detection payload given config parameters */
public final class PayloadGenerator {

  private final TcsClient tcsClient;

  private final PayloadSecretGenerator secretGenerator;

  @Inject
  PayloadGenerator(TcsClient tcsClient, PayloadSecretGenerator secretGenerator) {
    this.tcsClient = checkNotNull(tcsClient);
    this.secretGenerator = checkNotNull(secretGenerator);
  }

  public Payload generate(PayloadGeneratorConfig config) throws NotImplementedException {
    Payload p;
    switch (config.getVulnerabilityType()) {
      case REFLECTIVE_RCE:
        p = new ReflectiveRcePayload(config, this.tcsClient, this.secretGenerator);
        break;
      default:
        throw new NotImplementedException(
            "Payload selection for %s is not implemented", config.getVulnerabilityType());
    }

    p.initialize();
    return p;
  }
}
