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
package com.google.tsunami.common.command;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.flogger.GoogleLogger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Nullable;

/** Helper class that handles running a command line and collecting output and errors. */
// TODO(b/145315535): reimplement this class so that it is:
// 1. guice injectable in order to hide Executor interface.
// 2. unit testable to prevent actually executing commands in test.
public class CommandExecutor {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  private static final Joiner COMMAND_ARGS_JOINER = Joiner.on(" ");

  private final ProcessBuilder processBuilder;
  private final String[] args;
  private Process process;
  @Nullable private String output;
  @Nullable private String error;

  public CommandExecutor(String... args) {
    this.args = checkNotNull(args);
    this.processBuilder = new ProcessBuilder(args);
  }

  /*
   * Executes the command and uses a {@link ThreadPoolExecutor} to collect output and error.
   *
   * This is a convenience method for testing purposes only as the executor is not shared and
   * therefore defeats the purpose of having a cached thread pool.
   */
  @VisibleForTesting
  Process execute() throws IOException, InterruptedException, ExecutionException {
    // Nmap is a long running process and the collectStream method is a blocking method.
    // By default CompletableFuture uses ForkJoinPool, which is for suitable short
    // non-blocking operations.
    Executor executor = Executors.newCachedThreadPool();
    return execute(executor);
  }

  /**
   * Starts the command and uses the passed executor to collect output and error streams.
   *
   * <p>IMPORTANT: The stream collection uses an IO blocking method and the passed executor must be
   * well suited for the task. {@link ThreadPoolExecutor} is a viable option.
   *
   * @param executor The executor to collect output and error streams.
   * @return Started {@link Process} object.
   * @throws IOException if an I/O error occurs when starting the command executing process.
   * @throws InterruptedException if interrupted while waiting for the command's output.
   * @throws ExecutionException if the command execution failed.
   */
  public Process execute(Executor executor)
      throws IOException, InterruptedException, ExecutionException {
    logger.atInfo().log("Executing the following command: '%s'", COMMAND_ARGS_JOINER.join(args));
    process = processBuilder.start();
    output =
        CompletableFuture.supplyAsync(() -> collectStream(process.getInputStream()), executor)
            .get();
    error =
        CompletableFuture.supplyAsync(() -> collectStream(process.getErrorStream()), executor)
            .get();
    return process;
  }

  /**
   * Starts the command without collecting output and error streams.
   *
   * @return Started {@link Process} object.
   * @throws IOException if an I/O error occurs when starting the command executing process.
   * @throws InterruptedException if interrupted while starting the command executing process.
   * @throws ExecutionException if the command execution failed.
   */
  public Process executeWithNoStreamCollection()
      throws IOException, InterruptedException, ExecutionException {
    logger.atInfo().log("Executing the following command: '%s'", COMMAND_ARGS_JOINER.join(args));
    process = processBuilder.start();
    return process;
  }

  @Nullable
  public String getOutput() {
    return output;
  }

  @Nullable
  public String getError() {
    return error;
  }

  private static String collectStream(InputStream stream) {
    StringBuilder stringBuilder = new StringBuilder();
    try {
      String output;
      BufferedReader streamReader = new BufferedReader(new InputStreamReader(stream, UTF_8));
      while ((output = streamReader.readLine()) != null) {
        stringBuilder.append(output);
        stringBuilder.append("\n");
      }
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Error collecting output stream from command execution.");
    }
    return stringBuilder.toString();
  }
}
