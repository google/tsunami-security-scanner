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

import static com.google.common.truth.Truth.assertThat;
import static com.google.tsunami.common.data.NetworkEndpointUtils.forIpAndPort;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.FakeTicker;
import com.google.inject.Guice;
import com.google.tsunami.plugin.PluginManager;
import com.google.tsunami.plugin.PluginManager.PluginMatchingResult;
import com.google.tsunami.plugin.PortScanner;
import com.google.tsunami.plugin.VulnDetector;
import com.google.tsunami.plugin.testing.FakePortScannerBootstrapModule;
import com.google.tsunami.plugin.testing.FakeServiceFingerprinterBootstrapModule;
import com.google.tsunami.plugin.testing.FakeVulnDetectorBootstrapModule;
import com.google.tsunami.plugin.testing.FakeVulnDetectorBootstrapModule2;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.ReconnaissanceReport;
import com.google.tsunami.proto.ScanFinding;
import com.google.tsunami.proto.ScanResults;
import com.google.tsunami.proto.TransportProtocol;
import java.time.Duration;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ExecutionTracer}. */
@RunWith(JUnit4.class)
public final class ExecutionTracerTest {
  private static final Duration TICK_DURATION = Duration.ofSeconds(1);
  private final FakeTicker ticker = new FakeTicker().setAutoIncrementStep(TICK_DURATION);
  private final Stopwatch portScanningTimer = Stopwatch.createUnstarted(ticker);
  private final Stopwatch serviceFingerprintingTimer = Stopwatch.createUnstarted(ticker);
  private final Stopwatch vulnerabilityDetectingTimer = Stopwatch.createUnstarted(ticker);

  @Inject PluginManager pluginManager;

  @Before
  public void setUp() {
    Guice.createInjector(
            new FakePortScannerBootstrapModule(),
            new FakePortScannerBootstrapModule(),
            new FakeServiceFingerprinterBootstrapModule(),
            new FakeVulnDetectorBootstrapModule(),
            new FakeVulnDetectorBootstrapModule2())
        .injectMembers(this);
  }

  @Test
  public void startWorkflow_always_createExecutionTracerAtStartStage() {
    assertThat(ExecutionTracer.startWorkflow().getCurrentExecutionStage())
        .isEqualTo(ExecutionStage.START);
  }

  @Test
  public void startPortScanning_always_setsExecutionTracerToCorrectState() {
    ExecutionTracer executionTracer =
        new ExecutionTracer(
            portScanningTimer, serviceFingerprintingTimer, vulnerabilityDetectingTimer);
    ImmutableList<PluginMatchingResult<PortScanner>> installedPortScanners =
        pluginManager.getPortScanners();

    executionTracer.startPortScanning(installedPortScanners);

    assertThat(portScanningTimer.isRunning()).isTrue();
    assertThat(serviceFingerprintingTimer.isRunning()).isFalse();
    assertThat(vulnerabilityDetectingTimer.isRunning()).isFalse();
    assertThat(executionTracer.getCurrentExecutionStage()).isEqualTo(ExecutionStage.PORT_SCANNING);
    assertThat(executionTracer.getSelectedPortScanners())
        .containsExactlyElementsIn(installedPortScanners);
    assertThat(executionTracer.getPortScanningStageRuntime()).isEqualTo(TICK_DURATION);
  }

  @Test
  public void startPortScanning_whenExecutionStageIsNotStart_throwsException() {
    ExecutionTracer executionTracer =
        new ExecutionTracer(
            portScanningTimer, serviceFingerprintingTimer, vulnerabilityDetectingTimer);
    ImmutableList<PluginMatchingResult<PortScanner>> installedPortScanners =
        pluginManager.getPortScanners();
    executionTracer.startPortScanning(installedPortScanners);

    assertThrows(
        IllegalStateException.class,
        () -> executionTracer.startPortScanning(installedPortScanners));
  }

  @Test
  public void startServiceFingerprinting_always_setsExecutionTracerToCorrectState() {
    ExecutionTracer executionTracer =
        new ExecutionTracer(
            portScanningTimer, serviceFingerprintingTimer, vulnerabilityDetectingTimer);
    executionTracer.startPortScanning(pluginManager.getPortScanners());

    // TODO(b/145315535): fill service fingerprinter data when plugin manager has the interface.
    executionTracer.startServiceFingerprinting(ImmutableList.of());

    assertThat(portScanningTimer.isRunning()).isFalse();
    assertThat(serviceFingerprintingTimer.isRunning()).isTrue();
    assertThat(vulnerabilityDetectingTimer.isRunning()).isFalse();
    assertThat(executionTracer.getCurrentExecutionStage())
        .isEqualTo(ExecutionStage.SERVICE_FINGERPRINTING);
    assertThat(executionTracer.getSelectedServiceFingerprinters()).isEmpty();
    assertThat(executionTracer.getServiceFingerprintingStageRuntime()).isEqualTo(TICK_DURATION);
  }

  @Test
  public void startServiceFingerprinting_whenStageNotPortScanning_throwsException() {
    ExecutionTracer executionTracer =
        new ExecutionTracer(
            portScanningTimer, serviceFingerprintingTimer, vulnerabilityDetectingTimer);

    assertThrows(
        IllegalStateException.class,
        () -> executionTracer.startServiceFingerprinting(ImmutableList.of()));
  }

  @Test
  public void startServiceFingerprinting_whenPortScanningTimerNotRunning_throwsException() {
    ExecutionTracer executionTracer =
        new ExecutionTracer(
            portScanningTimer, serviceFingerprintingTimer, vulnerabilityDetectingTimer);
    executionTracer.startPortScanning(pluginManager.getPortScanners());
    portScanningTimer.stop();

    assertThrows(
        IllegalStateException.class,
        () -> executionTracer.startServiceFingerprinting(ImmutableList.of()));
  }

  @Test
  public void startVulnerabilityDetecting_always_setsExecutionTracerToCorrectState() {
    ExecutionTracer executionTracer =
        new ExecutionTracer(
            portScanningTimer, serviceFingerprintingTimer, vulnerabilityDetectingTimer);
    executionTracer.startPortScanning(pluginManager.getPortScanners());
    executionTracer.startServiceFingerprinting(ImmutableList.of());

    ImmutableList<PluginMatchingResult<VulnDetector>> installedVulnDetectors =
        pluginManager.getVulnDetectors(ReconnaissanceReport.getDefaultInstance());
    executionTracer.startVulnerabilityDetecting(installedVulnDetectors);

    assertThat(portScanningTimer.isRunning()).isFalse();
    assertThat(serviceFingerprintingTimer.isRunning()).isFalse();
    assertThat(vulnerabilityDetectingTimer.isRunning()).isTrue();
    assertThat(executionTracer.getCurrentExecutionStage())
        .isEqualTo(ExecutionStage.VULNERABILITY_DETECTING);
    assertThat(executionTracer.getSelectedVulnDetectors())
        .containsExactlyElementsIn(installedVulnDetectors);
    assertThat(executionTracer.getVulnerabilityDetectingStageRuntime()).isEqualTo(TICK_DURATION);
  }

  @Test
  public void startVulnerabilityDetecting_whenStageNotFingerprinting_throwsException() {
    ExecutionTracer executionTracer =
        new ExecutionTracer(
            portScanningTimer, serviceFingerprintingTimer, vulnerabilityDetectingTimer);

    assertThrows(
        IllegalStateException.class,
        () ->
            executionTracer.startVulnerabilityDetecting(
                pluginManager.getVulnDetectors(ReconnaissanceReport.getDefaultInstance())));
  }

  @Test
  public void startVulnerabilityDetecting_whenFingerprintingTimerNotRunning_throwsException() {
    ExecutionTracer executionTracer =
        new ExecutionTracer(
            portScanningTimer, serviceFingerprintingTimer, vulnerabilityDetectingTimer);
    executionTracer.startPortScanning(pluginManager.getPortScanners());
    executionTracer.startServiceFingerprinting(ImmutableList.of());
    serviceFingerprintingTimer.stop();

    assertThrows(
        IllegalStateException.class,
        () ->
            executionTracer.startVulnerabilityDetecting(
                pluginManager.getVulnDetectors(ReconnaissanceReport.getDefaultInstance())));
  }

  @Test
  public void setDone_always_setsExecutionTracerToCorrectState() {
    ExecutionTracer executionTracer =
        new ExecutionTracer(
            portScanningTimer, serviceFingerprintingTimer, vulnerabilityDetectingTimer);
    executionTracer.startPortScanning(pluginManager.getPortScanners());
    executionTracer.startServiceFingerprinting(ImmutableList.of());
    executionTracer.startVulnerabilityDetecting(
        pluginManager.getVulnDetectors(ReconnaissanceReport.getDefaultInstance()));

    executionTracer.setDone();

    assertThat(portScanningTimer.isRunning()).isFalse();
    assertThat(serviceFingerprintingTimer.isRunning()).isFalse();
    assertThat(vulnerabilityDetectingTimer.isRunning()).isFalse();
    assertThat(executionTracer.isDone()).isTrue();
  }

  @Test
  public void setDone_whenStageNotVulnerabilityDetecting_throwsException() {
    ExecutionTracer executionTracer =
        new ExecutionTracer(
            portScanningTimer, serviceFingerprintingTimer, vulnerabilityDetectingTimer);

    assertThrows(IllegalStateException.class, executionTracer::setDone);
  }

  @Test
  public void setDone_whenVulnerabilityDetectingTimerNotRunning_throwsException() {
    ExecutionTracer executionTracer =
        new ExecutionTracer(
            portScanningTimer, serviceFingerprintingTimer, vulnerabilityDetectingTimer);
    executionTracer.startPortScanning(pluginManager.getPortScanners());
    executionTracer.startServiceFingerprinting(ImmutableList.of());
    executionTracer.startVulnerabilityDetecting(
        pluginManager.getVulnDetectors(ReconnaissanceReport.getDefaultInstance()));
    vulnerabilityDetectingTimer.stop();

    assertThrows(IllegalStateException.class, executionTracer::setDone);
  }

  @Test
  public void forceDone_always_stopsAllTimerAndSetDoneStatus() {
    ExecutionTracer executionTracer =
        new ExecutionTracer(
            portScanningTimer, serviceFingerprintingTimer, vulnerabilityDetectingTimer);
    executionTracer.startPortScanning(pluginManager.getPortScanners());

    executionTracer.forceDone();

    assertThat(portScanningTimer.isRunning()).isFalse();
    assertThat(serviceFingerprintingTimer.isRunning()).isFalse();
    assertThat(vulnerabilityDetectingTimer.isRunning()).isFalse();
    assertThat(executionTracer.isDone()).isTrue();
  }

  @Test
  public void buildLoggableExecutionTrace_always_generatesExpectedMessage() {
    ReconnaissanceReport reconnaissanceReport =
        ReconnaissanceReport.newBuilder()
            .addNetworkServices(
                NetworkService.newBuilder()
                    .setNetworkEndpoint(forIpAndPort("1.1.1.1", 80))
                    .setTransportProtocol(TransportProtocol.TCP)
                    .setServiceName("http")
                    .build())
            .addNetworkServices(
                NetworkService.newBuilder()
                    .setNetworkEndpoint(forIpAndPort("1.1.1.1", 22))
                    .setTransportProtocol(TransportProtocol.TCP)
                    .setServiceName("ssh")
                    .build())
            .build();
    ExecutionTracer executionTracer =
        new ExecutionTracer(
            portScanningTimer, serviceFingerprintingTimer, vulnerabilityDetectingTimer);
    executionTracer.startPortScanning(pluginManager.getPortScanners());
    executionTracer.startServiceFingerprinting(ImmutableList.of());
    executionTracer.startVulnerabilityDetecting(
        pluginManager.getVulnDetectors(reconnaissanceReport));
    executionTracer.setDone();

    String message =
        executionTracer.buildLoggableExecutionTrace(
            ScanResults.newBuilder().addScanFindings(ScanFinding.getDefaultInstance()).build());

    assertThat(message)
        .isEqualTo(
            "Tsunami scanning workflow traces:\n"
                + "  Port scanning phase (1.000 s) with 1 plugin(s):\n"
                + "    /fake/PORT_SCAN/FakePortScanner/v0.1\n"
                + "  Service fingerprinting phase (1.000 s) with 0 plugin(s):\n"
                + "    \n"
                + "  Vuln detection phase (1.000 s) with 2 plugin(s):\n"
                + "    /fake/VULN_DETECTION/FakeVulnDetector/v0.1 was selected for the following"
                + " services: http (TCP, port 80), ssh (TCP, port 22)\n"
                + "    /fake/VULN_DETECTION/FakeVulnDetector2/v0.1 was selected for the following"
                + " services: http (TCP, port 80), ssh (TCP, port 22)\n"
                + "  # of detected vulnerability: 1.");
  }
}
