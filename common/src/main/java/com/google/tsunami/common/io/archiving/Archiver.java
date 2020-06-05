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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

/** An {@link Archiver} archives the given data to some data storage. */
public interface Archiver {

  /**
   * Archives the {@code data} associated with the given {@code name}.
   *
   * @param name the name that will be associated with the data
   * @param data the data to be archived in byte array format
   * @return whether the given data is archived successfully.
   */
  @CanIgnoreReturnValue
  boolean archive(String name, byte[] data);

  /**
   * Archives the {@code data} associated with the given {@code name}. By default, this method
   * encodes the {@link CharSequence} {@code data} into a sequence of bytes using {@code UTF_8}
   * {@link java.nio.charset.StandardCharsets} and calls the {@link #archive(String, byte[])}
   * method.
   *
   * @param name the name that will be associated with the data
   * @param data the data to be archived in {@link CharSequence} format
   * @return whether the given data is archived successfully.
   */
  @CanIgnoreReturnValue
  default boolean archive(String name, CharSequence data) {
    return archive(name, checkNotNull(data).toString().getBytes(UTF_8));
  }
}
