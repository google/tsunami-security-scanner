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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.tsunami.plugin.PluginManager.PluginMatchingResult;
import com.google.tsunami.plugin.PortScanner;
import com.google.tsunami.plugin.ServiceFingerprinter;
import com.google.tsunami.plugin.TsunamiPlugin;
import com.google.tsunami.plugin.VulnDetector;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.ScanResults;
import java.time.Duration;

/** Traces the execution of the default scanning workflow. */
final class ExecutionTracer {
  private static final Joiner PLUGIN_INFO_JOINER = Joiner.on("\n    ");
  private static final Joiner NETWORK_SERVICE_JOINER = Joiner.on(", ");

  private final Stopwatch portScanningTimer;
  private final Stopwatch serviceFingerprintingTimer;
  private final Stopwatch vulnerabilityDetectingTimer;

  private ExecutionStage currentExecutionStage;
  private ImmutableList<PluginMatchingResult<PortScanner>> selectedPortScanners;
  private ImmutableList<PluginMatchingResult<ServiceFingerprinter>> selectedServiceFingerprinters;
  private ImmutableList<PluginMatchingResult<VulnDetector>> selectedVulnDetectors;

  private ExecutionTracer() {
    this(Stopwatch.createUnstarted(), Stopwatch.createUnstarted(), Stopwatch.createUnstarted());
  }

  @VisibleForTesting
  ExecutionTracer(
      Stopwatch portScanningTimer,
      Stopwatch serviceFingerprintingTimer,
      Stopwatch vulnerabilityDetectingTimer) {
    this.currentExecutionStage = ExecutionStage.START;
    this.portScanningTimer = checkNotNull(portScanningTimer);
    this.serviceFingerprintingTimer = checkNotNull(serviceFingerprintingTimer);
    this.vulnerabilityDetectingTimer = checkNotNull(vulnerabilityDetectingTimer);
  }

  static ExecutionTracer startWorkflow() {
    return new ExecutionTracer();
  }

  ExecutionStage getCurrentExecutionStage() {
    return this.currentExecutionStage;
  }

  void startPortScanning(ImmutableList<PluginMatchingResult<PortScanner>> selectedPortScanners) {
    checkState(currentExecutionStage.equals(ExecutionStage.START));

    this.portScanningTimer.start();
    this.currentExecutionStage = ExecutionStage.PORT_SCANNING;
    this.selectedPortScanners = checkNotNull(selectedPortScanners);
  }

  void startServiceFingerprinting(
      ImmutableList<PluginMatchingResult<ServiceFingerprinter>> selectedServiceFingerprinters) {
    checkState(currentExecutionStage.equals(ExecutionStage.PORT_SCANNING));
    checkState(portScanningTimer.isRunning());

    this.portScanningTimer.stop();
    this.serviceFingerprintingTimer.start();
    this.currentExecutionStage = ExecutionStage.SERVICE_FINGERPRINTING;
    this.selectedServiceFingerprinters = checkNotNull(selectedServiceFingerprinters);
  }

  void startVulnerabilityDetecting(
      ImmutableList<PluginMatchingResult<VulnDetector>> selectedVulnDetectors) {
    checkState(currentExecutionStage.equals(ExecutionStage.SERVICE_FINGERPRINTING));
    checkState(serviceFingerprintingTimer.isRunning());

    this.serviceFingerprintingTimer.stop();
    this.vulnerabilityDetectingTimer.start();
    this.currentExecutionStage = ExecutionStage.VULNERABILITY_DETECTING;
    this.selectedVulnDetectors = checkNotNull(selectedVulnDetectors);
  }

  void setDone() {
    checkState(currentExecutionStage.equals(ExecutionStage.VULNERABILITY_DETECTING));
    checkState(!portScanningTimer.isRunning());
    checkState(!serviceFingerprintingTimer.isRunning());
    checkState(vulnerabilityDetectingTimer.isRunning());

    this.vulnerabilityDetectingTimer.stop();
    this.currentExecutionStage = ExecutionStage.DONE;
  }

  void forceDone() {
    if (portScanningTimer.isRunning()) {
      portScanningTimer.stop();
    }
    if (serviceFingerprintingTimer.isRunning()) {
      serviceFingerprintingTimer.stop();
    }
    if (vulnerabilityDetectingTimer.isRunning()) {
      vulnerabilityDetectingTimer.stop();
    }

    this.currentExecutionStage = ExecutionStage.DONE;
  }

  boolean isDone() {
    return currentExecutionStage.equals(ExecutionStage.DONE);
  }

  Duration getPortScanningStageRuntime() {
    return portScanningTimer.elapsed();
  }

  Duration getServiceFingerprintingStageRuntime() {
    return serviceFingerprintingTimer.elapsed();
  }

  Duration getVulnerabilityDetectingStageRuntime() {
    return vulnerabilityDetectingTimer.elapsed();
  }

  ImmutableList<PluginMatchingResult<PortScanner>> getSelectedPortScanners() {
    return selectedPortScanners;
  }

  ImmutableList<PluginMatchingResult<ServiceFingerprinter>> getSelectedServiceFingerprinters() {
    return selectedServiceFingerprinters;
  }

  ImmutableList<PluginMatchingResult<VulnDetector>> getSelectedVulnDetectors() {
    return selectedVulnDetectors;
  }

  String buildLoggableExecutionTrace(ScanResults scanResults) {
    checkState(isDone());
    return new StringBuilder("Tsunami scanning workflow traces:\n")
        // Port scanning phase.
        .append(
            String.format(
                "  Port scanning phase (%s) with %d plugin(s):\n    ",
                portScanningTimer, selectedPortScanners.size()))
        .append(
            PLUGIN_INFO_JOINER.join(
                selectedPortScanners.stream()
                    .map(ExecutionTracer::buildPluginInfoMessage)
                    .collect(toImmutableList())))
        // Service fingerprinting phase.
        .append(
            String.format(
                "\n  Service fingerprinting phase (%s) with %d plugin(s):\n    ",
                serviceFingerprintingTimer, selectedServiceFingerprinters.size()))
        .append(
            PLUGIN_INFO_JOINER.join(
                selectedServiceFingerprinters.stream()
                    .map(ExecutionTracer::buildPluginInfoMessage)
                    .collect(toImmutableList())))
        // Vuln detection phase.
        .append(
            String.format(
                "\n  Vuln detection phase (%s) with %d plugin(s):\n    ",
                vulnerabilityDetectingTimer, selectedVulnDetectors.size()))
        .append(
            PLUGIN_INFO_JOINER.join(
                selectedVulnDetectors.stream()
                    .map(ExecutionTracer::buildPluginInfoMessage)
                    .collect(toImmutableList())))
        .append(
            String.format(
                "\n  # of detected vulnerability: %d.", scanResults.getScanFindingsCount()))
        .toString();
  }

  static <T extends TsunamiPlugin> String buildPluginInfoMessage(
      PluginMatchingResult<T> pluginMatchingResult) {
    // TODO(b/145315535): add execution time once plugin execution logic is moved to a dedicated
    // plugin executor.
    StringBuilder pluginInfoBuilder = new StringBuilder(pluginMatchingResult.pluginId());
    if (!pluginMatchingResult.matchedServices().isEmpty()) {
      pluginInfoBuilder
          .append(" was selected for the following services: ")
          .append(
              NETWORK_SERVICE_JOINER.join(
                  pluginMatchingResult.matchedServices().stream()
                      .map(ExecutionTracer::formatNetworkService)
                      .collect(toImmutableList())));
    }
    return pluginInfoBuilder.toString();
  }

  static String formatNetworkService(NetworkService networkService) {
    return String.format(
        "%s (%s, port %d)",
        networkService.getServiceName(),
        networkService.getTransportProtocol(),
        networkService.getNetworkEndpoint().getPort().getPortNumber());
  }
}
