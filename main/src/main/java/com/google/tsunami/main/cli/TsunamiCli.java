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
import static com.google.tsunami.common.data.NetworkServiceUtils.buildUriNetworkService;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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
import com.google.tsunami.common.net.http.HttpClientCliOptions;
import com.google.tsunami.common.net.http.HttpClientModule;
import com.google.tsunami.common.reflection.ClassGraphModule;
import com.google.tsunami.common.server.LanguageServerCommand;
import com.google.tsunami.common.time.SystemUtcClockModule;
import com.google.tsunami.main.cli.option.MainCliOptions;
import com.google.tsunami.main.cli.server.RemoteServerLoader;
import com.google.tsunami.main.cli.server.RemoteServerLoaderModule;
import com.google.tsunami.plugin.PluginExecutionModule;
import com.google.tsunami.plugin.PluginLoadingModule;
import com.google.tsunami.plugin.RemoteVulnDetectorLoadingModule;
import com.google.tsunami.plugin.payload.PayloadGeneratorModule;
import com.google.tsunami.proto.ScanResults;
import com.google.tsunami.proto.ScanStatus;
import com.google.tsunami.proto.ScanTarget;
import com.google.tsunami.workflow.AdvisoriesWorkflow;
import com.google.tsunami.workflow.DefaultScanningWorkflow;
import com.google.tsunami.workflow.ScanningWorkflowException;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

/** Command line interface for the Tsunami Security Scanner. */
public final class TsunamiCli {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private final DefaultScanningWorkflow scanningWorkflow;
  private final AdvisoriesWorkflow advisoriesWorkflow;
  private final ScanResultsArchiver scanResultsArchiver;
  private final MainCliOptions mainCliOptions;
  private final RemoteServerLoader remoteServerLoader;

  @Inject
  TsunamiCli(
      DefaultScanningWorkflow scanningWorkflow,
      AdvisoriesWorkflow advisoriesWorkflow,
      ScanResultsArchiver scanResultsArchiver,
      MainCliOptions mainCliOptions,
      RemoteServerLoader remoteServerLoader) {
    this.scanningWorkflow = checkNotNull(scanningWorkflow);
    this.advisoriesWorkflow = checkNotNull(advisoriesWorkflow);
    this.scanResultsArchiver = checkNotNull(scanResultsArchiver);
    this.mainCliOptions = checkNotNull(mainCliOptions);
    this.remoteServerLoader = checkNotNull(remoteServerLoader);
  }

  public boolean run()
      throws ExecutionException, InterruptedException, ScanningWorkflowException, IOException {
    String logId = mainCliOptions.getLogId();
    // TODO(b/171405612): Find a way to print the log ID at every log line.
    logger.atInfo().log("%sTsunamiCli starting...", logId);

    ImmutableList<Process> languageServerProcesses = remoteServerLoader.runServerProcesses();
    if (mainCliOptions.dumpAdvisoriesPath != null && !mainCliOptions.dumpAdvisoriesPath.isEmpty()) {
      logger.atInfo().log("No scan will be performed. Dumping advisories.");
      advisoriesWorkflow.run(mainCliOptions.dumpAdvisoriesPath);
      return true;
    }

    ScanResults scanResults = scanningWorkflow.run(buildScanTarget());
    languageServerProcesses.forEach(Process::destroy);

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
    } else if (mainCliOptions.uriTarget != null) {
      scanTargetBuilder.setNetworkService(buildUriNetworkService(mainCliOptions.uriTarget));
    } else {
      scanTargetBuilder.setNetworkEndpoint(forHostname(mainCliOptions.hostnameTarget));
    }

    return scanTargetBuilder.build();
  }

  private void saveResults(ScanResults scanResults) throws IOException {
    scanResultsArchiver.archive(scanResults);
  }

  private static final class TsunamiCliFirstStageModule extends AbstractModule {
    private final ScanResult classScanResult;
    private final String[] args;
    private final TsunamiConfig tsunamiConfig;

    TsunamiCliFirstStageModule(
        ScanResult classScanResult, String[] args, TsunamiConfig tsunamiConfig) {
      this.classScanResult = checkNotNull(classScanResult);
      this.args = checkNotNull(args);
      this.tsunamiConfig = checkNotNull(tsunamiConfig);
    }

    @Override
    protected void configure() {
      install(new ClassGraphModule(classScanResult));
      install(new ConfigModule(classScanResult, tsunamiConfig));
      install(new CliOptionsModule(classScanResult, "TsunamiCli", args));
    }
  }

  private static final class TsunamiCliModule extends AbstractModule {
    private final ScanResult classScanResult;
    private final Injector parentInjector;
    private final TsunamiConfig tsunamiConfig;

    TsunamiCliModule(
        Injector parentInjector, ScanResult classScanResult, TsunamiConfig tsunamiConfig) {
      this.classScanResult = checkNotNull(classScanResult);
      this.parentInjector = checkNotNull(parentInjector);
      this.tsunamiConfig = checkNotNull(tsunamiConfig);
    }

    @Override
    protected void configure() {
      MainCliOptions mco = parentInjector.getInstance(MainCliOptions.class);
      LanguageServerOptions lso = parentInjector.getInstance(LanguageServerOptions.class);
      HttpClientCliOptions hcco = parentInjector.getInstance(HttpClientCliOptions.class);
      ScanResultsArchiver.Options srao =
          parentInjector.getInstance(ScanResultsArchiver.Options.class);

      ImmutableList<LanguageServerCommand> commands = extractPluginServerArgs(mco, lso, hcco, srao);

      install(new SystemUtcClockModule());
      install(new CommandExecutorModule());
      install(new HttpClientModule.Builder().setLogId(mco.getLogId()).build());
      install(new GoogleCloudStorageArchiverModule());
      install(new ScanResultsArchiverModule());
      install(new PluginExecutionModule());
      install(new PluginLoadingModule(classScanResult));
      install(new PayloadGeneratorModule(new SecureRandom()));
      install(new RemoteServerLoaderModule(commands));
      install(new RemoteVulnDetectorLoadingModule(commands));
    }

    private ImmutableList<LanguageServerCommand> extractPluginServerArgs(
        MainCliOptions mco,
        LanguageServerOptions lso,
        HttpClientCliOptions hcco,
        ScanResultsArchiver.Options srao) {
      List<LanguageServerCommand> commands = Lists.newArrayList();
      Boolean trustAllSslCertCli = hcco.trustAllCertificates;
      var logId = mco.getLogId();
      var paths = lso.pluginServerFilenames;
      var ports = lso.pluginServerPorts;
      var rpcDeadline = lso.pluginServerRpcDeadlineSeconds;
      var remoteServerAddresses = lso.remotePluginServerAddress;
      var remoteServerPorts = lso.remotePluginServerPort;
      var remoteRpcDeadlines = lso.remotePluginServerRpcDeadlineSeconds;
      if (paths.isEmpty() && remoteServerAddresses.isEmpty()) {
        return ImmutableList.of();
      }

      Map<String, Object> callbackConfig = tsunamiConfig.readConfigValue("plugin.callbackserver");
      Map<String, Object> httpClientConfig = tsunamiConfig.readConfigValue("common.net.http");
      boolean trustAllSslCertConfig =
          (boolean) httpClientConfig.getOrDefault("trust_all_certificates", false);

      String lngOutputDir = extractOutputDir(srao);
      boolean lngTrustAllSslCertCli =
          trustAllSslCertCli != null ? trustAllSslCertCli.booleanValue() : trustAllSslCertConfig;
      Duration lngConnectDuration =
          Duration.ofSeconds((int) httpClientConfig.getOrDefault("connect_timeout_seconds", 0));
      String lngCallbackAddress = (String) callbackConfig.getOrDefault("callback_address", "");
      Integer lngCallbackPort = (Integer) callbackConfig.getOrDefault("callback_port", 0);
      String lngPollingUri = (String) callbackConfig.getOrDefault("polling_uri", "");

      for (int i = 0; i < paths.size(); ++i) {
        commands.add(
            LanguageServerCommand.create(
                paths.get(i),
                "",
                ports.get(i),
                logId,
                lngOutputDir,
                lngTrustAllSslCertCli,
                lngConnectDuration,
                lngCallbackAddress,
                lngCallbackPort,
                lngPollingUri,
                rpcDeadline.isEmpty() ? 0 : rpcDeadline.get(i)));
      }
      for (int i = 0; i < remoteServerAddresses.size(); ++i) {
        commands.add(
            LanguageServerCommand.create(
                "",
                remoteServerAddresses.get(i),
                remoteServerPorts.get(i).toString(),
                logId,
                lngOutputDir,
                lngTrustAllSslCertCli,
                lngConnectDuration,
                lngCallbackAddress,
                lngCallbackPort,
                lngPollingUri,
                remoteRpcDeadlines.isEmpty() ? 0 : remoteRpcDeadlines.get(i)));
      }
      return ImmutableList.copyOf(commands);
    }

    private String extractOutputDir(ScanResultsArchiver.Options sra) {
      if (!Strings.isNullOrEmpty(sra.localOutputFilename)) {
        return Path.of(sra.localOutputFilename).getParent().toString();
      }
      return "";
    }
  }

  public static int doMain(String[] args) {
    Stopwatch stopwatch = Stopwatch.createStarted();

    TsunamiConfig tsunamiConfig = loadConfig();

    try (ScanResult scanResult =
        new ClassGraph()
            .enableAllInfo()
            .blacklistPackages("com.google.tsunami.plugin.testing")
            .scan()) {
      logger.atInfo().log("Full classpath scan took %s", stopwatch);

      Injector firstStageInjector =
          Guice.createInjector(new TsunamiCliFirstStageModule(scanResult, args, tsunamiConfig));

      Injector injector =
          firstStageInjector.createChildInjector(
              new TsunamiCliModule(firstStageInjector, scanResult, tsunamiConfig));

      // Exit with non-zero code if scan failed.
      if (!injector.getInstance(TsunamiCli.class).run()) {
        return 1;
      }
      logger.atInfo().log("Full Tsunami scan took %s.", stopwatch.stop());
      return 0;
    } catch (Throwable e) {
      logger.atSevere().withCause(e).log("Exiting due to workflow execution exceptions.");
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return 1;
    }
  }

  public static void main(String[] args) {
    System.exit(doMain(args));
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
      throw new LinkageError("Error loading config.", e);
    }
  }
}
