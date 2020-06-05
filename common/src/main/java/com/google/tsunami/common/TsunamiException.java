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
package com.google.tsunami.common;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;

/** Base exception definition of all Tsunami execution errors. */
public class TsunamiException extends RuntimeException {
  private final ErrorCode errorCode;

  public TsunamiException() {
    this(ErrorCode.UNKNOWN);
  }

  public TsunamiException(ErrorCode errorCode) {
    this(errorCode, null);
  }

  public TsunamiException(ErrorCode errorCode, String message) {
    this(errorCode, message, null);
  }

  public TsunamiException(ErrorCode errorCode, String message, Throwable cause) {
    super(buildExceptionMessage(checkNotNull(errorCode), message), cause);
    this.errorCode = errorCode;
  }

  private static String buildExceptionMessage(ErrorCode errorCode, String message) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    exceptionMessageBuilder.append("(Tsunami error ").append(errorCode).append(")");
    if (!Strings.isNullOrEmpty(message)) {
      exceptionMessageBuilder.append(": ").append(message);
    }
    return exceptionMessageBuilder.toString();
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
