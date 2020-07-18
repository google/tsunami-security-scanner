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
package com.google.tsunami.main.cli;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.tsunami.common.data.NetworkEndpointUtils.forHostname;
import static com.google.tsunami.common.data.NetworkEndpointUtils.forIp;

import com.google.common.base.Stopwatch;
import com.google.common.flogger.GoogleLogger;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.tsunami.common.cli.CliOptionsModule;
import com.google.tsunami.common.command.CommandExecutorModule;
import com.google.tsunami.common.config.ConfigLoader;
import com.google.tsunami.common.config.ConfigModule;
import com.google.tsunami.common.config.TsunamiConfig;
import com.google.tsunami.common.config.YamlConfigLoader;
import com.google.tsunami.common.io.archiving.GoogleCloudStorageArchiverModule;
import com.google.tsunami.common.net.http.HttpClientModule;
import com.google.tsunami.common.reflection.ClassGraphModule;
import com.google.tsunami.common.time.SystemUtcClockModule;
import com.google.tsunami.main.cli.option.ScanTargetCliOptions;
import com.google.tsunami.plugin.PluginExecutionModule;
import com.google.tsunami.plugin.PluginLoadingModule;
import com.google.tsunami.proto.ScanResults;
import com.google.tsunami.proto.ScanTarget;
import com.google.tsunami.workflow.DefaultScanningWorkflow;
import com.google.tsunami.workflow.ScanningWorkflowException;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.apache.commons.net.util.SubnetUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.inject.Inject;

/** Command line interface for the Tsunami Security Scanner. */
public final class TsunamiCli {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private final DefaultScanningWorkflow scanningWorkflow;
  private final ScanResultsArchiver scanResultsArchiver;
  private final ScanTargetCliOptions scanTargetCliOptions;

  @Inject
  TsunamiCli(
      DefaultScanningWorkflow scanningWorkflow,
      ScanResultsArchiver scanResultsArchiver,
      ScanTargetCliOptions scanTargetCliOptions) {
    this.scanningWorkflow = checkNotNull(scanningWorkflow);
    this.scanResultsArchiver = checkNotNull(scanResultsArchiver);
    this.scanTargetCliOptions = checkNotNull(scanTargetCliOptions);
  }

  public void run()
      throws ExecutionException, InterruptedException, ScanningWorkflowException, IOException {
    logger.atInfo().log("TsunamiCli starting...");

    int coreCount = Runtime.getRuntime().availableProcessors();
    ExecutorService executor = Executors.newFixedThreadPool(2 * coreCount);

    List<ScanTarget> scanTargets = buildScanTarget();
    List<Future<ScanResults>> scanResults = new ArrayList<>();

    for (ScanTarget target : scanTargets) {
      scanResults.add(executor.submit(() -> scanningWorkflow.run(target)));
    }

    logger.atInfo().log("Tsunami scan finished, saving results.");

    for (Future<ScanResults> scanResult : scanResults) {
        saveResults(scanResult.get());
    }

    logger.atInfo().log("TsunamiCli finished...");
  }

  private List<ScanTarget> buildScanTarget() {
    if (scanTargetCliOptions.ipV4SubnetTarget != null) {
      return buildSubnetTargets(scanTargetCliOptions.ipV4SubnetTarget);
    }
    if (scanTargetCliOptions.ipV6SubnetTarget != null) {
      return buildSubnetTargets(scanTargetCliOptions.ipV6SubnetTarget);
    }

    ScanTarget.Builder scanTargetBuilder = ScanTarget.newBuilder();

    if (scanTargetCliOptions.ipV4Target != null) {
      scanTargetBuilder.setNetworkEndpoint(forIp(scanTargetCliOptions.ipV4Target));
    } else if (scanTargetCliOptions.ipV6Target != null) {
      scanTargetBuilder.setNetworkEndpoint(forIp(scanTargetCliOptions.ipV6Target));
    } else {
      scanTargetBuilder.setNetworkEndpoint(forHostname(scanTargetCliOptions.hostnameTarget));
    }

    return new ArrayList<>(Collections.singletonList(scanTargetBuilder.build()));
  }

  private List<ScanTarget> buildSubnetTargets(String subnetTarget) {
    ArrayList<ScanTarget> scanTargets = new ArrayList<>();
    SubnetUtils subnet = new SubnetUtils(subnetTarget);
    String[] subnetAddresses = subnet.getInfo().getAllAddresses();
    for (String address : subnetAddresses) {
      ScanTarget.Builder scanTargetBuilder = ScanTarget.newBuilder();
      scanTargetBuilder.setNetworkEndpoint(forIp(address));
      scanTargets.add(scanTargetBuilder.build());
    }
    return scanTargets;
  }

  private void saveResults(ScanResults scanResults) throws IOException {
    scanResultsArchiver.archive(scanResults);
  }

  private static final class TsunamiCliModule extends AbstractModule {
    private final ScanResult classScanResult;
    private final String[] args;
    private final TsunamiConfig tsunamiConfig;

    TsunamiCliModule(ScanResult classScanResult, String[] args, TsunamiConfig tsunamiConfig) {
      this.classScanResult = checkNotNull(classScanResult);
      this.args = checkNotNull(args);
      this.tsunamiConfig = checkNotNull(tsunamiConfig);
    }

    @Override
    protected void configure() {
      install(new ClassGraphModule(classScanResult));
      install(new ConfigModule(classScanResult, tsunamiConfig));
      install(new CliOptionsModule(classScanResult, "TsunamiCli", args));
      install(new SystemUtcClockModule());
      install(new CommandExecutorModule());
      install(new HttpClientModule.Builder().build());
      install(new GoogleCloudStorageArchiverModule());
      install(new ScanResultsArchiverModule());
      install(new PluginExecutionModule());
      install(new PluginLoadingModule(classScanResult));
    }
  }

  public static void main(String[] args) {
    Stopwatch stopwatch = Stopwatch.createStarted();

    TsunamiConfig tsunamiConfig = loadConfig();

    try (ScanResult scanResult =
        new ClassGraph()
            .enableAllInfo()
            .blacklistPackages("com.google.tsunami.plugin.testing")
            .scan()) {
      logger.atInfo().log("Full classpath scan took %s", stopwatch);

      Injector injector =
          Guice.createInjector(new TsunamiCliModule(scanResult, args, tsunamiConfig));

      injector.getInstance(TsunamiCli.class).run();

      logger.atInfo().log("Full Tsunami scan took %s.", stopwatch.stop());
    } catch (Throwable e) {
      logger.atSevere().withCause(e).log("Exiting due to workflow execution exceptions.");
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      System.exit(1);
    }
  }

  private static TsunamiConfig loadConfig() {
    try (ScanResult scanResult = new ClassGraph().enableAllInfo().scan()) {
      ConfigLoader configLoader;
      Optional<String> loaderClass = TsunamiConfig.getSystemProperty("tsunami.config.loader");
      if (loaderClass.isPresent()
          && scanResult.getAllClassesAsMap().containsKey(loaderClass.get())) {
        configLoader =
            scanResult
                .getClassInfo(loaderClass.get())
                .loadClass(ConfigLoader.class)
                .getConstructor()
                .newInstance();
      } else {
        configLoader = new YamlConfigLoader();
      }

      return configLoader.loadConfig();
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Error loading config.", e);
    }
  }
}
