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

import com.google.common.flogger.GoogleLogger;
import com.google.inject.AbstractModule;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

/**
 * A Guice module that binds all Tsunami config objects at runtime.
 *
 * <p>This module relies on the {@link io.github.classgraph.ClassGraph} scan results to identify all
 * Tsunami config objects annotated by the {@link
 * com.google.tsunami.common.config.annotations.ConfigProperties} annotation. Each config class is
 * bound to a singleton object whose fields are populated from the Tsunami config file.
 */
public final class ConfigModule extends AbstractModule {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  private static final String CONFIG_PROPERTIES_ANNOTATION =
      "com.google.tsunami.common.config.annotations.ConfigProperties";

  private final ScanResult scanResult;
  private final TsunamiConfig tsunamiConfig;

  public ConfigModule(ScanResult scanResult, TsunamiConfig tsunamiConfig) {
    this.scanResult = checkNotNull(scanResult);
    this.tsunamiConfig = checkNotNull(tsunamiConfig);
  }

  @Override
  protected void configure() {
    bind(TsunamiConfig.class).toInstance(tsunamiConfig);

    for (ClassInfo configClass :
        scanResult
            .getClassesWithAnnotation(CONFIG_PROPERTIES_ANNOTATION)
            .filter(classInfo -> !classInfo.isAbstract())) {
      logger.atInfo().log("Found Tsunami config class: %s", configClass.getName());

      bindConfigClass(getConfigPrefix(configClass), configClass.loadClass());
    }
  }

  private <T> void bindConfigClass(String configPrefix, Class<T> configClass) {
    T configObject = tsunamiConfig.getConfig(configPrefix, configClass);
    bind(configClass).toInstance(configObject);
  }

  private static String getConfigPrefix(ClassInfo configClass) {
    Object configPrefix =
        configClass
            .getAnnotationInfo(CONFIG_PROPERTIES_ANNOTATION)
            .getParameterValues()
            .getValue("value");
    if (!(configPrefix instanceof String)) {
      throw new AssertionError("SHOULD NEVER HAPPEN, ConfigProperties value is not a string.");
    }

    return (String) configPrefix;
  }
}
