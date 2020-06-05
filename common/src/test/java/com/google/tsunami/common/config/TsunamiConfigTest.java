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
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link TsunamiConfig}. */
@RunWith(JUnit4.class)
public final class TsunamiConfigTest {
  private static final String TEST_PROPERTY = "test.property";

  @After
  public void tearDown() {
    System.clearProperty(TEST_PROPERTY);
  }

  @Test
  public void fromYamlData_always_createTsunamiConfigFromMapData() {
    TsunamiConfig tsunamiConfig = TsunamiConfig.fromYamlData(ImmutableMap.of("test", "value"));

    assertThat(tsunamiConfig.getRawConfigData()).containsEntry("test", "value");
  }

  @Test
  public void fromYamlData_whenNullYamlData_createEmptyConfigData() {
    TsunamiConfig tsunamiConfig = TsunamiConfig.fromYamlData(null);

    assertThat(tsunamiConfig.getRawConfigData()).isEmpty();
  }

  @Test
  public void getSystemProperty_whenPropertyExists_returnsPropertyValue() {
    System.setProperty(TEST_PROPERTY, "Test value");

    assertThat(TsunamiConfig.getSystemProperty(TEST_PROPERTY)).hasValue("Test value");
  }

  @Test
  public void getSystemProperty_whenPropertyNotExists_returnsEmptyOptional() {
    assertThat(TsunamiConfig.getSystemProperty(TEST_PROPERTY)).isEmpty();
  }

  @Test
  public void getSystemProperty_whenPropertyNotExistsWithDefaultValue_returnsDefaultValue() {
    assertThat(TsunamiConfig.getSystemProperty(TEST_PROPERTY, "Default")).isEqualTo("Default");
  }

  @Test
  public void getConfig_whenValidConfigData_returnsBoundConfigObject() {
    TsunamiConfig tsunamiConfig =
        TsunamiConfig.fromYamlData(ImmutableMap.of("string", "string_value", "number", 1234));

    SimpleConfig config = tsunamiConfig.getConfig("", SimpleConfig.class);

    assertThat(config.string).isEqualTo("string_value");
    assertThat(config.number).isEqualTo(1234);
  }

  @Test
  public void getConfig_whenValidConfigDataAndPrefix_returnsBoundConfigObject() {
    TsunamiConfig tsunamiConfig =
        TsunamiConfig.fromYamlData(
            ImmutableMap.of(
                "test",
                ImmutableMap.of(
                    "prefix", ImmutableMap.of("string", "string_value", "number", 1234))));

    SimpleConfig config = tsunamiConfig.getConfig("test.prefix", SimpleConfig.class);

    assertThat(config.string).isEqualTo("string_value");
    assertThat(config.number).isEqualTo(1234);
  }

  @Test
  public void getConfig_whenRequestedConfigNotExists_returnsObjectWithDefaultValue() {
    TsunamiConfig tsunamiConfig =
        TsunamiConfig.fromYamlData(
            ImmutableMap.of(
                "test",
                ImmutableMap.of(
                    "prefix", ImmutableMap.of("string", "string_value", "number", 1234))));

    SimpleConfig config = tsunamiConfig.getConfig("not.exist.prefix", SimpleConfig.class);

    assertThat(config.string).isNull();
    assertThat(config.number).isEqualTo(0);
  }

  @Test
  public void getConfig_whenConfigHasComplicateDataStructure_returnsValidObject() {
    ImmutableList<String> stringsField = ImmutableList.of("a", "b", "c");
    ImmutableMap<String, List<Long>> complicateField =
        ImmutableMap.of("keyA", ImmutableList.of(1L, 2L), "keyB", ImmutableList.of(123L));
    TsunamiConfig tsunamiConfig =
        TsunamiConfig.fromYamlData(
            ImmutableMap.of(
                "strings", stringsField, "complicateField", complicateField));

    CollectionConfig config = tsunamiConfig.getConfig("", CollectionConfig.class);

    assertThat(config.strings).isEqualTo(stringsField);
    assertThat(config.complicateField).isEqualTo(complicateField);
  }

  @Test
  public void getConfig_whenConfigDataUseLowerUnderscoreCase_returnsValidObject() {
    ImmutableList<String> stringsField = ImmutableList.of("a", "b", "c");
    ImmutableMap<String, List<Long>> complicateField =
        ImmutableMap.of("keyA", ImmutableList.of(1L, 2L), "keyB", ImmutableList.of(123L));
    TsunamiConfig tsunamiConfig =
        TsunamiConfig.fromYamlData(
            ImmutableMap.of(
                "strings", stringsField, "complicate_field", complicateField));

    CollectionConfig config = tsunamiConfig.getConfig("", CollectionConfig.class);

    assertThat(config.strings).isEqualTo(stringsField);
    assertThat(config.complicateField).isEqualTo(complicateField);
  }

  @Test
  public void getConfig_whenRequestedConfigHasInvalidType_throwsException() {
    TsunamiConfig tsunamiConfig =
        TsunamiConfig.fromYamlData(
            ImmutableMap.of("test", ImmutableMap.of("prefix", "invalid_type")));

    assertThrows(
        ConfigException.class, () -> tsunamiConfig.getConfig("test.prefix", SimpleConfig.class));
  }

  @Test
  public void getConfig_whenUnassignableConfigValue_throwsException() {
    TsunamiConfig tsunamiConfig =
        TsunamiConfig.fromYamlData(
            ImmutableMap.of("string", "string_value", "number", "incompatible_value"));

    assertThrows(
        IllegalArgumentException.class, () -> tsunamiConfig.getConfig("", SimpleConfig.class));
  }

  @Test
  public void getConfig_whenInvalidConfigObject_throwsException() {
    TsunamiConfig tsunamiConfig =
        TsunamiConfig.fromYamlData(ImmutableMap.of("string", "string_value"));

    assertThrows(AssertionError.class, () -> tsunamiConfig.getConfig("", InvalidConfig.class));
  }

  private static final class SimpleConfig {
    String string;
    long number;
  }

  private static final class CollectionConfig {
    List<String> strings;
    Map<String, List<Long>> complicateField;
  }

  private static final class InvalidConfig {
    String field;

    InvalidConfig(String field) {
      this.field = field;
    }
  }
}
