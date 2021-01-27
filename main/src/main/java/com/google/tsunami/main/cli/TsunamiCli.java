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
import static com.google.tsunami.common.data.NetworkEndpointUtils.forIpAndHostname;

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
import com.google.tsunami.main.cli.option.MainCliOptions;
import com.google.tsunami.plugin.PluginExecutionModule;
import com.google.tsunami.plugin.PluginLoadingModule;
import com.google.tsunami.proto.ScanResults;
import com.google.tsunami.proto.ScanStatus;
import com.google.tsunami.proto.ScanTarget;
import com.google.tsunami.workflow.DefaultScanningWorkflow;
import com.google.tsunami.workflow.ScanningWorkflowException;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

/** Command line interface for the Tsunami Security Scanner. */
public final class TsunamiCli {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private final DefaultScanningWorkflow scanningWorkflow;
  private final ScanResultsArchiver scanResultsArchiver;
  private final MainCliOptions mainCliOptions;

  @Inject
  TsunamiCli(
      DefaultScanningWorkflow scanningWorkflow,
      ScanResultsArchiver scanResultsArchiver,
      MainCliOptions mainCliOptions) {
    this.scanningWorkflow = checkNotNull(scanningWorkflow);
    this.scanResultsArchiver = checkNotNull(scanResultsArchiver);
    this.mainCliOptions = checkNotNull(mainCliOptions);
  }

  public boolean run()
      throws ExecutionException, InterruptedException, ScanningWorkflowException, IOException {
    String logId = (mainCliOptions.logId == null) ? "" : (mainCliOptions.logId + ": ");
    // TODO(b/171405612): Find a way to print the log ID at every log line.
    logger.atInfo().log("%sTsunamiCli starting...", logId);
    ScanResults scanResults = scanningWorkflow.run(buildScanTarget());

    logger.atInfo().log("Tsunami scan finished, saving results.");
    saveResults(scanResults);

    if (hasSuccessfulResults(scanResults)) {
      logger.atInfo().log("TsunamiCli finished...");
      return true;
    } else {
      logger.atInfo().log(
          "Tsunami scan has failed status, message = %s.", scanResults.getStatusMessage());
      return false;
    }
  }

  private static boolean hasSuccessfulResults(ScanResults scanResults) {
    return scanResults.getScanStatus().equals(ScanStatus.SUCCEEDED)
        || scanResults.getScanStatus().equals(ScanStatus.PARTIALLY_SUCCEEDED);
  }

  private ScanTarget buildScanTarget() {
    ScanTarget.Builder scanTargetBuilder = ScanTarget.newBuilder();

    String ip = null;
    if (mainCliOptions.ipV4Target != null) {
      ip = mainCliOptions.ipV4Target;
    } else if (mainCliOptions.ipV6Target != null) {
      ip = mainCliOptions.ipV6Target;
    }
    if (ip != null && mainCliOptions.hostnameTarget != null) {
      scanTargetBuilder.setNetworkEndpoint(forIpAndHostname(ip, mainCliOptions.hostnameTarget));
    } else if (ip != null) {
      scanTargetBuilder.setNetworkEndpoint(forIp(ip));
    } else {
      scanTargetBuilder.setNetworkEndpoint(forHostname(mainCliOptions.hostnameTarget));
    }

    return scanTargetBuilder.build();
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

      // Exit with non-zero code if scan failed.
      if (!injector.getInstance(TsunamiCli.class).run()) {
        System.exit(1);
      }

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
