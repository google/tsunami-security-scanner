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
package com.google.tsunami.common.io.archiving;

/** Utilities for all {@link Archiver} unit tests. */
final class ArchiverTestUtils {
  private ArchiverTestUtils() {}

  /** Returns a byte array of length size that has values 0 .. size - 1. */
  static byte[] newPreFilledByteArray(int size) {
    return newPreFilledByteArray(0, size);
  }

  /** Returns a byte array of length size that has values offset .. offset + size - 1. */
  static byte[] newPreFilledByteArray(int offset, int size) {
    byte[] array = new byte[size];
    for (int i = 0; i < size; i++) {
      array[i] = (byte) (offset + i);
    }
    return array;
  }
}
