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

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link CommandExecutor}. */
@RunWith(JUnit4.class)
public final class CommandExecutorTest {

  @Test
  public void execute_always_startsProcessAndReturnsProcessInstance()
      throws IOException, InterruptedException, ExecutionException {
    CommandExecutor executor = new CommandExecutor("/bin/sh", "-c", "echo 1");

    Process process = executor.execute();
    process.waitFor();

    assertThat(process.exitValue()).isEqualTo(0);
  }

  @Test
  public void executeWithNoStreamCollection_always_startsProcessAndReturnsProcessInstance()
      throws IOException, InterruptedException, ExecutionException {
    CommandExecutor executor = new CommandExecutor("/bin/sh", "-c", "echo 1");

    Process process = executor.executeWithNoStreamCollection();
    process.waitFor();

    assertThat(process.exitValue()).isEqualTo(0);
  }

  @Test
  public void getOutput_always_returnsExpect()
      throws IOException, InterruptedException, ExecutionException {
    CommandExecutor executor = new CommandExecutor("/bin/sh", "-c", "echo 1");

    Process process = executor.execute();
    process.waitFor();

    assertThat(executor.getOutput()).isEqualTo("1\n");
  }

  @Test
  public void getOutput_withMultipleGetOutputCalls_returnsExpect()
      throws IOException, InterruptedException, ExecutionException {
    CommandExecutor executor = new CommandExecutor("/bin/sh", "-c", "echo 1");

    Process process = executor.execute();
    process.waitFor();

    assertThat(executor.getOutput()).isEqualTo("1\n");
    assertThat(executor.getOutput()).isEqualTo("1\n");
  }

  @Test
  public void getError_always_returnsExpect()
      throws IOException, InterruptedException, ExecutionException {
    CommandExecutor executor = new CommandExecutor("/bin/sh", "-c", "echo 1 1>&2");

    Process process = executor.execute();
    process.waitFor();

    assertThat(executor.getError()).isEqualTo("1\n");
  }

  @Test
  public void getError_withMultipleGetOutputCalls_returnsExpect()
      throws IOException, InterruptedException, ExecutionException {
    CommandExecutor executor = new CommandExecutor("/bin/sh", "-c", "echo 1 1>&2");

    Process process = executor.execute();
    process.waitFor();

    assertThat(executor.getError()).isEqualTo("1\n");
    assertThat(executor.getError()).isEqualTo("1\n");
  }
}
