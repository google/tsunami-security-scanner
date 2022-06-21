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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.tsunami.common.concurrent.ScheduledThreadPoolModule;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.grpc.util.MutableHandlerRegistry;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link PluginServiceClient}. */
@RunWith(JUnit4.class)
public final class PluginServiceClientTest {

  // Useful test thread pool used for testing grpc handlers
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @interface TestThreadPool {}

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private static final int THREAD_POOLS = 1;
  private static final String THREAD_POOL_NAME = "test";

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
            InProcessChannelBuilder.forName(serverName).directExecutor().build(),
            Guice.createInjector(
                    new ScheduledThreadPoolModule.Builder()
                        .setName(THREAD_POOL_NAME)
                        .setSize(THREAD_POOLS)
                        .setAnnotation(TestThreadPool.class)
                        .build())
                .getInstance(
                    Key.get(ListeningScheduledExecutorService.class, TestThreadPool.class)));
  }

  @Test
  public void pluginService_isNotNull() {
    assertThat(pluginService).isNotNull();
  }

}
