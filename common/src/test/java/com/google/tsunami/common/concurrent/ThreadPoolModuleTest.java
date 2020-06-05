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
package com.google.tsunami.common.concurrent;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import javax.inject.Qualifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ThreadPoolModule}. */
@RunWith(JUnit4.class)
public final class ThreadPoolModuleTest {

  /** Internal annotation used for tests. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @interface TestThreadPool {}

  @Test
  public void configure_always_bindsListeningExecutorService() {
    Injector injector =
        Guice.createInjector(
            new ThreadPoolModule.Builder()
                .setName("test")
                .setCoreSize(1)
                .setMaxSize(1)
                .setAnnotation(TestThreadPool.class)
                .build());

    assertThat(injector.getInstance(Key.get(Executor.class, TestThreadPool.class)))
        .isInstanceOf(ListeningExecutorService.class);
    assertThat(injector.getInstance(Key.get(ExecutorService.class, TestThreadPool.class)))
        .isInstanceOf(ListeningExecutorService.class);
  }

  @Test
  public void configure_always_bindsSingleton() {
    Injector injector =
        Guice.createInjector(
            new ThreadPoolModule.Builder()
                .setName("test")
                .setCoreSize(1)
                .setMaxSize(1)
                .setAnnotation(TestThreadPool.class)
                .build());

    Executor executor = injector.getInstance(Key.get(Executor.class, TestThreadPool.class));
    ExecutorService executorService =
        injector.getInstance(Key.get(ExecutorService.class, TestThreadPool.class));
    ListeningExecutorService listeningExecutorService =
        injector.getInstance(Key.get(ListeningExecutorService.class, TestThreadPool.class));

    assertThat(executor).isSameInstanceAs(listeningExecutorService);
    assertThat(executorService).isSameInstanceAs(listeningExecutorService);
  }

  @Test
  public void build_whenNegativeQueueCapacity_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ThreadPoolModule.Builder()
                .setName("test")
                .setMaxSize(1)
                .setQueueCapacity(-1)
                .build());
  }

  @Test
  public void build_whenBothQueueCapacityAndBlockingQueueSet_throwsIllegalStateException() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new ThreadPoolModule.Builder()
                .setMaxSize(1)
                .setQueueCapacity(1)
                .setBlockingQueue(new PriorityBlockingQueue<>(1))
                .build());
  }

  @Test
  public void build_whenUnBoundedQueue_throwsIllegalStateException() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new ThreadPoolModule.Builder()
                .setName("test")
                .setCoreSize(1)
                .setMaxSize(2)
                .setAnnotation(TestThreadPool.class)
                .build());
  }
}
