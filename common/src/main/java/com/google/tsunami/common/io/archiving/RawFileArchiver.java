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

import com.google.common.base.Strings;
import com.google.common.flogger.GoogleLogger;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;

/** An {@link Archiver} implementation that archives data into file systems as raw files. */
public class RawFileArchiver implements Archiver {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  @Override
  public boolean archive(String fileName, byte[] data) {
    checkArgument(!Strings.isNullOrEmpty(fileName));
    checkNotNull(data);

    try {
      logger.atInfo().log("Archiving data to file system with filename '%s'.", fileName);
      Files.asByteSink(new File(fileName)).write(data);
      return true;
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Failed archiving data to file '%s'.", fileName);
      return false;
    }
  }
}
