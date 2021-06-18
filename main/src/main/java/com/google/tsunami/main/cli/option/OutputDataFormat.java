/*
 * Copyright 2020 Google LLC
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
package com.google.tsunami.main.cli.option;

import com.google.common.base.Ascii;
import java.util.Optional;

/** Output format of Tsunami's scanning results. */
public enum OutputDataFormat {
  BIN_PROTO,
  JSON;

  /**
   * Parses the given {@code value} into {@link OutputDataFormat} enum.
   *
   * @param value the string representation of the {@link OutputDataFormat} enum.
   * @return the parsed {@link OutputDataFormat} enum.
   */
  public static Optional<OutputDataFormat> parse(String value) {
    for (OutputDataFormat outputDataFormat : OutputDataFormat.values()) {
      if (Ascii.equalsIgnoreCase(outputDataFormat.name(), value)) {
        return Optional.of(outputDataFormat);
      }
    }

    return Optional.empty();
  }
}
