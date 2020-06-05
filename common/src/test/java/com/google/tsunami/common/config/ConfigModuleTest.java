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
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.tsunami.common.config.annotations.ConfigProperties;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ConfigModule}. */
@RunWith(JUnit4.class)
public final class ConfigModuleTest {

  @Test
  public void configure_always_bindsGivenTsunamiConfigObject() {
    TsunamiConfig tsunamiConfig = TsunamiConfig.fromYamlData(ImmutableMap.of());
    try (ScanResult scanResult =
        new ClassGraph()
            .enableAllInfo()
            .whitelistClasses(TestConfigWithoutPrefix.class.getTypeName())
            .scan()) {
      Injector injector = Guice.createInjector(new ConfigModule(scanResult, tsunamiConfig));

      TsunamiConfig boundTsunamiConfig = injector.getInstance(TsunamiConfig.class);

      assertThat(boundTsunamiConfig).isSameInstanceAs(tsunamiConfig);
    }
  }

  @Test
  public void configure_whenValidConfigData_bindsSuccessfully() {
    TsunamiConfig tsunamiConfig =
        TsunamiConfig.fromYamlData(ImmutableMap.of("string_config", "testString"));
    try (ScanResult scanResult =
        new ClassGraph()
            .enableAllInfo()
            .whitelistClasses(TestConfigWithoutPrefix.class.getTypeName())
            .scan()) {
      Injector injector = Guice.createInjector(new ConfigModule(scanResult, tsunamiConfig));

      TestConfigWithoutPrefix testConfig = injector.getInstance(TestConfigWithoutPrefix.class);

      assertThat(testConfig.stringConfig).isEqualTo("testString");
    }
  }

  @Test
  public void configure_whenMissingMatchedConfigData_bindsObjectWithDefaultValue() {
    TsunamiConfig tsunamiConfig = TsunamiConfig.fromYamlData(ImmutableMap.of());
    try (ScanResult scanResult =
        new ClassGraph()
            .enableAllInfo()
            .whitelistClasses(TestConfigWithoutPrefix.class.getTypeName())
            .scan()) {
      Injector injector = Guice.createInjector(new ConfigModule(scanResult, tsunamiConfig));

      TestConfigWithoutPrefix testConfig = injector.getInstance(TestConfigWithoutPrefix.class);

      assertThat(testConfig.stringConfig).isNull();
    }
  }

  @Test
  public void configure_whenValidConfigDataWithPrefix_bindsSuccessfully() {
    TsunamiConfig tsunamiConfig =
        TsunamiConfig.fromYamlData(
            ImmutableMap.of(
                "test", ImmutableMap.of("prefix", ImmutableMap.of("string_config", "testString"))));
    try (ScanResult scanResult =
        new ClassGraph()
            .enableAllInfo()
            .whitelistClasses(TestConfigWithPrefix.class.getTypeName())
            .scan()) {
      Injector injector = Guice.createInjector(new ConfigModule(scanResult, tsunamiConfig));

      TestConfigWithPrefix testConfig = injector.getInstance(TestConfigWithPrefix.class);

      assertThat(testConfig.stringConfig).isEqualTo("testString");
    }
  }

  @Test
  public void configure_whenInvalidConfigClass_throwsException() {
    TsunamiConfig tsunamiConfig =
        TsunamiConfig.fromYamlData(ImmutableMap.of("string_config", "testString"));
    try (ScanResult scanResult =
        new ClassGraph()
            .enableAllInfo()
            .whitelistClasses(InvalidConfig.class.getTypeName())
            .scan()) {
      assertThrows(
          AssertionError.class,
          () -> Guice.createInjector(new ConfigModule(scanResult, tsunamiConfig)));
    }
  }

  @ConfigProperties("")
  private static final class TestConfigWithoutPrefix {
    String stringConfig;
  }

  @ConfigProperties("test.prefix")
  private static final class TestConfigWithPrefix {
    String stringConfig;
  }

  @ConfigProperties("")
  private static final class InvalidConfig {
    String stringConfig;

    InvalidConfig(String stringConfig) {
      this.stringConfig = stringConfig;
    }
  }
}
