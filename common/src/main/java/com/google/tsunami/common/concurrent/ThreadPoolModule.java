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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Key;
import com.google.inject.Singleton;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A helper module for binding a thread pool. The module will bind an {@link Executor}, {@link
 * ExecutorService} and {@link ListeningExecutorService} annotated with the annotation passed to the
 * builder.
 */
public final class ThreadPoolModule extends BaseThreadPoolModule<ListeningExecutorService> {
  private final BlockingQueue<Runnable> blockingQueue;

  private ThreadPoolModule(Builder builder) {
    super(checkNotNull(builder));

    this.blockingQueue = builder.getBlockingQueue();
  }

  @Override
  void configureThreadPool(Key<ListeningExecutorService> key) {
    bind(key.ofType(Executor.class)).to(key);
    bind(key.ofType(ExecutorService.class)).to(key);
    bind(key).toProvider(new ThreadPoolProvider()).in(Singleton.class);
  }

  private final class ThreadPoolProvider extends BaseThreadPoolProvider {
    @Override
    ThreadPoolExecutor createThreadPool(
        int coreSize,
        int maxSize,
        long keepAliveSeconds,
        ThreadFactory factory,
        RejectedExecutionHandler rejectedExecutionHandler) {
      return new ThreadPoolExecutor(
          coreSize,
          maxSize,
          keepAliveSeconds,
          TimeUnit.SECONDS,
          blockingQueue,
          factory,
          rejectedExecutionHandler);
    }
  }

  /** Builder for {@link ThreadPoolModule}. */
  public static final class Builder
      extends BaseThreadPoolModuleBuilder<ListeningExecutorService, Builder> {
    private int queueCapacity = Integer.MAX_VALUE;
    private @Nullable BlockingQueue<Runnable> blockingQueue;

    public Builder() {
      super(ListeningExecutorService.class);
    }

    @Override
    Builder self() {
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public Builder setMaxSize(int maxSize) {
      return super.setMaxSize(maxSize);
    }

    /** {@inheritDoc} */
    @Override
    public Builder setCoreSize(int coreSize) {
      return super.setCoreSize(coreSize);
    }

    /**
     * Sets the queue capacity for the thread pool.
     *
     * <p>NOTE: Users should NOT specify both this value and the {@link BlockingQueue} via {@link
     * #setBlockingQueue}.
     *
     * <p>By default, {@link SynchronousQueue} will be used when {@code queueCapacity} is set to
     * zero. Otherwise a {@link LinkedBlockingQueue} will be used.
     *
     * @param queueCapacity the capacity of the task queue.
     * @return the Builder instance itself.
     */
    public Builder setQueueCapacity(int queueCapacity) {
      checkArgument(queueCapacity >= 0, "The queue capacity should be non-negative value.");
      this.queueCapacity = queueCapacity;
      return this;
    }

    /**
     * Sets the {@link BlockingQueue} to use for holding tasks before they are executed.
     *
     * <p>NOTE: Do NOT set both {@link BlockingQueue} and {@code queueCapacity}. Only use this
     * method to override the default {@link BlockingQueue} choice. See comments of {@link
     * #getBlockingQueue} for which {@link BlockingQueue} is used by default.
     *
     * @param blockingQueue a {@link BlockingQueue} used for holding tasks before executing.
     * @return the Builder instance itself.
     */
    public Builder setBlockingQueue(BlockingQueue<Runnable> blockingQueue) {
      this.blockingQueue = checkNotNull(blockingQueue);
      return this;
    }

    private BlockingQueue<Runnable> getBlockingQueue() {
      if (blockingQueue == null) {
        return queueCapacity == 0
            ? new SynchronousQueue<>()
            : new LinkedBlockingQueue<>(queueCapacity);
      }
      return blockingQueue;
    }

    private boolean isBoundedQueue() {
      return (blockingQueue == null ? queueCapacity : blockingQueue.remainingCapacity())
          < Integer.MAX_VALUE;
    }

    @Override
    void validate() {
      checkState(
          blockingQueue == null || queueCapacity == Integer.MAX_VALUE,
          "Both custom BlockingQueue and queue capacity are specified.");
      if (coreSize < maxSize) {
        checkState(
            isBoundedQueue(),
            "Finite capacity queue should be set when the core pool size is less than max pool"
                + " size. ThreadPoolExecutor will only create new threads past core size when the"
                + " queue is full.");
      }
    }

    @Override
    ThreadPoolModule newModule() {
      return new ThreadPoolModule(this);
    }
  }
}
