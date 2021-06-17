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

import com.google.cloud.storage.Storage;
import com.google.tsunami.common.io.archiving.GoogleCloudStorageArchiver;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/** A collection of fake {@link GoogleCloudStorageArchiver} created by {@link FakeFactory}. */
public final class FakeGoogleCloudStorageArchivers {
  private final Map<Storage, FakeArchiver> delegatedArchivers = new HashMap<>();

  public void assertNoDataStored() {
    for (FakeArchiver delegate : delegatedArchivers.values()) {
      delegate.assertNoDataStored();
    }
  }

  /**
   * Get the byte array data stored in {@code storage} at {@code gcsUrl}.
   *
   * @param storage the instance of the GCS storage.
   * @param gcsUrl the URL to the GCS storage object.
   * @return the content of the GCS storage object in byte array format.
   */
  public byte[] getStoredByteArrays(Storage storage, String gcsUrl) {
    if (!delegatedArchivers.containsKey(storage)) {
      throw new NoSuchElementException(String.format("Storage '%s' not found", storage));
    }
    return delegatedArchivers.get(storage).getStoredByteArrays(gcsUrl);
  }

  /**
   * Get the {@link CharSequence} data stored in {@code storage} at {@code gcsUrl}.
   *
   * @param storage the instance of the GCS storage.
   * @param gcsUrl the URL to the GCS storage object.
   * @return the content of the GCS storage object in {@link CharSequence} format.
   */
  public CharSequence getStoredCharSequence(Storage storage, String gcsUrl) {
    if (!delegatedArchivers.containsKey(storage)) {
      throw new NoSuchElementException(String.format("Storage '%s' not found", storage));
    }
    return delegatedArchivers.get(storage).getStoredCharSequence(gcsUrl);
  }

  final class FakeGoogleCloudStorageArchiver extends GoogleCloudStorageArchiver {
    private final Storage storage;

    private FakeGoogleCloudStorageArchiver(Storage storage) {
      super(new Options(), storage);
      this.storage = storage;
    }

    @Override
    public boolean archive(String gcsUrl, byte[] data) {
      FakeArchiver fakeArchiver =
          delegatedArchivers.computeIfAbsent(storage, unused -> new FakeArchiver());
      return fakeArchiver.archive(gcsUrl, data);
    }

    @Override
    public boolean archive(String gcsUrl, CharSequence data) {
      FakeArchiver fakeArchiver =
          delegatedArchivers.computeIfAbsent(storage, unused -> new FakeArchiver());
      return fakeArchiver.archive(gcsUrl, data);
    }
  }

  final class FakeFactory implements GoogleCloudStorageArchiver.Factory {

    @Override
    public GoogleCloudStorageArchiver create(Storage storage) {
      return new FakeGoogleCloudStorageArchiver(storage);
    }
  }
}
