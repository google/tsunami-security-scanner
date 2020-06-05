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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

/** A data holder for all Tsunami config data, including config files and Java system properties. */
public final class TsunamiConfig {
  private static final Converter<String, String> FIELD_NAME_TO_LOWER_UNDERSCORE =
      CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE);
  private static final Splitter CONFIG_PATH_SPLITTER = Splitter.on('.').omitEmptyStrings();

  private final ImmutableMap<String, Object> rawConfigData;

  private TsunamiConfig(ImmutableMap<String, Object> rawConfigData) {
    this.rawConfigData = checkNotNull(rawConfigData);
  }

  ImmutableMap<String, Object> getRawConfigData() {
    return rawConfigData;
  }

  public static TsunamiConfig fromYamlData(Map<String, Object> yamlConfig) {
    return new TsunamiConfig(
        yamlConfig == null ? ImmutableMap.of() : ImmutableMap.copyOf(yamlConfig));
  }

  public static Optional<String> getSystemProperty(String propertyName) {
    return Optional.ofNullable(getSystemProperty(propertyName, null));
  }

  public static String getSystemProperty(String propertyName, String def) {
    return System.getProperty(propertyName, def);
  }

  /**
   * Get a config object with the given {@code configPrefix} and bind all config values to the
   * requested {@code clazz}.
   *
   * <p>This code uses reflection to create the requested config object. The request type {@code T}
   * must provide a no-argument or default constructor.
   *
   * @param configPrefix the prefix of the config to be read from.
   * @param clazz the class of the returned config object.
   * @param <T> actual config object type.
   * @return an object whose field values are filled by the config data under the given {@code
   *     configPrefix}.
   */
  public <T> T getConfig(String configPrefix, Class<T> clazz) {
    checkNotNull(configPrefix);
    checkNotNull(clazz);

    Map<String, Object> configValue = readConfigValue(configPrefix);
    return newConfigObject(clazz, configValue);
  }

  @SuppressWarnings("unchecked") // We know Map key is always String from yaml file.
  private ImmutableMap<String, Object> readConfigValue(String configPrefix) {
    Map<String, Object> retrievedData = rawConfigData;

    // Config prefixes are dot separated words list, e.g. example.config.prefix.
    for (String configKey : CONFIG_PATH_SPLITTER.split(configPrefix)) {
      // Requested data not found under configPrefix.
      if (!retrievedData.containsKey(configKey)) {
        return ImmutableMap.of();
      }

      Object configData = retrievedData.get(configKey);
      if (!(configData instanceof Map)) {
        throw new ConfigException(
            String.format(
                "Unexpected data type for config '%s', expected '%s', got '%s'",
                configKey, Map.class, configData.getClass()));
      }

      retrievedData = (Map<String, Object>) configData;
    }

    return ImmutableMap.copyOf(retrievedData);
  }

  private static <T> T newConfigObject(Class<T> clazz, Map<String, Object> configValue) {
    try {
      Constructor<T> configObjectCtor = clazz.getDeclaredConstructor();
      // Always create an instance of the config data regardless of scope.
      configObjectCtor.setAccessible(true);
      T configObject = configObjectCtor.newInstance();

      // Fill each field of the configObject from configValue using the field name as key.
      for (Field field : clazz.getDeclaredFields()) {
        String fieldName = field.getName();
        if (configValue.containsKey(fieldName)
            || configValue.containsKey(FIELD_NAME_TO_LOWER_UNDERSCORE.convert(fieldName))) {
          Object fieldValue =
              Optional.ofNullable(configValue.get(fieldName))
                  .orElse(configValue.get(FIELD_NAME_TO_LOWER_UNDERSCORE.convert(fieldName)));
          field.setAccessible(true);
          field.set(configObject, fieldValue);
        }
      }

      return configObject;
    } catch (ReflectiveOperationException e) {
      // This is bad. Config objects cannot be created or config value cannot be assigned to the
      // field, we throw assertion error and fail the execution.
      throw new AssertionError(
          String.format(
              "Unable to create new instance of '%s' using config value '%s'", clazz, configValue),
          e);
    }
  }
}
