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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.validators.PositiveInteger;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.common.flogger.GoogleLogger;
import com.google.inject.assistedinject.Assisted;
import com.google.tsunami.common.cli.CliOption;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;

/** An {@link Archiver} implementation that archives data into Google Cloud Storage. */
public class GoogleCloudStorageArchiver implements Archiver {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  // For sanity-checking and to parse out the bucket name and object id.
  // See https://cloud.google.com/storage/docs/bucket-naming
  public static final Pattern GS_URL_PATTERN = Pattern.compile("gs://([^/]{3,63})/(.*)");

  private final Options options;
  private final Storage storage;

  /** All command line options for {@link GoogleCloudStorageArchiver}. */
  @Parameters(separators = "=")
  public static final class Options implements CliOption {
    @Parameter(
        names = "--gcs-archiver-chunk-size-in-bytes",
        description = "The size of the data chunk when GCS archiver uploads data to Cloud Storage.",
        validateWith = PositiveInteger.class)
    int chunkSizeInBytes = 1_000;

    @Parameter(
        names = "--gcs-archiver-chunk-upload-threshold-in-bytes",
        description = "The default data size threshold in bytes to enable chunk upload to GCS.",
        validateWith = PositiveInteger.class)
    int chunkUploadThresholdInBytes = 1_000_000;

    @Override
    public void validate() {}
  }

  @Inject
  public GoogleCloudStorageArchiver(Options options, @Assisted Storage storage) {
    this.options = checkNotNull(options);
    this.storage = checkNotNull(storage);
  }

  private static BlobInfo parseBlobInfo(String gcsUrl) {
    Matcher matcher = GS_URL_PATTERN.matcher(gcsUrl);
    checkArgument(matcher.matches(), "Invalid GCS URL: '%s'", gcsUrl);

    String bucketName = matcher.group(1);
    String objectName = matcher.group(2);
    return BlobInfo.newBuilder(bucketName, objectName).build();
  }

  @Override
  public boolean archive(String gcsUrl, byte[] data) {
    BlobInfo blobInfo = parseBlobInfo(gcsUrl);

    if (data.length <= options.chunkUploadThresholdInBytes) {
      // Create the blob in one request.
      logger.atInfo().log("Archiving data to GCS at '%s' in one request.", gcsUrl);
      storage.create(blobInfo, data);
      return true;
    }

    // When content is large (1MB or more) it is recommended to write it in chunks via the blob's
    // channel writer.
    logger.atInfo().log(
        "Content is larger than threshold, archiving data to GCS at '%s' in chunks.", gcsUrl);
    try (WriteChannel writer = storage.writer(blobInfo)) {
      for (int chunkOffset = 0;
          chunkOffset < data.length;
          chunkOffset += options.chunkSizeInBytes) {
        int chunkSize = Math.min(data.length - chunkOffset, options.chunkSizeInBytes);
        writer.write(ByteBuffer.wrap(data, chunkOffset, chunkSize));
      }
      return true;
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Unable to archving data to GCS at '%s'.", gcsUrl);
      return false;
    }
  }

  /** The factory of {@link GoogleCloudStorageArchiver} types for usage with assisted injection. */
  // TODO(b/145315535): consider wrap the Storage API into a client library. Current implementation
  // is not easily testable.
  public interface Factory {
    GoogleCloudStorageArchiver create(Storage storage);
  }
}
