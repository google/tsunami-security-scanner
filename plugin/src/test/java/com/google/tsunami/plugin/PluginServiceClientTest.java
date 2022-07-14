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

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.tsunami.common.data.NetworkEndpointUtils;
import com.google.tsunami.proto.DetectionReport;
import com.google.tsunami.proto.DetectionReportList;
import com.google.tsunami.proto.ListPluginsRequest;
import com.google.tsunami.proto.ListPluginsResponse;
import com.google.tsunami.proto.MatchedPlugin;
import com.google.tsunami.proto.NetworkEndpoint;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.PluginDefinition;
import com.google.tsunami.proto.PluginInfo;
import com.google.tsunami.proto.PluginServiceGrpc.PluginServiceImplBase;
import com.google.tsunami.proto.RunRequest;
import com.google.tsunami.proto.RunResponse;
import com.google.tsunami.proto.TargetInfo;
import com.google.tsunami.proto.TransportProtocol;
import io.grpc.Deadline;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.health.v1.HealthGrpc.HealthImplBase;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.grpc.util.MutableHandlerRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link PluginServiceClient}. */
@RunWith(JUnit4.class)
public final class PluginServiceClientTest {

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private static final String PLUGIN_NAME = "test plugin";
  private static final String PLUGIN_VERSION = "0.0.1";
  private static final String PLUGIN_DESCRIPTION = "test description";
  private static final String PLUGIN_AUTHOR = "tester";

  private static final Deadline DEADLINE_DEFAULT = Deadline.after(5, SECONDS);

  private PluginServiceClient pluginService;
  private final MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();

  @Before
  public void setUp() throws Exception {
    String serverName = InProcessServerBuilder.generateName();
    grpcCleanup.register(
        InProcessServerBuilder.forName(serverName)
            .fallbackHandlerRegistry(serviceRegistry)
            .directExecutor()
            .build()
            .start());

    pluginService =
        new PluginServiceClient(
            InProcessChannelBuilder.forName(serverName).directExecutor().build());
  }

  @Test
  public void pluginService_isNotNull() {
    assertThat(pluginService).isNotNull();
  }

  @Test
  public void run_invalidRequest_returnNoDetectionReports() throws Exception {
    RunRequest runRequest = RunRequest.getDefaultInstance();
    PluginServiceImplBase runImpl =
        new PluginServiceImplBase() {
          @Override
          public void run(RunRequest request, StreamObserver<RunResponse> responseObserver) {
            responseObserver.onNext(RunResponse.getDefaultInstance());
            responseObserver.onCompleted();
          }
        };
    serviceRegistry.addService(runImpl);

    ListenableFuture<RunResponse> run = pluginService.runWithDeadline(runRequest, DEADLINE_DEFAULT);
    RunResponse runResponse = run.get();

    assertThat(run.isDone()).isTrue();
    assertThat(runResponse.hasReports()).isFalse();
  }

  @Test
  public void run_singlePluginValidRequest_returnSingleDetectionReport() throws Exception {
    RunRequest runRequest = createSinglePluginRunRequest();
    PluginServiceImplBase runImpl =
        new PluginServiceImplBase() {
          @Override
          public void run(RunRequest request, StreamObserver<RunResponse> responseObserver) {
            DetectionReportList reportList =
                DetectionReportList.newBuilder()
                    .addDetectionReports(
                        DetectionReport.newBuilder()
                            .setTargetInfo(request.getTarget())
                            .setNetworkService(request.getPlugins(0).getServices(0)))
                    .build();
            responseObserver.onNext(RunResponse.newBuilder().setReports(reportList).build());
            responseObserver.onCompleted();
          }
        };
    serviceRegistry.addService(runImpl);

    ListenableFuture<RunResponse> run = pluginService.runWithDeadline(runRequest, DEADLINE_DEFAULT);
    RunResponse runResponse = run.get();

    assertThat(run.isDone()).isTrue();
    assertRunResponseContainsAllRunRequestParameters(runResponse, runRequest);
  }

  @Test
  public void run_multiplePluginValidRequest_returnMultipleDetectionReports() throws Exception {
    int numPluginsToTest = 5;

    List<NetworkEndpoint> endpoints = new ArrayList<>(numPluginsToTest);
    endpoints.add(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80));
    endpoints.add(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 443));
    endpoints.add(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 123));
    endpoints.add(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 456));
    endpoints.add(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 789));

    PluginInfo.Builder pluginInfoBuilder =
        PluginInfo.newBuilder()
            .setType(PluginInfo.PluginType.VULN_DETECTION)
            .setVersion(PLUGIN_VERSION)
            .setDescription(PLUGIN_DESCRIPTION)
            .setAuthor(PLUGIN_AUTHOR);

    TargetInfo target = TargetInfo.newBuilder().addAllNetworkEndpoints(endpoints).build();

    RunRequest.Builder runRequestBuilder = RunRequest.newBuilder().setTarget(target);

    for (int i = 0; i < numPluginsToTest; i++) {
      PluginInfo pluginInfo =
          pluginInfoBuilder.setName(String.format(PLUGIN_NAME + " %d", i)).build();
      NetworkService httpService =
          NetworkService.newBuilder()
              .setNetworkEndpoint(endpoints.get(i))
              .setTransportProtocol(TransportProtocol.TCP)
              .setServiceName("http")
              .build();
      runRequestBuilder.addPlugins(
          MatchedPlugin.newBuilder()
              .addServices(httpService)
              .setPlugin(PluginDefinition.newBuilder().setInfo(pluginInfo).build()));
    }
    RunRequest runRequest = runRequestBuilder.build();

    PluginServiceImplBase runImpl =
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
        };
    serviceRegistry.addService(runImpl);

    ListenableFuture<RunResponse> run = pluginService.runWithDeadline(runRequest, DEADLINE_DEFAULT);
    RunResponse runResponse = run.get();

    assertThat(run.isDone()).isTrue();
    assertThat(runResponse.getReports().getDetectionReportsCount()).isEqualTo(numPluginsToTest);
    assertRunResponseContainsAllRunRequestParameters(runResponse, runRequest);
  }

  @Test
  public void listPlugins_returnsMultiplePlugins() throws Exception {
    ListPluginsRequest request = ListPluginsRequest.getDefaultInstance();

    List<PluginDefinition> plugins = Lists.newArrayList();
    for (int i = 0; i < 5; i++) {
      plugins.add(createSinglePluginDefinitionWithName(String.format(PLUGIN_NAME + "%d", i)));
    }

    PluginServiceImplBase listPluginsImpl =
        new PluginServiceImplBase() {
          @Override
          public void listPlugins(
              ListPluginsRequest request, StreamObserver<ListPluginsResponse> responseObserver) {
            responseObserver.onNext(
                ListPluginsResponse.newBuilder().addAllPlugins(plugins).build());
            responseObserver.onCompleted();
          }
        };
    serviceRegistry.addService(listPluginsImpl);

    ListenableFuture<ListPluginsResponse> listPlugins =
        pluginService.listPluginsWithDeadline(request, DEADLINE_DEFAULT);

    assertThat(listPlugins.isDone()).isTrue();
    assertThat(listPlugins.get().getPluginsList()).containsExactlyElementsIn(plugins);
  }

  @Test
  public void checkHealth_returnServingHealthResponse() throws Exception {
    HealthCheckRequest request = HealthCheckRequest.getDefaultInstance();

    HealthImplBase healthImpl =
        new HealthImplBase() {
          @Override
          public void check(
              HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
            responseObserver.onNext(
                HealthCheckResponse.newBuilder().setStatus(ServingStatus.SERVING).build());
            responseObserver.onCompleted();
          }
        };
    serviceRegistry.addService(healthImpl);

    ListenableFuture<HealthCheckResponse> health =
        pluginService.checkHealthWithDeadline(request, DEADLINE_DEFAULT);

    assertThat(health.isDone()).isTrue();
    assertThat(health.get().getStatus()).isEqualTo(ServingStatus.SERVING);
  }

  @Test
  public void checkHealth_returnNotServingHealthResponse() throws Exception {
    HealthCheckRequest request = HealthCheckRequest.getDefaultInstance();

    HealthImplBase healthImpl =
        new HealthImplBase() {
          @Override
          public void check(
              HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
            responseObserver.onNext(
                HealthCheckResponse.newBuilder().setStatus(ServingStatus.NOT_SERVING).build());
            responseObserver.onCompleted();
          }
        };
    serviceRegistry.addService(healthImpl);

    ListenableFuture<HealthCheckResponse> health =
        pluginService.checkHealthWithDeadline(request, DEADLINE_DEFAULT);

    assertThat(health.isDone()).isTrue();
    assertThat(health.get().getStatus()).isEqualTo(ServingStatus.NOT_SERVING);
  }

  private void assertRunResponseContainsAllRunRequestParameters(
      RunResponse response, RunRequest request) throws Exception {
    for (MatchedPlugin plugin : request.getPluginsList()) {
      DetectionReport expectedReport =
          DetectionReport.newBuilder()
              .setTargetInfo(request.getTarget())
              .setNetworkService(plugin.getServices(0))
              .build();
      assertThat(response.getReports().getDetectionReportsList()).contains(expectedReport);
    }
  }

  private PluginDefinition createSinglePluginDefinitionWithName(String name) {
    PluginInfo pluginInfo =
        PluginInfo.newBuilder()
            .setType(PluginInfo.PluginType.VULN_DETECTION)
            .setName(name)
            .setVersion(PLUGIN_VERSION)
            .setDescription(PLUGIN_DESCRIPTION)
            .setAuthor(PLUGIN_AUTHOR)
            .build();
    return PluginDefinition.newBuilder().setInfo(pluginInfo).build();
  }

  private RunRequest createSinglePluginRunRequest() {
    PluginDefinition singlePlugin = createSinglePluginDefinitionWithName(PLUGIN_NAME);
    NetworkService httpService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(NetworkEndpointUtils.forIpAndPort("1.1.1.1", 80))
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("http")
            .build();
    TargetInfo target =
        TargetInfo.newBuilder().addNetworkEndpoints(httpService.getNetworkEndpoint()).build();

    return RunRequest.newBuilder()
        .setTarget(target)
        .addPlugins(MatchedPlugin.newBuilder().addServices(httpService).setPlugin(singlePlugin))
        .build();
  }
}
