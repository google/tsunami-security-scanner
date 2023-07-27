/*
 * Copyright 2022 Google LLC
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
package com.google.tsunami.main.cli.server;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.GoogleLogger;
import com.google.tsunami.common.command.CommandExecutor;
import com.google.tsunami.common.command.CommandExecutorFactory;
import com.google.tsunami.common.server.LanguageServerCommand;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Qualifier;

/** Loader to run language servers. */
public class RemoteServerLoader {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private final List<LanguageServerCommand> commands;

  @Inject
  RemoteServerLoader(@LanguageServerCommands List<LanguageServerCommand> commands) {
    this.commands = checkNotNull(commands);
  }

  public ImmutableList<Process> runServerProcesses() {
    logger.atInfo().log("Starting language server processes (if any)...");
    return commands.stream()
        .map(
            command ->
                runProcess(
                    CommandExecutorFactory.create(
                        command.serverCommand(),
                        getCommand("--port=", command.port()),
                        getCommand("--log_id=", command.logId()),
                        "--trust_all_ssl_cert=" + command.trustAllSslCert(),
                        getCommand("--timeout_seconds=", command.timeoutSeconds().getSeconds()),
                        getCommand("--callback_address=", command.callbackAddress()),
                        getCommand("--callback_port=", command.callbackPort()),
                        getCommand("--polling_uri=", command.pollingUri()))))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toImmutableList());
  }

  private String getCommand(String flag, Object command) {
    return command.toString().isEmpty() || command.toString().equals("0") ? "" : flag + command;
  }

  private Optional<Process> runProcess(CommandExecutor executor) {
    try {
      return Optional.of(executor.executeAsync());
    } catch (IOException | InterruptedException | ExecutionException e) {
      logger.atWarning().withCause(e).log("Could not execute language server binary.");
    }
    return Optional.empty();
  }

  /** Guice interface for injecting {@link LanguageServerCommand} object lists. */
  @Qualifier
  @Retention(RUNTIME)
  public @interface LanguageServerCommands {}
}
