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
package com.google.tsunami.common.server;

import com.google.auto.value.AutoValue;

/** Command to spawn a language server and associated port. */
@AutoValue
public abstract class ServerPortCommand {
  public static ServerPortCommand create(String serverCommand, String port) {
    return new AutoValue_ServerPortCommand(serverCommand, port);
  }

  public abstract String serverCommand();

  public abstract String port();
}
