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
package com.google.tsunami.common.config;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link YamlConfigLoader}. */
@RunWith(JUnit4.class)
public final class YamlConfigLoaderTest {
  private static final String YAML_DATA = "test: \"data\"\ntest2: 123";

  @After
  public void tearDown() {
    System.clearProperty("tsunami.config.location");
  }

  @Test
  public void loadConfig_whenValidYamlFile_loadsConfigFromFile() throws IOException {
    File configFile = File.createTempFile("YamlConfigLoaderTest", ".yaml");
    Files.asCharSink(configFile, UTF_8).write(YAML_DATA);
    System.setProperty("tsunami.config.location", configFile.getAbsolutePath());

    TsunamiConfig tsunamiConfig = new YamlConfigLoader().loadConfig();

    assertThat(tsunamiConfig.getRawConfigData()).containsExactly("test", "data", "test2", 123);
  }

  @Test
  public void loadConfig_whenYamlFileNotFound_usesEmptyConfig() {
    TsunamiConfig tsunamiConfig = new YamlConfigLoader().loadConfig();

    assertThat(tsunamiConfig.getRawConfigData()).isEmpty();
  }
}
