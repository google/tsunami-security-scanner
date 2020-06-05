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
package com.google.tsunami.common.io.archiving.testing;

import com.google.tsunami.common.io.archiving.RawFileArchiver;

/** A fake implementation of the {@link RawFileArchiver}. */
public final class FakeRawFileArchiver extends RawFileArchiver {
  private final FakeArchiver delegate = new FakeArchiver();

  @Override
  public boolean archive(String fileName, byte[] data) {
    return delegate.archive(fileName, data);
  }

  @Override
  public boolean archive(String fileName, CharSequence data) {
    return delegate.archive(fileName, data);
  }

  public byte[] getStoredByteArrays(String fileName) {
    return delegate.getStoredByteArrays(fileName);
  }

  public CharSequence getStoredCharSequence(String fileName) {
    return delegate.getStoredCharSequence(fileName);
  }

  public void assertNoDataStored() {
    delegate.assertNoDataStored();
  }
}
