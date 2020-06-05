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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/** Tests for {@link CommandExecutorFactory}. */
@RunWith(JUnit4.class)
public final class CommandExecutorFactoryTest {

  @Test
  public void getInstance_whenNoPreviousInstanceIsProvided_createsNewProcessExecutor() {
    CommandExecutor executor = CommandExecutorFactory.create("fakeArgs");
    assertThat(executor).isNotNull();
  }

  @Test
  public void getInstance_whenPreviousInstanceIsProvided_returnsProvidedInstance() {
    CommandExecutor executor = Mockito.mock(CommandExecutor.class);
    CommandExecutorFactory.setInstance(executor);

    assertThat(CommandExecutorFactory.create("fakeArgs")).isSameInstanceAs(executor);
  }
}
