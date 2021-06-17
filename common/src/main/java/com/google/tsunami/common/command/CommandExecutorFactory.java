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

/** Utility class to simplify the creation and testing of {@link CommandExecutor} instances. */
public class CommandExecutorFactory {

  private static CommandExecutor instance;

  /**
   * Sets an executor instance that will be returned by all future calls to {@link
   * CommandExecutorFactory#create(String...)}
   *
   * @param executor The {@link CommandExecutor} returned by this factory.
   */
  public static void setInstance(CommandExecutor executor) {
    instance = executor;
  }

  /**
   * Creates a new {@link CommandExecutor} if none is set.
   *
   * @param args List of arguments to pass to the newly created {@link CommandExecutor}.
   * @return the {@link CommandExecutor} instance created by this factory.
   */
  public static CommandExecutor create(String... args) {
    if (instance == null) {
      return new CommandExecutor(args);
    }
    return instance;
  }

  private CommandExecutorFactory() {}
}
