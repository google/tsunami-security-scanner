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

import static com.google.common.truth.Truth.assertThat;
import static com.google.tsunami.common.io.archiving.ArchiverTestUtils.newPreFilledByteArray;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link RawFileArchiver}. */
@RunWith(JUnit4.class)
public final class RawFileArchiverTest {
  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void archive_whenValidTargetFileAndByteArrayData_archivesGivenDataWithGivenName()
      throws IOException {
    File tempFile = temporaryFolder.newFile();
    byte[] data = newPreFilledByteArray(200);

    RawFileArchiver rawFileArchiver = new RawFileArchiver();

    assertThat(rawFileArchiver.archive(tempFile.getAbsolutePath(), data)).isTrue();
    assertThat(Files.toByteArray(tempFile)).isEqualTo(data);
  }

  @Test
  public void archive_whenInvalidTargetFileAndByteArrayData_returnsFalse() throws IOException {
    File tempFile = temporaryFolder.newFile();
    byte[] data = newPreFilledByteArray(200);

    RawFileArchiver rawFileArchiver = new RawFileArchiver();

    assertThat(rawFileArchiver.archive(tempFile.getParent(), data)).isFalse();
    assertThat(tempFile.length()).isEqualTo(0);
  }

  @Test
  public void archive_whenValidTargetFileAndCharSequenceData_archivesGivenDataWithGivenName()
      throws IOException {
    File tempFile = temporaryFolder.newFile();
    String data = "file data";

    RawFileArchiver rawFileArchiver = new RawFileArchiver();

    assertThat(rawFileArchiver.archive(tempFile.getAbsolutePath(), data)).isTrue();
    assertThat(Files.asCharSource(tempFile, UTF_8).read()).isEqualTo(data);
  }

  @Test
  public void archive_whenInvalidTargetFileAndCharSequenceData_returnsFalse() throws IOException {
    File tempFile = temporaryFolder.newFile();
    String data = "file data";

    RawFileArchiver rawFileArchiver = new RawFileArchiver();

    assertThat(rawFileArchiver.archive(tempFile.getParent(), data)).isFalse();
    assertThat(tempFile.length()).isEqualTo(0);
  }
}
