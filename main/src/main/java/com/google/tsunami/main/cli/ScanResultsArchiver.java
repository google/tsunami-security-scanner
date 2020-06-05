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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.tsunami.common.io.archiving.GoogleCloudStorageArchiver.GS_URL_PATTERN;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.tsunami.common.cli.CliOption;
import com.google.tsunami.common.io.archiving.Archiver;
import com.google.tsunami.common.io.archiving.GoogleCloudStorageArchiver;
import com.google.tsunami.common.io.archiving.RawFileArchiver;
import com.google.tsunami.main.cli.option.OutputDataFormat;
import com.google.tsunami.proto.ScanResults;
import javax.inject.Inject;

class ScanResultsArchiver {

  @Parameters(separators = "=")
  static final class Options implements CliOption {

    @Parameter(
        names = "--scan-results-local-output-filename",
        description = "The local output filename of the scanning results.")
    public String localOutputFilename;

    @Parameter(
        names = "--scan-results-local-output-format",
        description = "The format of the scanning results saved as local file.")
    public OutputDataFormat localOutputFormat;

    @Parameter(
        names = "--scan-results-gcs-output-file-url",
        description = "The GCS file url for the uploaded scanning results.")
    public String gcsOutputFileUrl;

    @Parameter(
        names = "--scan-results-gcs-output-format",
        description = "The format of the scanning results uploaded to GCS bucket.")
    public OutputDataFormat gcsOutputFormat;

    @Override
    public void validate() {
      if (!Strings.isNullOrEmpty(gcsOutputFileUrl)
          && !GS_URL_PATTERN.matcher(gcsOutputFileUrl).matches()) {
        throw new ParameterException(String.format("Malformed GCS URL: '%s'", gcsOutputFileUrl));
      }
    }
  }

  private final Options options;
  private final RawFileArchiver rawFileArchiver;
  private final GoogleCloudStorageArchiver.Factory googleCloudStorageArchiverFactory;

  @Inject
  // TODO(b/145315535): inject archivers using multibinder instead.
  ScanResultsArchiver(
      Options options,
      RawFileArchiver rawFileArchiver,
      GoogleCloudStorageArchiver.Factory googleCloudStorageArchiverFactory) {
    this.options = checkNotNull(options);
    this.rawFileArchiver = checkNotNull(rawFileArchiver);
    this.googleCloudStorageArchiverFactory = checkNotNull(googleCloudStorageArchiverFactory);
  }

  Storage getGcsStorage() {
    return StorageOptions.getDefaultInstance().getService();
  }

  void archive(ScanResults scanResults) throws InvalidProtocolBufferException {
    if (!Strings.isNullOrEmpty(options.localOutputFilename)) {
      archive(rawFileArchiver, options.localOutputFilename, options.localOutputFormat, scanResults);
    }

    if (!Strings.isNullOrEmpty(options.gcsOutputFileUrl)) {
      GoogleCloudStorageArchiver archiver =
          googleCloudStorageArchiverFactory.create(getGcsStorage());
      archive(archiver, options.gcsOutputFileUrl, options.gcsOutputFormat, scanResults);
    }
  }

  private static void archive(
      Archiver archiver, String location, OutputDataFormat outputFormat, ScanResults scanResults)
      throws InvalidProtocolBufferException {
    switch (outputFormat) {
      case BIN_PROTO:
        archiver.archive(location, scanResults.toByteArray());
        break;
      case JSON:
        archiver.archive(location, JsonFormat.printer().print(scanResults));
        break;
    }
  }
}
