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

import com.google.auto.value.AutoValue;
import com.google.tsunami.proto.PayloadAttributes;

/** Data model wrapped by {@link Payload} class which provides more context around the payload. */
@AutoValue
public abstract class BasePayload {
  static BasePayload create(String payload, Validator validator, PayloadAttributes attributes) {
    return new AutoValue_BasePayload(payload, validator, attributes);
  }

  /** The actual command that should be injected by the detector */
  public abstract String getPayloadString();

  /** Returns a function which is used to check if the payload was executed. */
  public abstract Validator getValidator();

  /** Returns context around the payload e.g. if it uses the callback server. */
  public abstract PayloadAttributes getPayloadAttributes();
}
