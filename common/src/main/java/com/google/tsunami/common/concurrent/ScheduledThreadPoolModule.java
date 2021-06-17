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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.inject.Key;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import javax.inject.Singleton;

/**
 * A helper module for binding a scheduled thread pool. The module will bind a {@link
 * ScheduledExecutorService} and a {@link ListeningScheduledExecutorService} to a singleton thread
 * pool that is annotated with the annotation passed to the builder.
 */
public final class ScheduledThreadPoolModule
    extends BaseThreadPoolModule<ListeningScheduledExecutorService> {

  ScheduledThreadPoolModule(Builder builder) {
    super(builder);
  }

  @Override
  void configureThreadPool(Key<ListeningScheduledExecutorService> key) {
    bind(key.ofType(ScheduledExecutorService.class)).to(key);
    bind(key).toProvider(new ScheduledThreadPoolProvider()).in(Singleton.class);
  }

  private final class ScheduledThreadPoolProvider extends BaseThreadPoolProvider {

    @Override
    ScheduledThreadPoolExecutor createThreadPool(
        int coreSize,
        int maxSize,
        long keepAliveSeconds,
        ThreadFactory factory,
        RejectedExecutionHandler rejectedExecutionHandler) {
      ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
          new ScheduledThreadPoolExecutor(coreSize, factory, rejectedExecutionHandler);
      scheduledThreadPoolExecutor.setMaximumPoolSize(maxSize);
      scheduledThreadPoolExecutor.setKeepAliveTime(keepAliveSeconds, SECONDS);
      return scheduledThreadPoolExecutor;
    }
  }

  /**
   * Builder for {@link ScheduledThreadPoolModule}.
   *
   * <p>NOTE: Unlike {@link ThreadPoolModule}, {@link ScheduledThreadPoolExecutor} acts as a
   * fixed-sized pool using {@code corePoolSize} threads and an unbounded queue. So this builder
   * only allows users to set a fixed thread pool size.
   */
  public static final class Builder
      extends BaseThreadPoolModuleBuilder<ListeningScheduledExecutorService, Builder> {

    public Builder() {
      super(ListeningScheduledExecutorService.class);
    }

    @Override
    Builder self() {
      return this;
    }

    /**
     * Sets the size of the thread pool.
     *
     * @param size the size of the thread pool.
     * @return the {@link Builder} instance itself.
     */
    public Builder setSize(int size) {
      checkArgument(size > 0, "Thread pool size should be positive.");
      setCoreSize(size);
      setMaxSize(size);
      return this;
    }

    @Override
    void validate() {}

    @Override
    ScheduledThreadPoolModule newModule() {
      return new ScheduledThreadPoolModule(this);
    }
  }
}
