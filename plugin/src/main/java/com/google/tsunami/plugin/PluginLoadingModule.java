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
package com.google.tsunami.plugin;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.flogger.GoogleLogger;
import com.google.inject.AbstractModule;
import io.github.classgraph.AnnotationClassRef;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.lang.reflect.Constructor;

/**
 * A Guice module that loads all {@link TsunamiPlugin TsunamiPlugins} at runtime.
 *
 * <p>This module relies on the {@link io.github.classgraph.ClassGraph} scan results to identify all
 * installed {@link TsunamiPlugin TsunamiPlugins} and bootstrap each {@link TsunamiPlugin plugin}
 * using the corresponding {@link PluginBootstrapModule} instantiated via reflection.
 */
public final class PluginLoadingModule extends AbstractModule {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  private static final String TSUNAMI_PLUGIN_INTERFACE = "com.google.tsunami.plugin.TsunamiPlugin";
  private static final String PLUGIN_INFO_ANNOTATION =
      "com.google.tsunami.plugin.annotations.PluginInfo";
  private static final String BOOTSTRAP_MODULE_PARAM_NAME = "bootstrapModule";

  private final boolean bootstrapModuleAlwaysAccessible;
  private final ScanResult classScanResult;

  public PluginLoadingModule(ScanResult classScanResult) {
    this(false, classScanResult);
  }

  @VisibleForTesting
  PluginLoadingModule(boolean bootstrapModuleAlwaysAccessible, ScanResult classScanResult) {
    this.bootstrapModuleAlwaysAccessible = bootstrapModuleAlwaysAccessible;
    this.classScanResult = checkNotNull(classScanResult);
  }

  @Override
  protected void configure() {
    ClassInfoList tsunamiPluginClasses =
        classScanResult
            .getClassesImplementing(TSUNAMI_PLUGIN_INTERFACE)
            .filter(
                classInfo ->
                    !classInfo.isInterface()
                        && !classInfo.implementsInterface(
                            "com.google.tsunami.plugin.RemoteVulnDetector"));
    for (ClassInfo tsunamiPluginClass : tsunamiPluginClasses) {
      logger.atInfo().log("Found plugin class: %s", tsunamiPluginClass.getName());
      // PluginInfo annotation is required for TsunamiPlugin.
      if (!tsunamiPluginClass.hasAnnotation(PLUGIN_INFO_ANNOTATION)) {
        throw new IllegalStateException(
            String.format(
                "Tsunami plugin '%s' must be annotated with PluginInfo",
                tsunamiPluginClass.getSimpleName()));
        }
      install(newPluginBootstrapModule(tsunamiPluginClass));
    }
  }

  private PluginBootstrapModule newPluginBootstrapModule(ClassInfo tsunamiPluginClass) {
    // Retrieves the bootstrap module from the PluginInfo annotation.
    Object bootstrapModuleValue =
        tsunamiPluginClass
            .getAnnotationInfo(PLUGIN_INFO_ANNOTATION)
            .getParameterValues()
            .getValue(BOOTSTRAP_MODULE_PARAM_NAME);
    if (!(bootstrapModuleValue instanceof AnnotationClassRef)) {
      throw new AssertionError(
          String.format(
              "Invalid bootstrapModule parameter type for Tsunami plugin '%s'",
              tsunamiPluginClass.getSimpleName()));
    }
    ClassInfo bootstrapModuleClassInfo = ((AnnotationClassRef) bootstrapModuleValue).getClassInfo();
    if (bootstrapModuleClassInfo == null) {
      throw new AssertionError(
          String.format(
              "bootstrapModule class for plugin '%s' not found in classpath",
              tsunamiPluginClass.getSimpleName()));
    }

    // Instantiate the bootstrap module via reflection.
    try {
      Constructor<? extends PluginBootstrapModule> pluginBootstrapModuleConstructor =
          bootstrapModuleClassInfo.loadClass(PluginBootstrapModule.class).getDeclaredConstructor();
      if (bootstrapModuleAlwaysAccessible) {
        pluginBootstrapModuleConstructor.setAccessible(true);
      }
      return pluginBootstrapModuleConstructor.newInstance();
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(
          String.format(
              "PluginBootstrapModule '%s' for plugin '%s' must be publicly constructable via a"
                  + " no-argument constructor",
              bootstrapModuleClassInfo.getSimpleName(), tsunamiPluginClass.getSimpleName()),
          e);
    }
  }
}
