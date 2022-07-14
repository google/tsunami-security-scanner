/*
 * Copyright 2022 Google LLC
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

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.tsunami.common.data.NetworkEndpointUtils;
import com.google.tsunami.proto.DetectionReport;
import com.google.tsunami.proto.DetectionReportList;
import com.google.tsunami.proto.ListPluginsRequest;
import com.google.tsunami.proto.ListPluginsResponse;
import com.google.tsunami.proto.MatchedPlugin;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.PluginDefinition;
import com.google.tsunami.proto.PluginInfo;
import com.google.tsunami.proto.PluginServiceGrpc.PluginServiceImplBase;
import com.google.tsunami.proto.RunRequest;
import com.google.tsunami.proto.RunResponse;
import com.google.tsunami.proto.TargetInfo;
import com.google.tsunami.proto.TransportProtocol;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.health.v1.HealthGrpc.HealthImplBase;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.grpc.util.MutableHandlerRegistry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class RemoteVulnDetectorImplTest {

  private static final String PLUGIN_VERSION = "0.0.1";
  private static final String PLUGIN_DESCRIPTION = "test description";
  private static final String PLUGIN_AUTHOR = "tester";

  private final MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Before
  public void setUp() throws Exception {
    serviceRegistry.addService(
        new PluginServiceImplBase() {
          @Override
          public void run(RunRequest request, StreamObserver<RunResponse> responseObserver) {
            DetectionReportList.Builder reportListBuilder = DetectionReportList.newBuilder();
            for (MatchedPlugin plugin : request.getPluginsList()) {
              reportListBuilder.addDetectionReports(
                  DetectionReport.newBuilder()
                      .setTargetInfo(request.getTarget())
                      .setNetworkService(plugin.getServices(0)));
            }
            responseObserver.onNext(RunResponse.newBuilder().setReports(reportListBuilder).build());
            responseObserver.onCompleted();
          }
        });
  }

  @Test
  public void detect_withServingServer_returnsSuccessfulDetectionReportList() throws Exception {
    registerHealthCheckWithStatus(ServingStatus.SERVING);
    registerSuccessfulRunService();

    RemoteVulnDetector pluginToTest = getNewRemoteVulnDetectorInstance();
    var endpointToTest = NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80);
    var serviceToTest =
        NetworkService.newBuilder()
            .setNetworkEndpoint(endpointToTest)
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("http")
            .build();

    TargetInfo testTarget = TargetInfo.newBuilder().addNetworkEndpoints(endpointToTest).build();
    pluginToTest.addMatchedPluginToDetect(
        MatchedPlugin.newBuilder()
            .addServices(serviceToTest)
            .setPlugin(createSinglePluginDefinitionWithName("test"))
            .build());
    assertThat(pluginToTest.detect(testTarget, ImmutableList.of()).getDetectionReportsList())
        .comparingExpectedFieldsOnly()
        .containsExactly(
            DetectionReport.newBuilder()
                .setTargetInfo(testTarget)
                .setNetworkService(serviceToTest)
                .build());
  }

  @Test
  public void detect_withNonServingServer_returnsEmptyDetectionReportList() throws Exception {
    registerHealthCheckWithStatus(ServingStatus.NOT_SERVING);

    RemoteVulnDetector pluginToTest = getNewRemoteVulnDetectorInstance();
    var endpointToTest = NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80);
    var serviceToTest =
        NetworkService.newBuilder()
            .setNetworkEndpoint(endpointToTest)
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("http")
            .build();

    TargetInfo testTarget = TargetInfo.newBuilder().addNetworkEndpoints(endpointToTest).build();
    pluginToTest.addMatchedPluginToDetect(
        MatchedPlugin.newBuilder()
            .addServices(serviceToTest)
            .setPlugin(createSinglePluginDefinitionWithName("test"))
            .build());
    assertThat(pluginToTest.detect(testTarget, ImmutableList.of()).getDetectionReportsList())
        .isEmpty();
  }

  @Test
  public void detect_withRpcError_throwsLanguageServerException() throws Exception {
    registerHealthCheckWithError();

    assertThrows(
        LanguageServerException.class,
        () ->
            getNewRemoteVulnDetectorInstance()
                .detect(TargetInfo.getDefaultInstance(), ImmutableList.of()));
  }

  @Test
  public void getAllPlugins_withServingServer_returnsSuccessfulList() throws Exception {
    registerHealthCheckWithStatus(ServingStatus.SERVING);

    var plugin = createSinglePluginDefinitionWithName("test");
    RemoteVulnDetector pluginToTest = getNewRemoteVulnDetectorInstance();
    serviceRegistry.addService(
        new PluginServiceImplBase() {
          @Override
          public void listPlugins(
              ListPluginsRequest request, StreamObserver<ListPluginsResponse> responseObserver) {
            responseObserver.onNext(ListPluginsResponse.newBuilder().addPlugins(plugin).build());
            responseObserver.onCompleted();
          }
        });

    assertThat(pluginToTest.getAllPlugins()).containsExactly(plugin);
  }

  @Test
  public void getAllPlugins_withNonServingServer_returnsEmptyList() throws Exception {
    registerHealthCheckWithStatus(ServingStatus.NOT_SERVING);
    assertThat(getNewRemoteVulnDetectorInstance().getAllPlugins()).isEmpty();
  }

  @Test
  public void getAllPlugins_withRpcError_throwsLanguageServerException() throws Exception {
    registerHealthCheckWithError();
    assertThrows(LanguageServerException.class, getNewRemoteVulnDetectorInstance()::getAllPlugins);
  }

  private RemoteVulnDetector getNewRemoteVulnDetectorInstance() throws Exception {
    String serverName = InProcessServerBuilder.generateName();
    grpcCleanup.register(
        InProcessServerBuilder.forName(serverName)
            .fallbackHandlerRegistry(serviceRegistry)
            .directExecutor()
            .build()
            .start());

    return Guice.createInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(RemoteVulnDetector.class)
                    .toInstance(
                        new RemoteVulnDetectorImpl(
                            InProcessChannelBuilder.forName(serverName).directExecutor().build()));
              }
            })
        .getInstance(RemoteVulnDetector.class);
  }

  private void registerHealthCheckWithError() {
    serviceRegistry.addService(
        new HealthImplBase() {
          @Override
          public void check(
              HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
            responseObserver.onError(new RuntimeException("Test failure."));
            responseObserver.onCompleted();
          }
        });
  }

  private void registerHealthCheckWithStatus(ServingStatus status) {
    serviceRegistry.addService(
        new HealthImplBase() {
          @Override
          public void check(
              HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
            responseObserver.onNext(HealthCheckResponse.newBuilder().setStatus(status).build());
            responseObserver.onCompleted();
          }
        });
  }

  private void registerSuccessfulRunService() {
    serviceRegistry.addService(
        new PluginServiceImplBase() {
          @Override
          public void run(RunRequest request, StreamObserver<RunResponse> responseObserver) {
            DetectionReportList.Builder reportListBuilder = DetectionReportList.newBuilder();
            for (MatchedPlugin plugin : request.getPluginsList()) {
              reportListBuilder.addDetectionReports(
                  DetectionReport.newBuilder()
                      .setTargetInfo(request.getTarget())
                      .setNetworkService(plugin.getServices(0)));
            }
            responseObserver.onNext(RunResponse.newBuilder().setReports(reportListBuilder).build());
            responseObserver.onCompleted();
          }
        });
  }

  private PluginDefinition createSinglePluginDefinitionWithName(String name) {
    return PluginDefinition.newBuilder()
        .setInfo(
            PluginInfo.newBuilder()
                .setType(PluginInfo.PluginType.VULN_DETECTION)
                .setName(name)
                .setVersion(PLUGIN_VERSION)
                .setDescription(PLUGIN_DESCRIPTION)
                .setAuthor(PLUGIN_AUTHOR))
        .build();
  }
}
