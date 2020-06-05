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

import com.google.inject.AbstractModule;
import com.google.tsunami.common.concurrent.ThreadPoolModule;
import java.time.Duration;

/** Installs dependencies used for plugin executions. */
public final class PluginExecutionModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new PluginExecutorModule());

    install(
        new ThreadPoolModule.Builder()
            .setName("PluginExecution")
            .setCoreSize(16)
            .setMaxSize(32)
            .setQueueCapacity(64)
            .setDaemon(true)
            .setDelayedShutdown(Duration.ofMinutes(1))
            .setPriority(Thread.NORM_PRIORITY)
            .setAnnotation(PluginExecutionThreadPool.class)
            .build());
  }
}
