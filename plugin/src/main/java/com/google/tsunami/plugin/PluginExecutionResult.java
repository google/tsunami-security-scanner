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
import com.google.common.base.Stopwatch;
import com.google.tsunami.plugin.PluginExecutor.PluginExecutorConfig;
import java.util.Optional;

/** The result of executing the Tsunami plugin core logic by the {@link PluginExecutor}. */
@AutoValue
public abstract class PluginExecutionResult<T> {
  /** All possible execution status of a Tsunami plugin. */
  public enum ExecutionStatus {
    SUCCEEDED,
    FAILED,
    TIMED_OUT
  }

  public boolean isSucceeded() {
    return executionStatus().equals(ExecutionStatus.SUCCEEDED);
  }

  public abstract ExecutionStatus executionStatus();
  public abstract Optional<T> resultData();
  public abstract Stopwatch executionStopwatch();
  public abstract Optional<PluginExecutionException> exception();
  public abstract PluginExecutorConfig<T> executorConfig();

  public static <T> Builder<T> builder() {
    return new AutoValue_PluginExecutionResult.Builder<>();
  }

  /** Builder for {@link PluginExecutionResult}. */
  @AutoValue.Builder
  public abstract static class Builder<T> {
    public abstract Builder<T> setExecutionStatus(ExecutionStatus executionStatus);
    public abstract Builder<T> setResultData(T resultData);
    public abstract Builder<T> setExecutionStopwatch(Stopwatch executionStopwatch);
    public abstract Builder<T> setException(PluginExecutionException exception);
    public abstract Builder<T> setExecutorConfig(PluginExecutorConfig<T> executorConfig);

    public abstract PluginExecutionResult<T> build();
  }
}
