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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.tsunami.common.server.LanguageServerCommand;
import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class RemoteServerLoaderTest {

  @Test
  public void runServerProcess_whenPathExistsAndNormalPort_returnsValidProcessList() {
    ImmutableList<LanguageServerCommand> commands =
        ImmutableList.of(
            LanguageServerCommand.create(
                "/bin/sh",
                "34567",
                "34",
                false,
                Duration.ofSeconds(10),
                "157.34.0.2",
                8080,
                "157.34.0.2:8881"));

    RemoteServerLoader loader =
        Guice.createInjector(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    install(new RemoteServerLoaderModule(commands));
                  }
                })
            .getInstance(RemoteServerLoader.class);
    var processList = loader.runServerProcesses();
    assertThat(processList).hasSize(1);
    assertThat(processList.get(0)).isNotNull();
  }
}


