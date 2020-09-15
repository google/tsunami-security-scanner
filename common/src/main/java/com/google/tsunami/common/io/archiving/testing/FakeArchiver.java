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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Maps;
import com.google.tsunami.common.io.archiving.Archiver;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/** An implementation of {@link Archiver} that stores data in memory for testing purposes. */
public final class FakeArchiver implements Archiver {
  private final Map<String, byte[]> archivedByteArrayData = Maps.newHashMap();
  private final Map<String, CharSequence> archivedCharSequenceData = Maps.newHashMap();
  private boolean shouldFail = false;

  @Override
  public boolean archive(String name, byte[] data) {
    if (shouldFail) {
      return false;
    }

    archivedByteArrayData.put(name, data);
    return true;
  }

  @Override
  public boolean archive(String name, CharSequence data) {
    if (shouldFail) {
      return false;
    }

    archivedCharSequenceData.put(name, data);
    return true;
  }

  public void failArchival() {
    this.shouldFail = true;
  }

  public byte[] getStoredByteArrays(String name) {
    if (!archivedByteArrayData.containsKey(name)) {
      throw new NoSuchElementException(String.format("'%s' not found in FakeArchiver", name));
    }
    return archivedByteArrayData.get(name);
  }

  public CharSequence getStoredCharSequence(String name) {
    if (!archivedCharSequenceData.containsKey(name)) {
      throw new NoSuchElementException(String.format("'%s' not found in FakeArchiver", name));
    }
    return archivedCharSequenceData.get(name);
  }

  public void assertNoByteArraysStored() {
    assertThat(archivedByteArrayData).isEmpty();
  }

  public void assertNoCharSequencesStored() {
    assertThat(archivedCharSequenceData).isEmpty();
  }

  public void assertNoDataStored() {
    assertNoByteArraysStored();
    assertNoCharSequencesStored();
  }

  public void assertByteArraysStored(Map<String, byte[]> expectedData) {
    assertThat(archivedByteArrayData).containsExactlyEntriesIn(expectedData);
  }

  public void assertByteArraysStoredForNames(Set<String> expectedNames) {
    assertThat(archivedByteArrayData.keySet()).containsExactlyElementsIn(expectedNames);
  }

  public void assertByteArraysStoredWithValues(Collection<byte[]> expectedValues) {
    assertThat(archivedByteArrayData.values()).containsExactlyElementsIn(expectedValues);
  }

  public void assertCharSequencesStored(Map<String, CharSequence> expectedData) {
    assertThat(archivedCharSequenceData).containsExactlyEntriesIn(expectedData);
  }

  public void assertCharSequencesStoredForNames(Set<String> expectedNames) {
    assertThat(archivedCharSequenceData.keySet()).containsExactlyElementsIn(expectedNames);
  }

  public void assertCharSequencesStoredWithValues(Collection<CharSequence> expectedValues) {
    assertThat(archivedCharSequenceData.values()).containsExactlyElementsIn(expectedValues);
  }
}
