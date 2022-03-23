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

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;

/**
 * Thrown whenever a {@link com.google.tsunami.proto.PayloadGeneratorConfig} results in a
 * combination that does not have a payload.
 *
 * <p> To reduce the burden on callers, this is an unchecked exception. The goal is simply to
 * notify the developer that the payload generator cannot be used in the requested context. If the
 * generator <em>does</em> work in the requested context, this exception would never be thrown.
 */
public final class NotImplementedException extends RuntimeException {

  @FormatMethod
  public NotImplementedException(@FormatString String format, Object... args) {
    super(String.format(format, args));
  }
}
