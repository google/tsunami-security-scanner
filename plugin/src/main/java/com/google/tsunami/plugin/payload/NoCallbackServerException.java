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

/**
 * Thrown if {@link PayloadFrameworkConfigs#throwErrorIfCallbackServerUnconfigured} is true and
 * {@link PayloadGenerator} is asked to return a payload that uses the callback server but either
 * the Tsunami instance does not have the callback server configured OR no callback-enabled payload
 * exists for the requested payload configuration.
 *
 * <p>To reduce the burden on callers, this is an unchecked exception.
 */
public final class NoCallbackServerException extends RuntimeException {

  public NoCallbackServerException() {
    super(
        "Received a request for a payload that uses the callback server, but no callback server is"
            + " configured. To have the payload generator attempt to find a fallback payload that"
            + " doesn't use the callback server, set"
            + " PayloadFrameworkConfigs.throwErrorIfCallbackServerUnconfigured to false.");
  }
}
