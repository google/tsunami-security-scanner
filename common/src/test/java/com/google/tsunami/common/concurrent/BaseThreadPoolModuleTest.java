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

import static org.junit.Assert.assertThrows;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ThreadPoolModule}. */
@RunWith(JUnit4.class)
public final class BaseThreadPoolModuleTest {

  /** Internal annotation used for tests. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @interface TestThreadPool {}

  static final class TestThreadPoolModule extends BaseThreadPoolModule<ListeningExecutorService> {

    TestThreadPoolModule(Builder builder) {
      super(builder);
    }

    @Override
    void configureThreadPool(Key<ListeningExecutorService> key) {}

    static final class Builder
        extends BaseThreadPoolModuleBuilder<ListeningExecutorService, Builder> {

      Builder() {
        super(ListeningExecutorService.class);
      }

      @Override
      Builder self() {
        return this;
      }

      @Override
      void validate() {}

      @Override
      AbstractModule newModule() {
        return new TestThreadPoolModule(this);
      }
    }
  }

  @Test
  public void build_whenNoName_throwsIllegalStateException() {
    assertThrows(IllegalStateException.class, () -> new TestThreadPoolModule.Builder().build());
  }

  @Test
  public void build_whenEmptyName_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new TestThreadPoolModule.Builder().setName("").build());
  }

  @Test
  public void build_whenNegativeCoreSize_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new TestThreadPoolModule.Builder().setName("test").setCoreSize(-1).build());
  }

  @Test
  public void build_whenNegativeMaxSize_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new TestThreadPoolModule.Builder().setName("test").setMaxSize(-1).build());
  }

  @Test
  public void build_whenNegativeKeepAliveSeconds_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TestThreadPoolModule.Builder()
                .setName("test")
                .setMaxSize(1)
                .setKeepAliveSeconds(-1)
                .build());
  }

  @Test
  public void build_whenCoreSizeLessThanMaxSize_throwsIllegalStateException() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new TestThreadPoolModule.Builder()
                .setName("test")
                .setCoreSize(2)
                .setMaxSize(1)
                .setAnnotation(TestThreadPool.class)
                .build());
  }

  @Test
  public void build_whenNoAnnotation_throwsIllegalStateException() {
    assertThrows(
        IllegalStateException.class,
        () -> new TestThreadPoolModule.Builder().setName("test").build());
  }
}
