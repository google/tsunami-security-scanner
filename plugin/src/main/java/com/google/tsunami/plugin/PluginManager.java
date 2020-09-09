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
import static com.google.tsunami.common.data.NetworkServiceUtils.isWebService;

import com.google.auto.value.AutoValue;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.ReconnaissanceReport;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Plugin manager manages all the registered plugins and provides map like interfaces for retrieving
 * plugins from the registry.
 */
public class PluginManager {
  private final Map<PluginDefinition, Provider<TsunamiPlugin>> tsunamiPlugins;

  @Inject
  PluginManager(Map<PluginDefinition, Provider<TsunamiPlugin>> tsunamiPlugins) {
    this.tsunamiPlugins = tsunamiPlugins;
  }

  /** Retrieves all {@link PortScanner} plugins. */
  public ImmutableList<PluginMatchingResult<PortScanner>> getPortScanners() {
    return tsunamiPlugins.entrySet().stream()
        .filter(entry -> entry.getKey().type().equals(PluginType.PORT_SCAN))
        .map(
            entry ->
                PluginMatchingResult.<PortScanner>builder()
                    .setPluginDefinition(entry.getKey())
                    .setTsunamiPlugin((PortScanner) entry.getValue().get())
                    .build())
        .collect(toImmutableList());
  }

  /** Retrieves the first {@link PortScanner} plugin if present. */
  public Optional<PluginMatchingResult<PortScanner>> getPortScanner() {
    ImmutableList<PluginMatchingResult<PortScanner>> allPortScanners = getPortScanners();

    return allPortScanners.isEmpty() ? Optional.empty() : Optional.of(allPortScanners.get(0));
  }

  /** Retrieves a {@link ServiceFingerprinter} plugin for the given {@link NetworkService}. */
  public Optional<PluginMatchingResult<ServiceFingerprinter>> getServiceFingerprinter(
      NetworkService networkService) {
    return tsunamiPlugins.entrySet().stream()
        .filter(entry -> entry.getKey().type().equals(PluginType.SERVICE_FINGERPRINT))
        .filter(entry -> hasMatchingServiceName(networkService, entry.getKey()))
        .map(
            entry ->
                PluginMatchingResult.<ServiceFingerprinter>builder()
                    .setPluginDefinition(entry.getKey())
                    .setTsunamiPlugin((ServiceFingerprinter) entry.getValue().get())
                    .addMatchedService(networkService)
                    .build())
        .findFirst();
  }

  public ImmutableList<PluginMatchingResult<VulnDetector>> getVulnDetectors(
      ReconnaissanceReport reconnaissanceReport) {
    return tsunamiPlugins.entrySet().stream()
        .filter(entry -> entry.getKey().type().equals(PluginType.VULN_DETECTION))
        .map(entry -> matchVulnDetectors(entry.getKey(), entry.getValue(), reconnaissanceReport))
        .flatMap(Streams::stream)
        .collect(toImmutableList());
  }

  private static Optional<PluginMatchingResult<VulnDetector>> matchVulnDetectors(
      PluginDefinition pluginDefinition,
      Provider<TsunamiPlugin> vulnDetectorProvider,
      ReconnaissanceReport reconnaissanceReport) {
    List<NetworkService> matchedNetworkServices;
    if (!pluginDefinition.targetServiceName().isPresent()
        && !pluginDefinition.targetSoftware().isPresent()
        && !pluginDefinition.isForWebService()) {
      // No filtering annotation applied, just match all network services from reconnaissance.
      matchedNetworkServices = reconnaissanceReport.getNetworkServicesList();
    } else {
      // At least one filtering annotation applied, check services to see if any one matches.
      matchedNetworkServices =
          reconnaissanceReport.getNetworkServicesList().stream()
              .filter(
                  networkService ->
                      hasMatchingServiceName(networkService, pluginDefinition)
                          || hasMatchingSoftware(networkService, pluginDefinition))
              .collect(toImmutableList());
    }

    return matchedNetworkServices.isEmpty()
        ? Optional.empty()
        : Optional.of(
            PluginMatchingResult.<VulnDetector>builder()
                .setPluginDefinition(pluginDefinition)
                .setTsunamiPlugin((VulnDetector) vulnDetectorProvider.get())
                .addAllMatchedServices(matchedNetworkServices)
                .build());
  }

  private static boolean hasMatchingServiceName(
      NetworkService networkService, PluginDefinition pluginDefinition) {
    String serviceName = networkService.getServiceName();
    boolean hasServiceNameMatch =
        pluginDefinition.targetServiceName().isPresent()
            && (serviceName.isEmpty()
                || Arrays.stream(pluginDefinition.targetServiceName().get().value())
                    .anyMatch(
                        targetServiceName ->
                            Ascii.equalsIgnoreCase(targetServiceName, serviceName)));
    boolean hasWebServiceMatch = pluginDefinition.isForWebService() && isWebService(networkService);
    return hasServiceNameMatch || hasWebServiceMatch;
  }

  private static boolean hasMatchingSoftware(
      NetworkService networkService, PluginDefinition pluginDefinition) {
    String softwareName = networkService.getSoftware().getName();
    return pluginDefinition.targetSoftware().isPresent()
        && (softwareName.isEmpty()
            || Ascii.equalsIgnoreCase(
                pluginDefinition.targetSoftware().get().name(), softwareName));
  }

  /** Matched {@link TsunamiPlugin}s based on certain criteria. */
  @AutoValue
  public abstract static class PluginMatchingResult<T extends TsunamiPlugin> {
    public abstract PluginDefinition pluginDefinition();
    public abstract T tsunamiPlugin();
    public abstract ImmutableList<NetworkService> matchedServices();

    public String pluginId() {
      return pluginDefinition().id();
    }

    public static <T extends TsunamiPlugin> Builder<T> builder() {
      return new AutoValue_PluginManager_PluginMatchingResult.Builder<T>();
    }

    /** Builder for {@link PluginMatchingResult}. */
    @AutoValue.Builder
    public abstract static class Builder<T extends TsunamiPlugin> {
      public abstract Builder<T> setPluginDefinition(PluginDefinition value);
      public abstract Builder<T> setTsunamiPlugin(T value);

      abstract ImmutableList.Builder<NetworkService> matchedServicesBuilder();
      public Builder<T> addMatchedService(NetworkService networkService) {
        matchedServicesBuilder().add(networkService);
        return this;
      }
      public Builder<T> addAllMatchedServices(Iterable<NetworkService> networkServices) {
        matchedServicesBuilder().addAll(networkServices);
        return this;
      }

      public abstract PluginMatchingResult<T> build();
    }
  }
}
