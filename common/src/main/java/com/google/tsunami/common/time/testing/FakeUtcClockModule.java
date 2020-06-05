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

import com.google.inject.AbstractModule;
import com.google.tsunami.common.time.UtcClock;
import java.time.Clock;

/** Binds {@link java.time.Clock} to a {@link FakeUtcClock}. */
public class FakeUtcClockModule extends AbstractModule {
  private final FakeUtcClock fakeUtcClock;

  public FakeUtcClockModule() {
    this.fakeUtcClock = FakeUtcClock.create();
  }

  public FakeUtcClockModule(FakeUtcClock fakeUtcClock) {
    this.fakeUtcClock = checkNotNull(fakeUtcClock);
  }

  @Override
  protected void configure() {
    bind(Clock.class).annotatedWith(UtcClock.class).toInstance(fakeUtcClock);
    bind(FakeUtcClock.class).annotatedWith(UtcClock.class).toInstance(fakeUtcClock);
  }
}
