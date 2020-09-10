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
import static com.google.common.truth.Truth8.assertThat;
import static com.google.tsunami.common.data.NetworkEndpointUtils.forIp;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.tsunami.common.time.testing.FakeUtcClockModule;
import com.google.tsunami.plugin.testing.FailedPortScannerBootstrapModule;
import com.google.tsunami.plugin.testing.FailedServiceFingerprinterBootstrapModule;
import com.google.tsunami.plugin.testing.FailedVulnDetectorBootstrapModule;
import com.google.tsunami.plugin.testing.FakePluginExecutionModule;
import com.google.tsunami.plugin.testing.FakePortScanner;
import com.google.tsunami.plugin.testing.FakePortScannerBootstrapModule;
import com.google.tsunami.plugin.testing.FakePortScannerBootstrapModule2;
import com.google.tsunami.plugin.testing.FakeServiceFingerprinter;
import com.google.tsunami.plugin.testing.FakeServiceFingerprinterBootstrapModule;
import com.google.tsunami.plugin.testing.FakeVulnDetector;
import com.google.tsunami.plugin.testing.FakeVulnDetector2;
import com.google.tsunami.plugin.testing.FakeVulnDetectorBootstrapModule;
import com.google.tsunami.plugin.testing.FakeVulnDetectorBootstrapModule2;
import com.google.tsunami.proto.ScanResults;
import com.google.tsunami.proto.ScanStatus;
import com.google.tsunami.proto.ScanTarget;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link DefaultScanningWorkflow}. */
@RunWith(JUnit4.class)
public final class DefaultScanningWorkflowTest {
  @Inject private DefaultScanningWorkflow scanningWorkflow;

  @Before
  public void setUp() {
    Guice.createInjector(
            new FakeUtcClockModule(),
            new FakePluginExecutionModule(),
            new FakePortScannerBootstrapModule(),
            new FakePortScannerBootstrapModule2(),
            new FakeServiceFingerprinterBootstrapModule(),
            new FakeVulnDetectorBootstrapModule(),
            new FakeVulnDetectorBootstrapModule2())
        .injectMembers(this);
  }

  @Test
  public void run_whenAllRequiredPluginsInstalled_executesScanningWorkflow()
      throws InterruptedException, ExecutionException {
    ScanResults scanResults = scanningWorkflow.run(buildScanTarget());
    ExecutionTracer executionTracer = scanningWorkflow.getExecutionTracer();

    assertThat(scanResults.getScanStatus()).isEqualTo(ScanStatus.SUCCEEDED);
    assertThat(executionTracer.isDone()).isTrue();
    assertThat(executionTracer.getSelectedPortScanners()).hasSize(1);
    assertThat(executionTracer.getSelectedPortScanners().get(0).tsunamiPlugin().getClass())
        .isEqualTo(FakePortScanner.class);
    assertThat(executionTracer.getSelectedServiceFingerprinters()).hasSize(1);
    assertThat(executionTracer.getSelectedServiceFingerprinters().get(0).tsunamiPlugin().getClass())
        .isEqualTo(FakeServiceFingerprinter.class);
    assertThat(
            executionTracer.getSelectedVulnDetectors().stream()
                .map(selectedVulnDetector -> selectedVulnDetector.tsunamiPlugin().getClass()))
        .containsExactlyElementsIn(
            ImmutableList.of(FakeVulnDetector.class, FakeVulnDetector2.class));
  }

  // TODO(b/145315535): add default output for the fake plugins and test the output of the workflow.

  @Test
  public void run_whenNoPortScannerInstalled_returnsFailedScanResult()
      throws ExecutionException, InterruptedException {
    Injector injector =
        Guice.createInjector(
            new FakeUtcClockModule(),
            new FakePluginExecutionModule(),
            new FakeServiceFingerprinterBootstrapModule(),
            new FakeVulnDetectorBootstrapModule());
    scanningWorkflow = injector.getInstance(DefaultScanningWorkflow.class);

    ScanResults scanResults = scanningWorkflow.run(buildScanTarget());

    assertThat(scanResults.getScanStatus()).isEqualTo(ScanStatus.FAILED);
    assertThat(scanResults.getStatusMessage())
        .contains("At least one PortScanner plugin is required");
    assertThat(scanResults.getScanFindingsList()).isEmpty();
  }

  @Test
  public void run_whenNoFingerprinterInstalled_executesScanningWorkflow()
      throws InterruptedException, ExecutionException {
    Injector injector =
        Guice.createInjector(
            new FakeUtcClockModule(),
            new FakePluginExecutionModule(),
            new FakePortScannerBootstrapModule(),
            new FakeVulnDetectorBootstrapModule(),
            new FakeVulnDetectorBootstrapModule2());
    scanningWorkflow = injector.getInstance(DefaultScanningWorkflow.class);

    ScanResults scanResults = scanningWorkflow.run(buildScanTarget());
    ExecutionTracer executionTracer = scanningWorkflow.getExecutionTracer();

    assertThat(scanResults.getScanStatus()).isEqualTo(ScanStatus.SUCCEEDED);
    assertThat(executionTracer.isDone()).isTrue();
    assertThat(executionTracer.getSelectedPortScanners()).hasSize(1);
    assertThat(executionTracer.getSelectedPortScanners().get(0).tsunamiPlugin().getClass())
        .isEqualTo(FakePortScanner.class);
    assertThat(executionTracer.getSelectedServiceFingerprinters()).isEmpty();
    assertThat(
            executionTracer.getSelectedVulnDetectors().stream()
                .map(selectedVulnDetector -> selectedVulnDetector.tsunamiPlugin().getClass()))
        .containsExactlyElementsIn(
            ImmutableList.of(FakeVulnDetector.class, FakeVulnDetector2.class));
  }

  @Test
  public void run_whenPortScannerFailed_returnsFailedScanResult()
      throws ExecutionException, InterruptedException {
    Injector injector =
        Guice.createInjector(
            new FakeUtcClockModule(),
            new FakePluginExecutionModule(),
            new FailedPortScannerBootstrapModule(),
            new FakeServiceFingerprinterBootstrapModule(),
            new FakeVulnDetectorBootstrapModule());
    scanningWorkflow = injector.getInstance(DefaultScanningWorkflow.class);

    ScanResults scanResults = scanningWorkflow.run(buildScanTarget());

    assertThat(scanResults.getScanStatus()).isEqualTo(ScanStatus.FAILED);
    assertThat(scanResults.getStatusMessage())
        .contains("Plugin execution error on '/fake/PORT_SCAN/FailedPortScanner/v0.1'");
    assertThat(scanResults.getScanFindingsList()).isEmpty();
  }

  @Test
  public void run_whenServiceFingerprinterFailed_reusesNetworkServicesFromPortScan()
      throws ExecutionException, InterruptedException {
    Injector injector =
        Guice.createInjector(
            new FakeUtcClockModule(),
            new FakePluginExecutionModule(),
            new FakePortScannerBootstrapModule(),
            new FailedServiceFingerprinterBootstrapModule(),
            new FakeVulnDetectorBootstrapModule(),
            new FakeVulnDetectorBootstrapModule2());
    scanningWorkflow = injector.getInstance(DefaultScanningWorkflow.class);

    ScanResults scanResults = scanningWorkflow.run(buildScanTarget());

    assertThat(scanResults.getScanStatus()).isEqualTo(ScanStatus.SUCCEEDED);
    assertThat(scanResults.getReconnaissanceReport().getNetworkServicesList())
        .containsExactly(
            FakePortScanner.getFakeNetworkService(buildScanTarget().getNetworkEndpoint()));
  }

  @Test
  public void run_whenServiceFingerprinterSucceeded_fillsReconnaissanceReportWithFingerprintResult()
      throws ExecutionException, InterruptedException {
    Injector injector =
        Guice.createInjector(
            new FakeUtcClockModule(),
            new FakePluginExecutionModule(),
            new FakePortScannerBootstrapModule(),
            new FakeServiceFingerprinterBootstrapModule(),
            new FakeVulnDetectorBootstrapModule(),
            new FakeVulnDetectorBootstrapModule2());
    scanningWorkflow = injector.getInstance(DefaultScanningWorkflow.class);

    ScanResults scanResults = scanningWorkflow.run(buildScanTarget());

    assertThat(scanResults.getScanStatus()).isEqualTo(ScanStatus.SUCCEEDED);
    assertThat(scanResults.getReconnaissanceReport().getNetworkServicesList())
        .containsExactly(
            FakeServiceFingerprinter.addWebServiceContext(
                FakePortScanner.getFakeNetworkService(buildScanTarget().getNetworkEndpoint())));
  }

  @Test
  public void run_whenSomeVulnDetectorFailed_returnsPartiallySucceededScanResult()
      throws ExecutionException, InterruptedException {
    Injector injector =
        Guice.createInjector(
            new FakeUtcClockModule(),
            new FakePluginExecutionModule(),
            new FakePortScannerBootstrapModule(),
            new FakeServiceFingerprinterBootstrapModule(),
            new FakeVulnDetectorBootstrapModule(),
            new FailedVulnDetectorBootstrapModule());
    scanningWorkflow = injector.getInstance(DefaultScanningWorkflow.class);

    ScanResults scanResults = scanningWorkflow.run(buildScanTarget());

    assertThat(scanResults.getScanStatus()).isEqualTo(ScanStatus.PARTIALLY_SUCCEEDED);
    assertThat(scanResults.getStatusMessage())
        .contains("Failed plugins:\n/fake/VULN_DETECTION/FailedVulnDetector/v0.1");
    assertThat(scanResults.getScanFindingsList()).hasSize(1);
  }

  @Test
  public void run_whenAllVulnDetectorFailed_returnsFailedScanResult()
      throws ExecutionException, InterruptedException {
    Injector injector =
        Guice.createInjector(
            new FakeUtcClockModule(),
            new FakePluginExecutionModule(),
            new FakePortScannerBootstrapModule(),
            new FakeServiceFingerprinterBootstrapModule(),
            new FailedVulnDetectorBootstrapModule());
    scanningWorkflow = injector.getInstance(DefaultScanningWorkflow.class);

    ScanResults scanResults = scanningWorkflow.run(buildScanTarget());

    assertThat(scanResults.getScanStatus()).isEqualTo(ScanStatus.FAILED);
    assertThat(scanResults.getStatusMessage()).contains("All VulnDetectors failed");
    assertThat(scanResults.getScanFindingsList()).isEmpty();
  }

  @Test
  public void run_whenNullScanTarget_throwsNullPointerException() {
    Injector injector =
        Guice.createInjector(
            new FakeUtcClockModule(),
            new FakePluginExecutionModule(),
            new FakeServiceFingerprinterBootstrapModule(),
            new FakeVulnDetectorBootstrapModule());
    scanningWorkflow = injector.getInstance(DefaultScanningWorkflow.class);

    assertThrows(NullPointerException.class, () -> scanningWorkflow.run(null));
  }

  private static ScanTarget buildScanTarget() {
    return ScanTarget.newBuilder().setNetworkEndpoint(forIp("1.2.3.4")).build();
  }
}
