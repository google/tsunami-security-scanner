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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Qualifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ScheduledThreadPoolModule}. */
@RunWith(JUnit4.class)
public final class ScheduledThreadPoolModuleTest {

  /** Internal annotation used for tests. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @interface TestThreadPool {}

  @Test
  public void configure_always_bindsListeningScheduledExecutorService() {
    Injector injector =
        Guice.createInjector(
            new ScheduledThreadPoolModule.Builder()
                .setName("test")
                .setSize(1)
                .setAnnotation(TestThreadPool.class)
                .build());

    assertThat(
            injector.getInstance(Key.get(ScheduledExecutorService.class, TestThreadPool.class)))
        .isInstanceOf(ListeningScheduledExecutorService.class);
  }

  @Test
  public void configure_always_bindsSingleton() {
    Injector injector =
        Guice.createInjector(
            new ScheduledThreadPoolModule.Builder()
                .setName("test")
                .setSize(1)
                .setAnnotation(TestThreadPool.class)
                .build());

    ScheduledExecutorService scheduledExecutorService =
        injector.getInstance(Key.get(ScheduledExecutorService.class, TestThreadPool.class));
    ListeningScheduledExecutorService listeningScheduledExecutorService =
        injector.getInstance(
            Key.get(ListeningScheduledExecutorService.class, TestThreadPool.class));

    assertThat(scheduledExecutorService).isSameInstanceAs(listeningScheduledExecutorService);
  }
}
