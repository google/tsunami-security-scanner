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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.flogger.GoogleLogger;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** A {@link ConfigLoader} implementation that loads Tsunami configs from YAML file. */
public final class YamlConfigLoader implements ConfigLoader {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  private static final String DEFAULT_CONFIG_FILE = "tsunami.yaml";

  @Override
  public TsunamiConfig loadConfig() {
    Yaml yaml = new Yaml(new SafeConstructor());
    Map<String, Object> rawYamlData = yaml.load(configFileReader());
    return TsunamiConfig.fromYamlData(rawYamlData);
  }

  private static Reader configFileReader() {
    String configFile =
        TsunamiConfig.getSystemProperty("tsunami.config.location").orElse(DEFAULT_CONFIG_FILE);

    try {
      return Files.newReader(new File(configFile), UTF_8);
    } catch (FileNotFoundException e) {
      logger.atWarning().log(
          "Unable to read config file '%s', default to empty config.", configFile);
      return new StringReader("");
    }
  }
}
