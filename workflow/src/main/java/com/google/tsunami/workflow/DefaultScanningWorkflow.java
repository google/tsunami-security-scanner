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
package com.google.tsunami.workflow;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.flogger.GoogleLogger;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import com.google.tsunami.common.TsunamiException;
import com.google.tsunami.common.time.UtcClock;
import com.google.tsunami.plugin.PluginExecutionException;
import com.google.tsunami.plugin.PluginExecutionResult;
import com.google.tsunami.plugin.PluginExecutor;
import com.google.tsunami.plugin.PluginExecutor.PluginExecutorConfig;
import com.google.tsunami.plugin.PluginManager;
import com.google.tsunami.plugin.PluginManager.PluginMatchingResult;
import com.google.tsunami.plugin.PortScanner;
import com.google.tsunami.plugin.ServiceFingerprinter;
import com.google.tsunami.plugin.VulnDetector;
import com.google.tsunami.proto.DetectionReport;
import com.google.tsunami.proto.DetectionReportList;
import com.google.tsunami.proto.DetectionStatus;
import com.google.tsunami.proto.FingerprintingReport;
import com.google.tsunami.proto.FullDetectionReports;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.PortScanningReport;
import com.google.tsunami.proto.ReconnaissanceReport;
import com.google.tsunami.proto.ScanFinding;
import com.google.tsunami.proto.ScanResults;
import com.google.tsunami.proto.ScanStatus;
import com.google.tsunami.proto.ScanTarget;
import com.google.tsunami.proto.TargetInfo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Default scanning workflow for Tsunami.
 *
 * <p>This workflow is intended to be invoked by Tsunami's command line tool. One {@link
 * DefaultScanningWorkflow} object will be created for one target IP or hostname, and scans for
 * different IP / hostname target will be executed in isolated processes.
 *
 * <p>Tsunami performs the network scanning in the following steps:
 *
 * <ol>
 *   <li>Port scanning using NMap.
 *   <li>If target serves any web application, web fingerprinting to determine the exposed
 *       application.
 *   <li>Vulnerability detection by matching the identified network services to corresponding
 *       detectors.
 * </ol>
 */
// TODO(b/145315535): provide a cleaner API to avoid executing the same workflow on different scan
// target.
public final class DefaultScanningWorkflow {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private final PluginManager pluginManager;
  private final Clock clock;
  private final Provider<PluginExecutor> pluginExecutorProvider;

  private Instant scanStartTimestamp;
  private ExecutionTracer executionTracer;

  @Inject
  public DefaultScanningWorkflow(
      PluginManager pluginManager,
      @UtcClock Clock clock,
      Provider<PluginExecutor> pluginExecutorProvider) {
    this.pluginManager = checkNotNull(pluginManager);
    this.clock = checkNotNull(clock);
    this.pluginExecutorProvider = checkNotNull(pluginExecutorProvider);
  }

  public ExecutionTracer getExecutionTracer() {
    return executionTracer;
  }

  /**
   * Performs the scanning workflow in blocking manner.
   *
   * @param scanTarget the IP or hostname target to be scanned
   * @return The result of the scanning workflow.
   */
  public ScanResults run(ScanTarget scanTarget)
      throws ExecutionException, InterruptedException, ScanningWorkflowException {
    return runAsync(scanTarget).get();
  }

  /**
   * Performs the scanning workflow asynchronously.
   *
   * @param scanTarget the IP or hostname target to be scanned
   * @return A {@link ListenableFuture} over the result of the scanning workflow.
   */
  public ListenableFuture<ScanResults> runAsync(ScanTarget scanTarget)
      throws ScanningWorkflowException {
    checkNotNull(scanTarget);
    scanStartTimestamp = Instant.now(clock);
    executionTracer = ExecutionTracer.startWorkflow();
    logger.atInfo().log("Staring Tsunami scanning workflow.");
    return FluentFuture.from(scanPorts(scanTarget))
        .transformAsync(this::fingerprintNetworkServices, directExecutor())
        .transformAsync(this::detectVulnerabilities, directExecutor())
        // Unfortunately FluentFuture doesn't support future peeking.
        .transform(
            scanResults -> {
              logger.atInfo().log(executionTracer.buildLoggableExecutionTrace(scanResults));
              return scanResults;
            },
            directExecutor())
        // Execution errors are handled and reported back in the ScanResults.
        .catching(PluginExecutionException.class, this::onExecutionError, directExecutor())
        .catching(ScanningWorkflowException.class, this::onExecutionError, directExecutor());
  }

  private ScanResults onExecutionError(TsunamiException exception) {
    logger.atSevere().withCause(exception).log("Tsunami scan failed, aborting workflow!!!");
    return buildScanResultForFailure(exception);
  }

  private ListenableFuture<PortScanningReport> scanPorts(ScanTarget scanTarget)
      throws ScanningWorkflowException {
    Optional<PluginMatchingResult<PortScanner>> matchedPortScanner = pluginManager.getPortScanner();
    if (!matchedPortScanner.isPresent()) {
      return immediateFailedFuture(
          new ScanningWorkflowException("At least one PortScanner plugin is required"));
    }

    PluginExecutorConfig<PortScanningReport> executorConfig =
        PluginExecutorConfig.<PortScanningReport>builder()
            .setMatchedPlugin(matchedPortScanner.get())
            .setPluginExecutionLogic(
                () -> matchedPortScanner.get().tsunamiPlugin().scan(scanTarget))
            .build();
    executionTracer.startPortScanning(ImmutableList.of(matchedPortScanner.get()));
    logger.atInfo().log("Starting port scanning phase of the scanning workflow.");
    return FluentFuture.from(pluginExecutorProvider.get().executeAsync(executorConfig))
        .transformAsync(
            pluginExecutionResult ->
                pluginExecutionResult.isSucceeded()
                    ? immediateFuture(pluginExecutionResult.resultData().get())
                    : immediateFailedFuture(pluginExecutionResult.exception().get()),
            directExecutor());
  }

  private ListenableFuture<ReconnaissanceReport> fingerprintNetworkServices(
      PortScanningReport portScanningReport) {
    checkNotNull(portScanningReport);

    // For each network service, find matching fingerprinting plugin, otherwise directly add to
    // ReconnaissanceReport.
    TargetInfo targetInfo = portScanningReport.getTargetInfo();
    List<PluginMatchingResult<ServiceFingerprinter>> matchedFingerprinters = Lists.newArrayList();
    List<NetworkService> networkServicesToKeep = Lists.newArrayList();
    for (NetworkService networkService : portScanningReport.getNetworkServicesList()) {
      Optional<PluginMatchingResult<ServiceFingerprinter>> matchedFingerprinter =
          pluginManager.getServiceFingerprinter(networkService);
      if (matchedFingerprinter.isPresent()) {
        matchedFingerprinters.add(matchedFingerprinter.get());
      } else {
        networkServicesToKeep.add(networkService);
      }
    }

    executionTracer.startServiceFingerprinting(ImmutableList.copyOf(matchedFingerprinters));
    logger.atInfo().log(
        "Port scanning phase done, moving to service fingerprinting phase with '%d'"
            + " fingerprinter(s) selected.",
        matchedFingerprinters.size());

    // Execute matched fingerprinters asynchronously.
    ImmutableList<ListenableFuture<PluginExecutionResult<FingerprintingReport>>>
        fingerprintingResultFutures =
            matchedFingerprinters.stream()
                .map(fingerprinter -> buildFingerprinterExecutorConfig(targetInfo, fingerprinter))
                .map(executorConfig -> pluginExecutorProvider.get().executeAsync(executorConfig))
                .collect(toImmutableList());
    return FluentFuture.from(Futures.successfulAsList(fingerprintingResultFutures))
        .transform(
            executionResults ->
                ReconnaissanceReport.newBuilder()
                    .setTargetInfo(targetInfo)
                    .addAllNetworkServices(networkServicesToKeep)
                    .addAllNetworkServices(getFingerprintedServices(executionResults))
                    .build(),
            directExecutor());
  }

  private static PluginExecutorConfig<FingerprintingReport> buildFingerprinterExecutorConfig(
      TargetInfo targetInfo, PluginMatchingResult<ServiceFingerprinter> fingerprinter) {
    return PluginExecutorConfig.<FingerprintingReport>builder()
        .setMatchedPlugin(fingerprinter)
        .setPluginExecutionLogic(
            () ->
                fingerprinter
                    .tsunamiPlugin()
                    .fingerprint(targetInfo, fingerprinter.matchedServices().get(0)))
        .build();
  }

  @SuppressWarnings("unchecked")
  private static ImmutableList<NetworkService> getFingerprintedServices(
      Collection<PluginExecutionResult<FingerprintingReport>> executionResults) {
    return executionResults.stream()
        .flatMap(
            result ->
                result.isSucceeded()
                    ? result.resultData().get().getNetworkServicesList().stream()
                    : ((List<NetworkService>)
                            result.executorConfig().matchedPlugin().matchedServices())
                        .stream())
        .collect(toImmutableList());
  }

  private ListenableFuture<ScanResults> detectVulnerabilities(
      ReconnaissanceReport reconnaissanceReport) {
    checkNotNull(reconnaissanceReport);

    ImmutableList<PluginMatchingResult<VulnDetector>> matchedVulnDetectors =
        pluginManager.getVulnDetectors(reconnaissanceReport);
    executionTracer.startVulnerabilityDetecting(matchedVulnDetectors);
    logger.atInfo().log("Service fingerprinting phase done, moving to vuln detection phase.");

    ImmutableList<ListenableFuture<PluginExecutionResult<DetectionReportList>>>
        detectionResultFutures =
            matchedVulnDetectors.stream()
                .map(
                    matchedVulnDetector ->
                        PluginExecutorConfig.<DetectionReportList>builder()
                            .setMatchedPlugin(matchedVulnDetector)
                            .setPluginExecutionLogic(
                                () ->
                                    matchedVulnDetector
                                        .tsunamiPlugin()
                                        .detect(
                                            reconnaissanceReport.getTargetInfo(),
                                            matchedVulnDetector.matchedServices()))
                            .build())
                .map(
                    vulnDetectorExecutorConfig ->
                        pluginExecutorProvider.get().executeAsync(vulnDetectorExecutorConfig))
                .collect(toImmutableList());
    return FluentFuture.from(Futures.successfulAsList(detectionResultFutures))
        .transform(
            detectionResult -> generateScanResults(detectionResult, reconnaissanceReport),
            directExecutor());
  }

  private ScanResults generateScanResults(
      Collection<PluginExecutionResult<DetectionReportList>> detectionResults,
      ReconnaissanceReport reconnaissanceReport) {
    executionTracer.setDone();
    logger.atInfo().log("Tsunami scanning workflow done. Generating scan results.");

    ImmutableList<DetectionReport> succeededDetectionReports =
        detectionResults.stream()
            .filter(PluginExecutionResult::isSucceeded)
            .flatMap(
                detectionResult ->
                    detectionResult.resultData().get().getDetectionReportsList().stream())
            .collect(toImmutableList());
    ImmutableList<String> failedPlugins =
        detectionResults.stream()
            .filter(executionResult -> !executionResult.isSucceeded())
            .map(executionResult -> executionResult.executorConfig().matchedPlugin().pluginId())
            .collect(toImmutableList());

    ScanStatus scanStatus;
    String statusMessage = "";
    if (failedPlugins.isEmpty()) {
      scanStatus = ScanStatus.SUCCEEDED;
    } else if (failedPlugins.size() == detectionResults.size()) {
      scanStatus = ScanStatus.FAILED;
      statusMessage = "All VulnDetectors failed.";
    } else {
      scanStatus = ScanStatus.PARTIALLY_SUCCEEDED;
      statusMessage = "Failed plugins:\n" + Joiner.on("\n").join(failedPlugins);
    }

    return ScanResults.newBuilder()
        .setScanStatus(scanStatus)
        .setStatusMessage(statusMessage)
        .addAllScanFindings(
            succeededDetectionReports.stream()
                .filter(
                    detectionReport ->
                        detectionReport
                                .getDetectionStatus()
                                .equals(DetectionStatus.VULNERABILITY_VERIFIED)
                            || detectionReport
                                .getDetectionStatus()
                                .equals(DetectionStatus.VULNERABILITY_PRESENT))
                .map(
                    detectionReport ->
                        ScanFinding.newBuilder()
                            .setTargetInfo(detectionReport.getTargetInfo())
                            .setNetworkService(detectionReport.getNetworkService())
                            .setVulnerability(detectionReport.getVulnerability())
                            .build())
                .collect(toImmutableList()))
        .setScanStartTimestamp(Timestamps.fromMillis(scanStartTimestamp.toEpochMilli()))
        .setScanDuration(
            Durations.fromMillis(
                Duration.between(scanStartTimestamp, Instant.now(clock)).toMillis()))
        .setFullDetectionReports(
            FullDetectionReports.newBuilder().addAllDetectionReports(succeededDetectionReports))
        .setReconnaissanceReport(reconnaissanceReport)
        .build();
  }

  private ScanResults buildScanResultForFailure(TsunamiException exception) {
    executionTracer.forceDone();
    return ScanResults.newBuilder()
        .setScanStatus(ScanStatus.FAILED)
        .setStatusMessage(exception.getMessage())
        .setScanStartTimestamp(Timestamps.fromMillis(scanStartTimestamp.toEpochMilli()))
        .setScanDuration(
            Durations.fromMillis(
                Duration.between(scanStartTimestamp, Instant.now(clock)).toMillis()))
        .build();
  }
}
