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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.util.Types;
import com.google.tsunami.plugin.annotations.PluginInfo;
import com.google.tsunami.proto.DetectionReportList;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.PortScanningReport;
import com.google.tsunami.proto.ScanTarget;
import com.google.tsunami.proto.TargetInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link PluginLoadingModule}. */
@RunWith(JUnit4.class)
public final class PluginLoadingModuleTest {
  @SuppressWarnings("unchecked")
  private static final Key<Map<PluginDefinition, TsunamiPlugin>> PLUGIN_BINDING_KEY =
      (Key<Map<PluginDefinition, TsunamiPlugin>>)
          Key.get(Types.mapOf(PluginDefinition.class, TsunamiPlugin.class));

  private static final ImmutableList<String> COMMON_CLASSES_TO_LOAD =
      ImmutableList.of(
          TsunamiPlugin.class.getTypeName(),
          PortScanner.class.getTypeName(),
          VulnDetector.class.getTypeName(),
          PluginBootstrapModule.class.getTypeName());

  @Test
  public void configure_always_loadsAllTsunamiPlugins() {
    ImmutableList<String> whitelistedClasses =
        ImmutableList.<String>builder()
            .addAll(COMMON_CLASSES_TO_LOAD)
            .add(
                FakePortScanner.class.getTypeName(),
                FakePortScannerBootstrapModule.class.getTypeName(),
                FakeVulnDetector.class.getTypeName(),
                FakeVulnDetectorBootstrapModule.class.getTypeName(),
                FakeVulnDetector2.class.getTypeName(),
                FakeVulnDetector2.FakeVulnDetector2BootstrapModule.class.getTypeName())
            .build();
    try (ScanResult classScanResult =
        new ClassGraph()
            .enableAllInfo()
            .whitelistClasses(whitelistedClasses.toArray(new String[0]))
            .scan()) {
      Iterable<Class<? extends TsunamiPlugin>> installedPluginTypes =
          Guice.createInjector(new PluginLoadingModule(true, classScanResult))
              .getInstance(PLUGIN_BINDING_KEY)
              .values()
              .stream()
              .map(TsunamiPlugin::getClass)
              .collect(toImmutableList());

      assertThat(installedPluginTypes)
          .containsExactly(FakePortScanner.class, FakeVulnDetector.class, FakeVulnDetector2.class);
    }
  }

  @Test
  public void configure_whenPluginMissingRequiredAnnotation_throwsException() {
    ImmutableList<String> whitelistedClasses =
        ImmutableList.<String>builder()
            .addAll(COMMON_CLASSES_TO_LOAD)
            .add(NoAnnotationDetector.class.getTypeName())
            .build();
    try (ScanResult classScanResult =
        new ClassGraph()
            .enableAllInfo()
            .whitelistClasses(whitelistedClasses.toArray(new String[0]))
            .scan()) {
      CreationException ex =
          assertThrows(
              CreationException.class,
              () ->
                  Guice.createInjector(new PluginLoadingModule(true, classScanResult))
                      .getAllBindings());
      assertThat(ex).hasCauseThat().isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  public void configure_whenPluginBootstrapModuleCannotBeInitialized_throwsException() {
    ImmutableList<String> whitelistedClasses =
        ImmutableList.<String>builder()
            .addAll(COMMON_CLASSES_TO_LOAD)
            .add(
                FakePortScanner.class.getTypeName(),
                FakePortScannerBootstrapModule.class.getTypeName())
            .build();
    try (ScanResult classScanResult =
        new ClassGraph()
            .enableAllInfo()
            .whitelistClasses(whitelistedClasses.toArray(new String[0]))
            .scan()) {
      assertThrows(
          AssertionError.class,
          () ->
              Guice.createInjector(new PluginLoadingModule(false, classScanResult))
                  .getAllBindings());
    }
  }

  @PluginInfo(
      type = PluginType.PORT_SCAN,
      name = "FakePortScanner",
      version = "0.1",
      description = "FakePortScanner",
      author = "TestAuthor",
      bootstrapModule = FakePortScannerBootstrapModule.class)
  private static final class FakePortScanner implements PortScanner {
    @Override
    public PortScanningReport scan(ScanTarget scanTarget) {
      return null;
    }
  }

  private static final class FakePortScannerBootstrapModule extends PluginBootstrapModule {
    @Override
    protected void configurePlugin() {
      registerPlugin(FakePortScanner.class);
    }
  }

  @PluginInfo(
      type = PluginType.VULN_DETECTION,
      name = "FakeVulnDetector",
      version = "0.1",
      description = "FakeVulnDetector",
      author = "TestAuthor",
      bootstrapModule = FakeVulnDetectorBootstrapModule.class)
  private static final class FakeVulnDetector implements VulnDetector {
    @Override
    public DetectionReportList detect(
        TargetInfo targetInfo, ImmutableList<NetworkService> matchedServices) {
      return null;
    }
  }

  private static final class FakeVulnDetectorBootstrapModule extends PluginBootstrapModule {
    @Override
    protected void configurePlugin() {
      registerPlugin(FakeVulnDetector.class);
    }
  }

  @PluginInfo(
      type = PluginType.VULN_DETECTION,
      name = "FakeVulnDetector2",
      version = "0.1",
      description = "FakeVulnDetector2",
      author = "TestAuthor",
      bootstrapModule = FakeVulnDetector2.FakeVulnDetector2BootstrapModule.class)
  private static final class FakeVulnDetector2 implements VulnDetector {
    @Override
    public DetectionReportList detect(
        TargetInfo targetInfo, ImmutableList<NetworkService> matchedServices) {
      return null;
    }

    static final class FakeVulnDetector2BootstrapModule extends PluginBootstrapModule {
      @Override
      protected void configurePlugin() {
        registerPlugin(FakeVulnDetector2.class);
      }
    }
  }

  private static final class NoAnnotationDetector implements VulnDetector {
    @Override
    public DetectionReportList detect(
        TargetInfo targetInfo, ImmutableList<NetworkService> matchedServices) {
      return null;
    }
  }
}
