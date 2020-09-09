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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.tsunami.common.data.NetworkEndpointUtils;
import com.google.tsunami.plugin.PluginManager.PluginMatchingResult;
import com.google.tsunami.plugin.annotations.ForServiceName;
import com.google.tsunami.plugin.annotations.ForSoftware;
import com.google.tsunami.plugin.annotations.ForWebService;
import com.google.tsunami.plugin.annotations.PluginInfo;
import com.google.tsunami.plugin.testing.FakePortScanner;
import com.google.tsunami.plugin.testing.FakePortScanner2;
import com.google.tsunami.plugin.testing.FakePortScannerBootstrapModule;
import com.google.tsunami.plugin.testing.FakePortScannerBootstrapModule2;
import com.google.tsunami.plugin.testing.FakeServiceFingerprinterBootstrapModule;
import com.google.tsunami.plugin.testing.FakeVulnDetector;
import com.google.tsunami.plugin.testing.FakeVulnDetector2;
import com.google.tsunami.plugin.testing.FakeVulnDetectorBootstrapModule;
import com.google.tsunami.plugin.testing.FakeVulnDetectorBootstrapModule2;
import com.google.tsunami.proto.DetectionReportList;
import com.google.tsunami.proto.FingerprintingReport;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.ReconnaissanceReport;
import com.google.tsunami.proto.Software;
import com.google.tsunami.proto.TargetInfo;
import com.google.tsunami.proto.TransportProtocol;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link PluginManager}. */
@RunWith(JUnit4.class)
public class PluginManagerTest {

  @Test
  public void getPortScanners_whenMultiplePortScannersInstalled_returnsAllPortScanners() {
    PluginManager pluginManager =
        Guice.createInjector(
                new FakePortScannerBootstrapModule(),
                new FakePortScannerBootstrapModule2(),
                new FakeServiceFingerprinterBootstrapModule(),
                new FakeVulnDetectorBootstrapModule())
            .getInstance(PluginManager.class);

    ImmutableList<PluginMatchingResult<PortScanner>> portScanners = pluginManager.getPortScanners();

    assertThat(
            portScanners.stream()
                .map(pluginMatchingResult -> pluginMatchingResult.tsunamiPlugin().getClass()))
        .containsExactly(FakePortScanner.class, FakePortScanner2.class);
  }

  @Test
  public void getPortScanners_whenNoPortScannersInstalled_returnsEmptyList() {
    PluginManager pluginManager =
        Guice.createInjector(
                new FakeServiceFingerprinterBootstrapModule(),
                new FakeVulnDetectorBootstrapModule())
            .getInstance(PluginManager.class);

    assertThat(pluginManager.getPortScanners()).isEmpty();
  }

  @Test
  public void getPortScanner_whenMultiplePortScannersInstalled_returnsTheFirstMatchedPortScanner() {
    PluginManager pluginManager =
        Guice.createInjector(
                new FakePortScannerBootstrapModule(),
                new FakePortScannerBootstrapModule2(),
                new FakeServiceFingerprinterBootstrapModule(),
                new FakeVulnDetectorBootstrapModule())
            .getInstance(PluginManager.class);

    ImmutableList<PluginMatchingResult<PortScanner>> allPortScanners =
        pluginManager.getPortScanners();
    Optional<PluginMatchingResult<PortScanner>> firstMatchedPortScanner =
        pluginManager.getPortScanner();

    assertThat(firstMatchedPortScanner).isPresent();
    assertThat(firstMatchedPortScanner.get().pluginDefinition())
        .isEqualTo(allPortScanners.get(0).pluginDefinition());
    assertThat(firstMatchedPortScanner.get().tsunamiPlugin().getClass())
        .isEqualTo(allPortScanners.get(0).tsunamiPlugin().getClass());
  }

  @Test
  public void getPortScanner_whenNoPortScannersInstalled_returnsEmptyOptional() {
    PluginManager pluginManager =
        Guice.createInjector(
                new FakeServiceFingerprinterBootstrapModule(),
                new FakeVulnDetectorBootstrapModule())
            .getInstance(PluginManager.class);

    assertThat(pluginManager.getPortScanner()).isEmpty();
  }

  @Test
  public void getServiceFingerprinter_whenFingerprinterNotAnnotated_returnsEmpty() {
    NetworkService httpService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("http")
            .build();
    PluginManager pluginManager =
        Guice.createInjector(
                new FakePortScannerBootstrapModule(), NoAnnotationFingerprinter.getModule())
            .getInstance(PluginManager.class);

    Optional<PluginMatchingResult<ServiceFingerprinter>> fingerprinter =
        pluginManager.getServiceFingerprinter(httpService);

    assertThat(fingerprinter).isEmpty();
  }

  @Test
  public void getServiceFingerprinter_whenFingerprinterHasMatch_returnsMatch() {
    NetworkService httpService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("http")
            .build();
    PluginManager pluginManager =
        Guice.createInjector(
                new FakePortScannerBootstrapModule(), new FakeServiceFingerprinterBootstrapModule())
            .getInstance(PluginManager.class);

    Optional<PluginMatchingResult<ServiceFingerprinter>> fingerprinter =
        pluginManager.getServiceFingerprinter(httpService);

    assertThat(fingerprinter).isPresent();
    assertThat(fingerprinter.get().matchedServices()).containsExactly(httpService);
  }

  @Test
  public void getServiceFingerprinter_whenNoFingerprinterMatches_returnsEmpty() {
    NetworkService httpsService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("https")
            .build();
    PluginManager pluginManager =
        Guice.createInjector(
                new FakePortScannerBootstrapModule(), new FakeServiceFingerprinterBootstrapModule())
            .getInstance(PluginManager.class);

    Optional<PluginMatchingResult<ServiceFingerprinter>> fingerprinter =
        pluginManager.getServiceFingerprinter(httpsService);

    assertThat(fingerprinter).isEmpty();
  }

  @Test
  public void getServiceFingerprinter_whenForWebServiceAnnotationAndWebService_returnsMatch() {
    NetworkService httpProxyService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("http-proxy")
            .build();
    NetworkService httpsService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("https")
            .build();
    PluginManager pluginManager =
        Guice.createInjector(new FakePortScannerBootstrapModule(), FakeWebFingerprinter.getModule())
            .getInstance(PluginManager.class);

    Optional<PluginMatchingResult<ServiceFingerprinter>> fingerprinter =
        pluginManager.getServiceFingerprinter(httpsService);
    assertThat(fingerprinter).isPresent();
    assertThat(fingerprinter.get().matchedServices()).containsExactly(httpsService);

    fingerprinter = pluginManager.getServiceFingerprinter(httpProxyService);
    assertThat(fingerprinter).isPresent();
    assertThat(fingerprinter.get().matchedServices()).containsExactly(httpProxyService);
  }

  @Test
  public void getServiceFingerprinter_whenForWebServiceAnnotationAndNonWebService_returnsEmpty() {
    NetworkService sshService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("ssh")
            .build();
    NetworkService rdpService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("rdp")
            .build();
    PluginManager pluginManager =
        Guice.createInjector(new FakePortScannerBootstrapModule(), FakeWebFingerprinter.getModule())
            .getInstance(PluginManager.class);

    assertThat(pluginManager.getServiceFingerprinter(sshService)).isEmpty();
    assertThat(pluginManager.getServiceFingerprinter(rdpService)).isEmpty();
  }

  @Test
  public void
      getVulnDetectors_whenMultipleVulnDetectorsInstalledNoFiltering_returnsAllVulnDetector() {
    NetworkService fakeNetworkService1 =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("http")
            .build();
    NetworkService fakeNetworkService2 =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 443))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("https")
            .build();
    ReconnaissanceReport fakeReconnaissanceReport =
        ReconnaissanceReport.newBuilder()
            .setTargetInfo(TargetInfo.getDefaultInstance())
            .addNetworkServices(fakeNetworkService1)
            .addNetworkServices(fakeNetworkService2)
            .build();
    PluginManager pluginManager =
        Guice.createInjector(
                new FakePortScannerBootstrapModule(),
                new FakeServiceFingerprinterBootstrapModule(),
                new FakeVulnDetectorBootstrapModule(),
                new FakeVulnDetectorBootstrapModule2())
            .getInstance(PluginManager.class);

    ImmutableList<PluginMatchingResult<VulnDetector>> vulnDetectors =
        pluginManager.getVulnDetectors(fakeReconnaissanceReport);

    assertThat(
            vulnDetectors.stream()
                .map(pluginMatchingResult -> pluginMatchingResult.tsunamiPlugin().getClass()))
        .containsExactly(FakeVulnDetector.class, FakeVulnDetector2.class);
    assertThat(vulnDetectors.stream().map(PluginMatchingResult::matchedServices))
        .containsExactly(
            fakeReconnaissanceReport.getNetworkServicesList(),
            fakeReconnaissanceReport.getNetworkServicesList());
  }

  @Test
  public void getVulnDetectors_whenServiceNameFilterHasMatchingService_returnsMatchedService() {
    NetworkService httpService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("http")
            .build();
    NetworkService httpsService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 443))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("https")
            .build();
    NetworkService noNameService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 12345))
            .setTransportProtocol(TransportProtocol.TCP)
            .build();
    ReconnaissanceReport fakeReconnaissanceReport =
        ReconnaissanceReport.newBuilder()
            .setTargetInfo(TargetInfo.getDefaultInstance())
            .addNetworkServices(httpService)
            .addNetworkServices(httpsService)
            .addNetworkServices(noNameService)
            .build();
    PluginManager pluginManager =
        Guice.createInjector(
                new FakePortScannerBootstrapModule(),
                new FakeServiceFingerprinterBootstrapModule(),
                FakeServiceNameFilteringDetector.getModule())
            .getInstance(PluginManager.class);

    ImmutableList<PluginMatchingResult<VulnDetector>> vulnDetectors =
        pluginManager.getVulnDetectors(fakeReconnaissanceReport);

    assertThat(vulnDetectors).hasSize(1);
    assertThat(vulnDetectors.get(0).tsunamiPlugin().getClass())
        .isEqualTo(FakeServiceNameFilteringDetector.class);
    assertThat(vulnDetectors.get(0).matchedServices()).containsExactly(httpService, noNameService);
  }

  @Test
  public void getVulnDetectors_whenServiceNameFilterHasNoMatchingService_returnsEmpty() {
    NetworkService httpsService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 443))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("https")
            .build();
    ReconnaissanceReport fakeReconnaissanceReport =
        ReconnaissanceReport.newBuilder()
            .setTargetInfo(TargetInfo.getDefaultInstance())
            .addNetworkServices(httpsService)
            .build();
    PluginManager pluginManager =
        Guice.createInjector(
                new FakePortScannerBootstrapModule(),
                new FakeServiceFingerprinterBootstrapModule(),
                FakeServiceNameFilteringDetector.getModule())
            .getInstance(PluginManager.class);

    ImmutableList<PluginMatchingResult<VulnDetector>> vulnDetectors =
        pluginManager.getVulnDetectors(fakeReconnaissanceReport);

    assertThat(vulnDetectors).isEmpty();
  }

  @Test
  public void getVulnDetectors_whenSoftwareFilterHasMatchingService_returnsMatchedService() {
    NetworkService wordPressService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("http")
            .setSoftware(Software.newBuilder().setName("WordPress"))
            .build();
    NetworkService jenkinsService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 443))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("https")
            .setSoftware(Software.newBuilder().setName("Jenkins"))
            .build();
    NetworkService noNameService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 12345))
            .setTransportProtocol(TransportProtocol.TCP)
            .build();
    ReconnaissanceReport fakeReconnaissanceReport =
        ReconnaissanceReport.newBuilder()
            .setTargetInfo(TargetInfo.getDefaultInstance())
            .addNetworkServices(wordPressService)
            .addNetworkServices(jenkinsService)
            .addNetworkServices(noNameService)
            .build();
    PluginManager pluginManager =
        Guice.createInjector(
                new FakePortScannerBootstrapModule(),
                new FakeServiceFingerprinterBootstrapModule(),
                FakeSoftwareFilteringDetector.getModule())
            .getInstance(PluginManager.class);

    ImmutableList<PluginMatchingResult<VulnDetector>> vulnDetectors =
        pluginManager.getVulnDetectors(fakeReconnaissanceReport);

    assertThat(vulnDetectors).hasSize(1);
    assertThat(vulnDetectors.get(0).tsunamiPlugin().getClass())
        .isEqualTo(FakeSoftwareFilteringDetector.class);
    assertThat(vulnDetectors.get(0).matchedServices())
        .containsExactly(jenkinsService, noNameService);
  }

  @Test
  public void getVulnDetectors_whenSoftwareFilterHasNoMatchingService_returnsEmpty() {
    NetworkService wordPressService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 443))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("https")
            .setSoftware(Software.newBuilder().setName("WordPress"))
            .build();
    ReconnaissanceReport fakeReconnaissanceReport =
        ReconnaissanceReport.newBuilder()
            .setTargetInfo(TargetInfo.getDefaultInstance())
            .addNetworkServices(wordPressService)
            .build();
    PluginManager pluginManager =
        Guice.createInjector(
                new FakePortScannerBootstrapModule(),
                new FakeServiceFingerprinterBootstrapModule(),
                FakeSoftwareFilteringDetector.getModule())
            .getInstance(PluginManager.class);

    ImmutableList<PluginMatchingResult<VulnDetector>> vulnDetectors =
        pluginManager.getVulnDetectors(fakeReconnaissanceReport);

    assertThat(vulnDetectors).isEmpty();
  }

  @Test
  public void getVulnDetectors_whenNoVulnDetectorsInstalled_returnsEmptyList() {
    NetworkService fakeNetworkService1 =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("http")
            .build();
    NetworkService fakeNetworkService2 =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 443))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("https")
            .build();
    ReconnaissanceReport fakeReconnaissanceReport =
        ReconnaissanceReport.newBuilder()
            .setTargetInfo(TargetInfo.getDefaultInstance())
            .addNetworkServices(fakeNetworkService1)
            .addNetworkServices(fakeNetworkService2)
            .build();
    PluginManager pluginManager =
        Guice.createInjector(
                new FakePortScannerBootstrapModule(), new FakeServiceFingerprinterBootstrapModule())
            .getInstance(PluginManager.class);

    assertThat(pluginManager.getVulnDetectors(fakeReconnaissanceReport)).isEmpty();
  }

  @PluginInfo(
      type = PluginType.SERVICE_FINGERPRINT,
      name = "NoAnnotationFingerprinter",
      version = "v0.1",
      description = "A fake ServiceFingerprinter.",
      author = "fake",
      bootstrapModule = NoAnnotationFingerprinter.Module.class)
  private static final class NoAnnotationFingerprinter implements ServiceFingerprinter {
    @Override
    public FingerprintingReport fingerprint(TargetInfo targetInfo, NetworkService networkService) {
      return null;
    }

    static Module getModule() {
      return new Module();
    }

    private static final class Module extends PluginBootstrapModule {
      @Override
      protected void configurePlugin() {
        registerPlugin(NoAnnotationFingerprinter.class);
      }
    }
  }

  @PluginInfo(
      type = PluginType.SERVICE_FINGERPRINT,
      name = "FakeWebFingerprinter",
      version = "v0.1",
      description = "A fake ServiceFingerprinter for web services.",
      author = "fake",
      bootstrapModule = NoAnnotationFingerprinter.Module.class)
  @ForWebService
  private static final class FakeWebFingerprinter implements ServiceFingerprinter {
    @Override
    public FingerprintingReport fingerprint(TargetInfo targetInfo, NetworkService networkService) {
      return null;
    }

    static Module getModule() {
      return new Module();
    }

    private static final class Module extends PluginBootstrapModule {
      @Override
      protected void configurePlugin() {
        registerPlugin(FakeWebFingerprinter.class);
      }
    }
  }

  @PluginInfo(
      type = PluginType.VULN_DETECTION,
      name = "FakeServiceNameFilteringDetector",
      version = "v0.1",
      description = "A fake VulnDetector.",
      author = "fake",
      bootstrapModule = FakeServiceNameFilteringDetector.Module.class)
  @ForServiceName("http")
  private static final class FakeServiceNameFilteringDetector implements VulnDetector {
    @Override
    public DetectionReportList detect(
        TargetInfo targetInfo, ImmutableList<NetworkService> matchedServices) {
      return null;
    }

    static Module getModule() {
      return new Module();
    }

    private static final class Module extends PluginBootstrapModule {
      @Override
      protected void configurePlugin() {
        registerPlugin(FakeServiceNameFilteringDetector.class);
      }
    }
  }

  @PluginInfo(
      type = PluginType.VULN_DETECTION,
      name = "FakeSoftwareFilteringDetector",
      version = "v0.1",
      description = "A fake VulnDetector.",
      author = "fake",
      bootstrapModule = FakeSoftwareFilteringDetector.Module.class)
  @ForSoftware(name = "Jenkins")
  private static final class FakeSoftwareFilteringDetector implements VulnDetector {
    @Override
    public DetectionReportList detect(
        TargetInfo targetInfo, ImmutableList<NetworkService> matchedServices) {
      return null;
    }

    static Module getModule() {
      return new Module();
    }

    private static final class Module extends PluginBootstrapModule {
      @Override
      protected void configurePlugin() {
        registerPlugin(FakeSoftwareFilteringDetector.class);
      }
    }
  }
}
