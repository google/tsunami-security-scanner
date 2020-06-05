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
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/** Tests for {@link GoogleCloudStorageArchiver}. */
@RunWith(JUnit4.class)
public final class GoogleCloudStorageArchiverTest {
  private static final String BUCKET_ID = "test_bucket";
  private static final String OBJECT_ID = "test/object";

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock Storage mockStorage;
  @Mock WriteChannel mockWriter;

  @Captor ArgumentCaptor<BlobInfo> blobInfoCaptor;
  @Captor ArgumentCaptor<byte[]> byteDataCaptor;
  @Captor ArgumentCaptor<ByteBuffer> byteBufferCaptor;

  @Inject private GoogleCloudStorageArchiver.Options options;
  @Inject private GoogleCloudStorageArchiver.Factory archiverFactory;

  @Before
  public void setUp() {
    Guice.createInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(GoogleCloudStorageArchiver.Options.class)
                    .toInstance(new GoogleCloudStorageArchiver.Options());
                install(new GoogleCloudStorageArchiverModule());
              }
            })
        .injectMembers(this);
  }

  @Test
  public void archive_withSmallSizeString_createsBlobInOneRequest() {
    GoogleCloudStorageArchiver archiver = archiverFactory.create(mockStorage);
    String dataToArchive = "TEST DATA";

    boolean succeeded = archiver.archive(buildGcsUrl(BUCKET_ID, OBJECT_ID), dataToArchive);

    assertThat(succeeded).isTrue();
    verify(mockStorage, times(1)).create(blobInfoCaptor.capture(), byteDataCaptor.capture());
    assertThat(blobInfoCaptor.getValue())
        .isEqualTo(BlobInfo.newBuilder(BUCKET_ID, OBJECT_ID).build());
    assertThat(byteDataCaptor.getValue()).isEqualTo(dataToArchive.getBytes(UTF_8));
  }

  @Test
  public void archive_withSmallSizeBlob_createsBlobInOneRequest() {
    GoogleCloudStorageArchiver archiver = archiverFactory.create(mockStorage);
    byte[] dataToArchive = newPreFilledByteArray(10);

    boolean succeeded = archiver.archive(buildGcsUrl(BUCKET_ID, OBJECT_ID), dataToArchive);

    assertThat(succeeded).isTrue();
    verify(mockStorage, times(1)).create(blobInfoCaptor.capture(), byteDataCaptor.capture());
    assertThat(blobInfoCaptor.getValue())
        .isEqualTo(BlobInfo.newBuilder(BUCKET_ID, OBJECT_ID).build());
    assertThat(byteDataCaptor.getValue()).isEqualTo(dataToArchive);
  }

  @Test
  public void archive_withLargeSizeString_createsBlobWithWriter() throws IOException {
    options.chunkSizeInBytes = 8;
    options.chunkUploadThresholdInBytes = 16;
    doReturn(mockWriter)
        .when(mockStorage)
        .writer(eq(BlobInfo.newBuilder(BUCKET_ID, OBJECT_ID).build()));
    GoogleCloudStorageArchiver archiver = archiverFactory.create(mockStorage);
    String dataToArchive = "THIS IS A LONG DATA";
    int numOfChunks = (int) Math.ceil((double) dataToArchive.length() / options.chunkSizeInBytes);

    boolean succeeded = archiver.archive(buildGcsUrl(BUCKET_ID, OBJECT_ID), dataToArchive);

    assertThat(succeeded).isTrue();
    verify(mockWriter, times(numOfChunks)).write(byteBufferCaptor.capture());
    assertThat(byteBufferCaptor.getAllValues())
        .containsExactly(
            ByteBuffer.wrap(dataToArchive.getBytes(UTF_8), 0, 8),
            ByteBuffer.wrap(dataToArchive.getBytes(UTF_8), 8, 8),
            ByteBuffer.wrap(dataToArchive.getBytes(UTF_8), 16, 3));
  }

  @Test
  public void archive_withLargeSizeBlob_createsBlobWithWriter() throws IOException {
    options.chunkSizeInBytes = 8;
    options.chunkUploadThresholdInBytes = 16;
    doReturn(mockWriter)
        .when(mockStorage)
        .writer(eq(BlobInfo.newBuilder(BUCKET_ID, OBJECT_ID).build()));
    GoogleCloudStorageArchiver archiver = archiverFactory.create(mockStorage);
    byte[] dataToArchive = newPreFilledByteArray(20);
    int numOfChunks = (int) Math.ceil((double) dataToArchive.length / options.chunkSizeInBytes);

    boolean succeeded = archiver.archive(buildGcsUrl(BUCKET_ID, OBJECT_ID), dataToArchive);

    assertThat(succeeded).isTrue();
    verify(mockWriter, times(numOfChunks)).write(byteBufferCaptor.capture());
    assertThat(byteBufferCaptor.getAllValues())
        .containsExactly(
            ByteBuffer.wrap(dataToArchive, 0, 8),
            ByteBuffer.wrap(dataToArchive, 8, 8),
            ByteBuffer.wrap(dataToArchive, 16, 4));
  }

  @Test
  public void archive_withLargeSizeBlobAndWriteError_returnsFalse() throws IOException {
    options.chunkSizeInBytes = 8;
    options.chunkUploadThresholdInBytes = 16;
    doReturn(mockWriter)
        .when(mockStorage)
        .writer(eq(BlobInfo.newBuilder(BUCKET_ID, OBJECT_ID).build()));
    doThrow(IOException.class).when(mockWriter).write(any());
    GoogleCloudStorageArchiver archiver = archiverFactory.create(mockStorage);
    byte[] dataToArchive = newPreFilledByteArray(20);

    assertThat(archiver.archive(buildGcsUrl(BUCKET_ID, OBJECT_ID), dataToArchive)).isFalse();
  }

  @Test
  public void archive_withInvalidGcsUrl_throwsIllegalArgumentException() {
    GoogleCloudStorageArchiver archiver = archiverFactory.create(mockStorage);

    assertThrows(IllegalArgumentException.class, () -> archiver.archive("invalid_url", ""));
  }

  private static final String buildGcsUrl(String bucketId, String objectId) {
    return String.format("gs://%s/%s", bucketId, objectId);
  }
}
