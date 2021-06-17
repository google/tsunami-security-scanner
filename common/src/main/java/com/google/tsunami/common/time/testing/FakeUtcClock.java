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
package com.google.tsunami.common.time.testing;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of a {@link java.time.Clock} that returns a settable {@link Instant} value.
 *
 * <p>By default, the clock is set to the {@link Instant} when the clock is created. Clock can be
 * set to a specific instant by {@link #setNow}.
 */
public final class FakeUtcClock extends Clock {

  private final AtomicReference<Instant> nowReference = new AtomicReference<>();

  private FakeUtcClock(Instant now) {
    nowReference.set(checkNotNull(now));
  }

  /**
   * Create a {@link FakeUtcClock} instance initialized to UTC now.
   *
   * <p>To create a fake UTC clock at a specific instant, calling {@code setNow()} as in:
   *
   * <pre>{@code FakeUtcClock fakeUtcClock = FakeUtcClock.create().setNow(TARGET_INSTANT);}</pre>
   *
   * @return a {@link FakeUtcClock} instance.
   */
  public static FakeUtcClock create() {
    return new FakeUtcClock(Instant.now());
  }

  /**
   * Sets the return value of {@link #instant()}.
   *
   * @param now the instant that this clock points to
   * @return this
   */
  public FakeUtcClock setNow(Instant now) {
    nowReference.set(checkNotNull(now));
    return this;
  }

  /**
   * Advances the clock by the given duration.
   *
   * <p>NOTE: this method can be called with a negative duration if the clock needs to go back in
   * time.
   *
   * @param increment the duration to advance the clock by
   * @return this
   */
  public FakeUtcClock advance(Duration increment) {
    checkNotNull(increment);
    nowReference.getAndUpdate(now -> now.plus(increment));
    return this;
  }

  @Override
  public Instant instant() {
    return nowReference.get();
  }

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    throw new UnsupportedOperationException("Setting ZoneId to FakeUtcClock is not supported");
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof FakeUtcClock) {
      FakeUtcClock other = (FakeUtcClock) obj;
      return nowReference.get().equals(other.nowReference.get());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return nowReference.get().hashCode();
  }

  @Override
  public String toString() {
    return String.format("FakeUtcClock(now = %s)", nowReference.get());
  }
}
