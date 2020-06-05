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
package com.google.tsunami.main.cli;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.beust.jcommander.ParameterException;
import com.google.cloud.storage.Storage;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.tsunami.common.io.archiving.testing.FakeGoogleCloudStorageArchivers;
import com.google.tsunami.common.io.archiving.testing.FakeGoogleCloudStorageArchiversModule;
import com.google.tsunami.common.io.archiving.testing.FakeRawFileArchiver;
import com.google.tsunami.common.io.archiving.testing.FakeRawFileArchiverModule;
import com.google.tsunami.main.cli.option.OutputDataFormat;
import com.google.tsunami.proto.ScanResults;
import com.google.tsunami.proto.ScanStatus;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Inject;
import javax.inject.Qualifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/** Tests for {@link ScanResultsArchiver}. */
@RunWith(JUnit4.class)
public final class ScanResultsArchiverTest {
  private static final ScanResults SCAN_RESULTS =
      ScanResults.newBuilder().setScanStatus(ScanStatus.SUCCEEDED).build();

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock Storage mockStorage;

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  private @interface SpyArchiver {}

  @Inject private FakeRawFileArchiver fakeRawFileArchiver;
  @Inject private FakeGoogleCloudStorageArchivers fakeGoogleCloudStorageArchivers;
  @Inject private ScanResultsArchiver.Options options;
  @Inject @SpyArchiver private ScanResultsArchiver scanResultsArchiver;

  @Before
  public void setUp() {
    Guice.createInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(ScanResultsArchiver.Options.class)
                    .toInstance(new ScanResultsArchiver.Options());
                install(new ScanResultsArchiverModule());
                install(new FakeRawFileArchiverModule());
                install(new FakeGoogleCloudStorageArchiversModule());
              }

              // TODO(b/145315535): wrap GCS API into a client library to get rid of this spy.
              @Provides
              @SpyArchiver
              ScanResultsArchiver getScanResultsArchiverSpy(ScanResultsArchiver delegate) {
                return spy(delegate);
              }
            })
        .injectMembers(this);
  }

  @Test
  public void optionsValidate_whenInvalidGcsUrl_throwsParameterException() {
    options.gcsOutputFileUrl = "invalid_url";
    assertThrows(ParameterException.class, options::validate);
  }

  @Test
  public void archive_withNoStorageEnabled_storesNothing() throws InvalidProtocolBufferException {
    options.localOutputFilename = "";
    options.gcsOutputFileUrl = "";

    scanResultsArchiver.archive(SCAN_RESULTS);

    fakeRawFileArchiver.assertNoDataStored();
    fakeGoogleCloudStorageArchivers.assertNoDataStored();
  }

  @Test
  public void archive_withLocalFileEnabledForJsonOutput_storesStringDataLocally()
      throws InvalidProtocolBufferException {
    options.localOutputFilename = "/tmp/result.json";
    options.localOutputFormat = OutputDataFormat.JSON;
    options.gcsOutputFileUrl = "";

    scanResultsArchiver.archive(SCAN_RESULTS);

    assertThat(
            parseJsonScanResults(
                fakeRawFileArchiver.getStoredCharSequence("/tmp/result.json").toString()))
        .isEqualTo(SCAN_RESULTS);
    fakeGoogleCloudStorageArchivers.assertNoDataStored();
  }

  @Test
  public void archive_withLocalFileEnabledForBinProtoOutput_storesBytesDataLocally()
      throws InvalidProtocolBufferException {
    options.localOutputFilename = "/tmp/test.binproto";
    options.localOutputFormat = OutputDataFormat.BIN_PROTO;
    options.gcsOutputFileUrl = "";

    scanResultsArchiver.archive(SCAN_RESULTS);

    assertThat(
            ScanResults.parseFrom(
                fakeRawFileArchiver.getStoredByteArrays("/tmp/test.binproto")))
        .isEqualTo(SCAN_RESULTS);
    fakeGoogleCloudStorageArchivers.assertNoDataStored();
  }

  @Test
  public void archive_withGcsEnabledForJsonOutput_uploadsStringDataToGcs()
      throws InvalidProtocolBufferException {
    options.localOutputFilename = "";
    options.gcsOutputFileUrl = "gs://test/object/result.json";
    options.gcsOutputFormat = OutputDataFormat.JSON;
    doReturn(mockStorage).when(scanResultsArchiver).getGcsStorage();

    scanResultsArchiver.archive(SCAN_RESULTS);

    assertThat(
            parseJsonScanResults(
                fakeGoogleCloudStorageArchivers
                    .getStoredCharSequence(mockStorage, "gs://test/object/result.json")
                    .toString()))
        .isEqualTo(SCAN_RESULTS);
    fakeRawFileArchiver.assertNoDataStored();
  }

  @Test
  public void archive_withGcsEnabledForBinProtoOutput_uploadsBytesDataToGcs()
      throws InvalidProtocolBufferException {
    options.localOutputFilename = "";
    options.gcsOutputFileUrl = "gs://test/object/result.binproto";
    options.gcsOutputFormat = OutputDataFormat.BIN_PROTO;
    doReturn(mockStorage).when(scanResultsArchiver).getGcsStorage();

    scanResultsArchiver.archive(SCAN_RESULTS);

    assertThat(
            ScanResults.parseFrom(
                fakeGoogleCloudStorageArchivers.getStoredByteArrays(
                    mockStorage, "gs://test/object/result.binproto")))
        .isEqualTo(SCAN_RESULTS);
    fakeRawFileArchiver.assertNoDataStored();
  }

  @Test
  public void archive_withLocalAndGcsOptionEnabled_archivesToBothLocation()
      throws InvalidProtocolBufferException {
    options.localOutputFilename = "/tmp/result.json";
    options.localOutputFormat = OutputDataFormat.JSON;
    options.gcsOutputFileUrl = "gs://test/object/result.binproto";
    options.gcsOutputFormat = OutputDataFormat.BIN_PROTO;
    doReturn(mockStorage).when(scanResultsArchiver).getGcsStorage();

    scanResultsArchiver.archive(SCAN_RESULTS);

    assertThat(
            parseJsonScanResults(
                fakeRawFileArchiver.getStoredCharSequence("/tmp/result.json").toString()))
        .isEqualTo(SCAN_RESULTS);
    assertThat(
        ScanResults.parseFrom(
            fakeGoogleCloudStorageArchivers.getStoredByteArrays(
                mockStorage, "gs://test/object/result.binproto")))
        .isEqualTo(SCAN_RESULTS);
  }

  private static ScanResults parseJsonScanResults(String jsonScanResults)
      throws InvalidProtocolBufferException {
    ScanResults.Builder scanResultsBuilder = ScanResults.newBuilder();
    JsonFormat.parser().merge(jsonScanResults, scanResultsBuilder);
    return scanResultsBuilder.build();
  }
}
