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

import com.google.auto.value.AutoValue;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.tsunami.plugin.PluginManager.PluginMatchingResult;
import java.util.concurrent.Callable;

/** The interface of the component to execute Tsunami plugins. */
public interface PluginExecutor {
  /**
   * Executes a plugin's core business logic implemented in non-block manner.
   *
   * @param executorConfig The configuration of the execution, cannot be null.
   * @param <T> type of the plugin execution result.
   * @return The future of the execution result.
   */
  <T> ListenableFuture<PluginExecutionResult<T>> executeAsync(
      PluginExecutorConfig<T> executorConfig);

  /** Configures a plugin's core business logic to be executed by the {@link PluginExecutor}. */
  @AutoValue
  abstract class PluginExecutorConfig<T> {
    @SuppressWarnings("rawtypes")  // AutoValue bug for not handling generic correctly in this case.
    public abstract PluginMatchingResult matchedPlugin();
    public abstract Callable<T> pluginExecutionLogic();

    public static <T> Builder<T> builder() {
      return new AutoValue_PluginExecutor_PluginExecutorConfig.Builder<>();
    }

    @AutoValue.Builder
    public abstract static class Builder<T> {
      @SuppressWarnings("rawtypes")  // AutoValue bug for not handling generic correctly.
      public abstract Builder<T> setMatchedPlugin(PluginMatchingResult matchedPlugin);
      public abstract Builder<T> setPluginExecutionLogic(Callable<T> pluginExecutionLogic);

      public abstract PluginExecutorConfig<T> build();
    }
  }
}
