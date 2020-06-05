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

import com.google.common.base.Stopwatch;
import com.google.common.testing.FakeTicker;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.tsunami.plugin.PluginExecutor.PluginExecutorConfig;
import com.google.tsunami.plugin.PluginManager.PluginMatchingResult;
import com.google.tsunami.plugin.testing.FakePortScanner;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link PluginExecutorImpl}. */
@RunWith(JUnit4.class)
public final class PluginExecutorImplTest {
  private static final PluginMatchingResult<FakePortScanner> FAKE_MATCHING_RESULT =
      PluginMatchingResult.<FakePortScanner>builder()
          .setTsunamiPlugin(new FakePortScanner())
          .setPluginDefinition(PluginDefinition.forPlugin(FakePortScanner.class))
          .build();
  private static final Duration TICK_DURATION = Duration.ofSeconds(1);
  private static final ListeningExecutorService PLUGIN_EXECUTION_THREAD_POOL =
      MoreExecutors.newDirectExecutorService();

  private final FakeTicker ticker = new FakeTicker().setAutoIncrementStep(TICK_DURATION);
  private final Stopwatch executionStopWatch = Stopwatch.createUnstarted(ticker);

  @Test
  public void executeAsync_whenSucceeded_returnsSucceededResult()
      throws ExecutionException, InterruptedException {
    PluginExecutorConfig<String> executorConfig =
        PluginExecutorConfig.<String>builder()
            .setMatchedPlugin(FAKE_MATCHING_RESULT)
            .setPluginExecutionLogic(() -> "result data")
            .build();

    PluginExecutionResult<String> executionResult =
        new PluginExecutorImpl(PLUGIN_EXECUTION_THREAD_POOL, executionStopWatch)
            .executeAsync(executorConfig)
            .get();

    assertThat(executionResult.exception()).isEmpty();
    assertThat(executionResult.isSucceeded()).isTrue();
    assertThat(executionResult.executionStopwatch().elapsed()).isEqualTo(TICK_DURATION);
    assertThat(executionResult.resultData()).hasValue("result data");
  }

  @Test
  public void executeAsync_whenFailedWithPluginExecutionException_returnsFailedResult()
      throws ExecutionException, InterruptedException {
    PluginExecutorConfig<String> executorConfig =
        PluginExecutorConfig.<String>builder()
            .setMatchedPlugin(FAKE_MATCHING_RESULT)
            .setPluginExecutionLogic(
                () -> {
                  throw new PluginExecutionException("test exception");
                })
            .build();

    PluginExecutionResult<String> executionResult =
        new PluginExecutorImpl(PLUGIN_EXECUTION_THREAD_POOL, executionStopWatch)
            .executeAsync(executorConfig)
            .get();

    assertThat(executionResult.exception()).isPresent();
    assertThat(executionResult.exception().get()).hasCauseThat().isNull();
    assertThat(executionResult.exception().get()).hasMessageThat().contains("test exception");
    assertThat(executionResult.isSucceeded()).isFalse();
    assertThat(executionResult.executionStopwatch().elapsed()).isEqualTo(TICK_DURATION);
    assertThat(executionResult.resultData()).isEmpty();
  }

  @Test
  public void executeAsync_whenFailedWithUnknownException_returnsFailedResult()
      throws ExecutionException, InterruptedException {
    PluginExecutorConfig<String> executorConfig =
        PluginExecutorConfig.<String>builder()
            .setMatchedPlugin(FAKE_MATCHING_RESULT)
            .setPluginExecutionLogic(
                () -> {
                  throw new RuntimeException("test unknown exception");
                })
            .build();

    PluginExecutionResult<String> executionResult =
        new PluginExecutorImpl(PLUGIN_EXECUTION_THREAD_POOL, executionStopWatch)
            .executeAsync(executorConfig)
            .get();

    assertThat(executionResult.exception()).isPresent();
    assertThat(executionResult.exception().get())
        .hasCauseThat()
        .isInstanceOf(RuntimeException.class);
    assertThat(executionResult.exception().get())
        .hasMessageThat()
        .contains(
            String.format("Plugin execution error on '%s'.", FAKE_MATCHING_RESULT.pluginId()));
    assertThat(executionResult.isSucceeded()).isFalse();
    assertThat(executionResult.executionStopwatch().elapsed()).isEqualTo(TICK_DURATION);
    assertThat(executionResult.resultData()).isEmpty();
  }
}
