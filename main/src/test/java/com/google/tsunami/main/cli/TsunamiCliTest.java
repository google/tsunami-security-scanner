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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.tsunami.common.cli.CliOptionsModule;
import com.google.tsunami.common.config.ConfigModule;
import com.google.tsunami.common.config.TsunamiConfig;
import com.google.tsunami.common.data.NetworkEndpointUtils;
import com.google.tsunami.common.time.testing.FakeUtcClockModule;
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
import com.google.tsunami.proto.DetectionReport;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.ReconnaissanceReport;
import com.google.tsunami.proto.ScanFinding;
import com.google.tsunami.proto.ScanResults;
import com.google.tsunami.proto.ScanStatus;
import com.google.tsunami.proto.TargetInfo;
import com.google.tsunami.workflow.ScanningWorkflowException;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/** Tests for {@link TsunamiCli}. */
@RunWith(JUnit4.class)
public final class TsunamiCliTest {
  private static final String IP_TARGET = "127.0.0.1";
  private static final String HOSTNAME_TARGET = "localhost";

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock ScanResultsArchiver scanResultsArchiver;

  @Captor ArgumentCaptor<ScanResults> scanResultsCaptor;

  @Inject private TsunamiCli tsunamiCli;

  private boolean runCli(ImmutableMap<String, Object> rawConfigData, String... args)
      throws InterruptedException, ExecutionException, ScanningWorkflowException, IOException {
    try (ScanResult scanResult = new ClassGraph().enableAllInfo().scan()) {
      Guice.createInjector(
              new AbstractModule() {
                @Override
                protected void configure() {
                  bind(ScanResultsArchiver.class).toInstance(scanResultsArchiver);
                  install(new ConfigModule(scanResult, TsunamiConfig.fromYamlData(rawConfigData)));
                  install(new CliOptionsModule(scanResult, "TsunamiCliTest", args));
                  install(new FakeUtcClockModule());
                  install(new FakePluginExecutionModule());
                  install(new FakePortScannerBootstrapModule());
                  install(new FakePortScannerBootstrapModule2());
                  install(new FakeServiceFingerprinterBootstrapModule());
                  install(new FakeVulnDetectorBootstrapModule());
                  install(new FakeVulnDetectorBootstrapModule2());
                }
              })
          .injectMembers(this);
      return tsunamiCli.run();
    }
  }

  @Test
  public void run_whenIpTarget_generatesAndArchivesCorrectResult()
      throws InterruptedException, ExecutionException, ScanningWorkflowException, IOException {
    NetworkService expectedNetworkService =
        FakeServiceFingerprinter.addWebServiceContext(
            FakePortScanner.getFakeNetworkService(NetworkEndpointUtils.forIp(IP_TARGET)));

    boolean scanSucceeded = runCli(ImmutableMap.of(), "--ip-v4-target=" + IP_TARGET);

    assertThat(scanSucceeded).isTrue();
    TargetInfo targetInfo =
        TargetInfo.newBuilder().addNetworkEndpoints(NetworkEndpointUtils.forIp(IP_TARGET)).build();
    verify(scanResultsArchiver, times(1)).archive(scanResultsCaptor.capture());
    ScanResults storedScanResult = scanResultsCaptor.getValue();
    assertThat(storedScanResult.getScanStatus()).isEqualTo(ScanStatus.SUCCEEDED);
    assertThat(storedScanResult.getScanFindingsList())
        .containsExactlyElementsIn(
            Stream.of(
                    FakeVulnDetector.getFakeDetectionReport(targetInfo, expectedNetworkService),
                    FakeVulnDetector2.getFakeDetectionReport(targetInfo, expectedNetworkService))
                .map(TsunamiCliTest::buildScanFindingFromDetectionReport)
                .toArray());
    assertThat(storedScanResult.getReconnaissanceReport())
        .isEqualTo(
            ReconnaissanceReport.newBuilder()
                .setTargetInfo(
                    TargetInfo.newBuilder()
                        .addNetworkEndpoints(NetworkEndpointUtils.forIp(IP_TARGET)))
                .addNetworkServices(
                    FakeServiceFingerprinter.addWebServiceContext(
                        FakePortScanner.getFakeNetworkService(
                            NetworkEndpointUtils.forIp(IP_TARGET))))
                .build());
  }

  @Test
  public void run_whenHostnameTarget_generatesAndArchivesCorrectResult()
      throws InterruptedException, ExecutionException, ScanningWorkflowException, IOException {
    NetworkService expectedNetworkService =
        FakeServiceFingerprinter.addWebServiceContext(
            FakePortScanner.getFakeNetworkService(
                NetworkEndpointUtils.forHostname(HOSTNAME_TARGET)));

    boolean scanSucceeded = runCli(ImmutableMap.of(), "--hostname-target=" + HOSTNAME_TARGET);

    assertThat(scanSucceeded).isTrue();

    TargetInfo targetInfo =
        TargetInfo.newBuilder()
            .addNetworkEndpoints(NetworkEndpointUtils.forHostname(HOSTNAME_TARGET))
            .build();
    verify(scanResultsArchiver, times(1)).archive(scanResultsCaptor.capture());
    ScanResults storedScanResult = scanResultsCaptor.getValue();
    assertThat(storedScanResult.getScanStatus()).isEqualTo(ScanStatus.SUCCEEDED);
    assertThat(storedScanResult.getScanFindingsList())
        .containsExactlyElementsIn(
            Stream.of(
                    FakeVulnDetector.getFakeDetectionReport(targetInfo, expectedNetworkService),
                    FakeVulnDetector2.getFakeDetectionReport(targetInfo, expectedNetworkService))
                .map(TsunamiCliTest::buildScanFindingFromDetectionReport)
                .toArray());
    assertThat(storedScanResult.getReconnaissanceReport())
        .isEqualTo(
            ReconnaissanceReport.newBuilder()
                .setTargetInfo(
                    TargetInfo.newBuilder()
                        .addNetworkEndpoints(NetworkEndpointUtils.forHostname(HOSTNAME_TARGET)))
                .addNetworkServices(
                    FakeServiceFingerprinter.addWebServiceContext(
                        FakePortScanner.getFakeNetworkService(
                            NetworkEndpointUtils.forHostname(HOSTNAME_TARGET))))
                .build());
  }

  @Test
  public void run_whenIpAndHostnameTarget_generatesCorrectResult()
      throws InterruptedException, ExecutionException, IOException {

    boolean scanSucceeded =
        runCli(
            ImmutableMap.of(),
            "--ip-v4-target=" + IP_TARGET,
            "--hostname-target=" + HOSTNAME_TARGET);

    assertThat(scanSucceeded).isTrue();

    verify(scanResultsArchiver, times(1)).archive(scanResultsCaptor.capture());
    ScanResults storedScanResult = scanResultsCaptor.getValue();
    assertThat(storedScanResult.getScanStatus()).isEqualTo(ScanStatus.SUCCEEDED);
    assertThat(storedScanResult.getReconnaissanceReport())
        .isEqualTo(
            ReconnaissanceReport.newBuilder()
                .setTargetInfo(
                    TargetInfo.newBuilder()
                        .addNetworkEndpoints(
                            NetworkEndpointUtils.forIpAndHostname(IP_TARGET, HOSTNAME_TARGET)))
                .addNetworkServices(
                    FakeServiceFingerprinter.addWebServiceContext(
                        FakePortScanner.getFakeNetworkService(
                            NetworkEndpointUtils.forIpAndHostname(IP_TARGET, HOSTNAME_TARGET))))
                .build());
  }

  @Test
  public void run_whenScanFailed_generatesFailedScanResults()
      throws InterruptedException, ExecutionException, IOException {

    try (ScanResult scanResult = new ClassGraph().enableAllInfo().scan()) {
      Guice.createInjector(
              new AbstractModule() {
                @Override
                protected void configure() {
                  bind(ScanResultsArchiver.class).toInstance(scanResultsArchiver);
                  install(
                      new ConfigModule(scanResult, TsunamiConfig.fromYamlData(ImmutableMap.of())));
                  install(
                      new CliOptionsModule(
                          scanResult,
                          "TsunamiCliTest",
                          new String[] {
                            "--ip-v4-target=" + IP_TARGET, "--hostname-target=" + HOSTNAME_TARGET
                          }));
                  install(new FakeUtcClockModule());
                  install(new FakePluginExecutionModule());
                  install(new FakePortScannerBootstrapModule());
                  install(new FailedVulnDetectorBootstrapModule());
                }
              })
          .injectMembers(this);

      boolean scanSucceeded = tsunamiCli.run();

      assertThat(scanSucceeded).isFalse();

      verify(scanResultsArchiver, times(1)).archive(scanResultsCaptor.capture());
      ScanResults storedScanResult = scanResultsCaptor.getValue();
      assertThat(storedScanResult.getScanStatus()).isEqualTo(ScanStatus.FAILED);
      assertThat(storedScanResult.getStatusMessage()).isEqualTo("All VulnDetectors failed.");
    }
  }

  private static ScanFinding buildScanFindingFromDetectionReport(DetectionReport detectionReport) {
    return ScanFinding.newBuilder()
        .setTargetInfo(detectionReport.getTargetInfo())
        .setNetworkService(detectionReport.getNetworkService())
        .setVulnerability(detectionReport.getVulnerability())
        .build();
  }
}
