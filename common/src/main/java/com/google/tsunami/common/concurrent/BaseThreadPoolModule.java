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

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The base module for binding a thread pool.
 *
 * <p>This module is essentially a thin wrapper around {@link ThreadFactoryBuilder} and the
 * corresponding {@link ExecutorService} families. Based on the intended usage, it is expected that
 * subclasses of this module should provides bindings to a concrete thread pool implementation of
 * {@link ExecutorService}. This base module wraps the actual {@link ExecutorService} implementation
 * in order to support {@link com.google.common.util.concurrent.ListenableFuture} usage in the code
 * base.
 *
 * @param <ExecutorServiceT> The expected thread pool implementation, must be a subclass of {@link
 *     ListeningExecutorService}.
 */
abstract class BaseThreadPoolModule<ExecutorServiceT extends ListeningExecutorService>
    extends AbstractModule {
  private final ThreadFactory factory;
  private final int maxSize;
  private final int coreSize;
  private final long keepAliveSeconds;
  private final @Nullable Duration shutdownDelay;
  private final Key<ExecutorServiceT> key;
  private final Class<ExecutorServiceT> executorServiceTypeClass;
  private final RejectedExecutionHandler rejectedExecutionHandler;

  BaseThreadPoolModule(BaseThreadPoolModuleBuilder<ExecutorServiceT, ?> builder) {
    checkNotNull(builder);

    this.factory = builder.factoryBuilder.build();
    this.maxSize = builder.maxSize;
    this.coreSize = builder.coreSize;
    this.keepAliveSeconds = builder.keepAliveSeconds;
    this.shutdownDelay = builder.daemon ? builder.shutdownDelay : null;
    this.key = builder.key;
    this.executorServiceTypeClass = builder.executorServiceTypeClass;
    this.rejectedExecutionHandler = builder.rejectedExecutionHandler;
  }

  @Override
  protected final void configure() {
    configureThreadPool(key);
  }

  /** Subclasses should override this method for providing Guice bindings. */
  abstract void configureThreadPool(Key<ExecutorServiceT> key);

  /** Base {@link Provider} implementation for providing the target thread pool. */
  abstract class BaseThreadPoolProvider implements Provider<ExecutorServiceT> {
    abstract ExecutorService createThreadPool(
        int coreSize,
        int maxSize,
        long keepAliveSeconds,
        ThreadFactory factory,
        RejectedExecutionHandler rejectedExecutionHandler);

    @Override
    public final ExecutorServiceT get() {
      ExecutorService service =
          createThreadPool(coreSize, maxSize, keepAliveSeconds, factory, rejectedExecutionHandler);

      if (shutdownDelay != null) {
        MoreExecutors.addDelayedShutdownHook(
            service, shutdownDelay.toMillis(), TimeUnit.MILLISECONDS);
      }
      return executorServiceTypeClass.cast(MoreExecutors.listeningDecorator(service));
    }
  }

  /** Base Builder for {@link BaseThreadPoolModule}. */
  abstract static class BaseThreadPoolModuleBuilder<
      ExecutorServiceT extends ListeningExecutorService,
      BuilderImplT extends BaseThreadPoolModuleBuilder<ExecutorServiceT, BuilderImplT>> {
    protected final ThreadFactoryBuilder factoryBuilder = new ThreadFactoryBuilder();
    protected String name;
    protected int maxSize;
    protected int coreSize;
    protected long keepAliveSeconds = 60L;
    protected boolean daemon;
    protected Duration shutdownDelay;
    protected Key<ExecutorServiceT> key;
    protected final Class<ExecutorServiceT> executorServiceTypeClass;
    protected RejectedExecutionHandler rejectedExecutionHandler = new AbortPolicy();

    BaseThreadPoolModuleBuilder(Class<ExecutorServiceT> executorServiceTypeClass) {
      this.executorServiceTypeClass = checkNotNull(executorServiceTypeClass);
    }

    abstract BuilderImplT self();

    /**
     * Sets the name used to name the threads; automatically suffixed with "-%s"to incorporate the
     * thread number
     *
     * @param name the name of the thread pool.
     * @return the Builder instance itself.
     */
    public BuilderImplT setName(String name) {
      checkArgument(!Strings.isNullOrEmpty(name), "Name should not be empty");
      this.name = name;
      return self();
    }

    /**
     * Sets the maximum number of threads allowed in the pool; value should be positive.
     *
     * @param maxSize the maximum number of threads allowed in this thread pool.
     * @return the Builder instance itself.
     */
    BuilderImplT setMaxSize(int maxSize) {
      checkArgument(maxSize > 0, "Max thread pool size should be positive.");
      this.maxSize = maxSize;
      return self();
    }

    /**
     * Sets the number of threads to keep in the pool.
     *
     * @param coreSize the minimum number of threads to keep alive in this thread pool.
     * @return the Builder instance itself.
     */
    BuilderImplT setCoreSize(int coreSize) {
      checkArgument(coreSize >= 0, "The core pool size should be non-negative.");
      this.coreSize = coreSize;
      return self();
    }

    /**
     * Sets the keep alive time in seconds for the threads not in core pool.
     *
     * @param keepAliveSeconds the maximum number of seconds an idle thread in this pool can keep
     *     alive before being terminated.
     * @return the Builder instance itself.
     */
    public BuilderImplT setKeepAliveSeconds(long keepAliveSeconds) {
      checkArgument(keepAliveSeconds >= 0, "The keep alive time should be non-negative.");
      this.keepAliveSeconds = keepAliveSeconds;
      return self();
    }

    /**
     * Sets whether or not new threads created by the pool will be daemon threads.*
     *
     * @param daemon whether threads created in this pool are daemon threads.
     * @return the Builder instance itself.
     */
    public BuilderImplT setDaemon(boolean daemon) {
      factoryBuilder.setDaemon(daemon);
      this.daemon = daemon;
      return self();
    }

    /**
     * Sets how long the JVM should wait to exit for daemon threads to complete.
     *
     * <p>This has no effect if the pool does not use daemon threads.
     *
     * @param shutdownDelay the delay enforced during the thread pool shutdown.
     * @return the Builder instance itself.
     */
    public BuilderImplT setDelayedShutdown(Duration shutdownDelay) {
      this.shutdownDelay = checkNotNull(shutdownDelay);
      return self();
    }

    /**
     * Sets the priority for threads created by the pool.
     *
     * @param priority the priority of the threads created by this pool.
     * @return the Builder instance itself.
     */
    public BuilderImplT setPriority(int priority) {
      factoryBuilder.setPriority(priority);
      return self();
    }

    /**
     * Sets the binding annotation.
     *
     * @param annotation the Guice binding annotation for this thread pool.
     * @return the Builder instance itself.
     */
    public BuilderImplT setAnnotation(Annotation annotation) {
      key = Key.get(executorServiceTypeClass, checkNotNull(annotation));
      return self();
    }

    /**
     * Sets the binding annotation.
     *
     * @param annotationClass the Guice binding annotation class for this thread pool.
     * @return the Builder instance itself.
     */
    public BuilderImplT setAnnotation(Class<? extends Annotation> annotationClass) {
      key = Key.get(executorServiceTypeClass, checkNotNull(annotationClass));
      return self();
    }

    /**
     * Sets the handler to use when thread execution is blocked due to thread bounds and queue
     * capacities are reached.
     *
     * <p>By default, {@link AbortPolicy} is used for rejected execution, which throws the {@link
     * java.util.concurrent.RejectedExecutionException}.
     *
     * @param rejectedExecutionHandler A handler for tasks that cannot be executed by this thread
     *     pool.
     * @return the Builder instance itself.
     */
    public BuilderImplT setRejectedExecutionHandler(
        RejectedExecutionHandler rejectedExecutionHandler) {
      this.rejectedExecutionHandler = checkNotNull(rejectedExecutionHandler);
      return self();
    }

    final void validateAll() {
      checkState(!Strings.isNullOrEmpty(name), "Name is required.");
      checkState(
          maxSize > 0,
          "Max thread pool size must be positive. Did you forget setting maximum thread pool size"
              + " by calling setMaxSize?");
      checkState(
          coreSize <= maxSize, "Thread pool core size should be less than or equal to max size.");
      checkState(key != null, "Annotation is required.");

      validate();
    }

    abstract void validate();

    public final AbstractModule build() {
      validateAll();
      return newModule();
    }

    abstract AbstractModule newModule();
  }
}
