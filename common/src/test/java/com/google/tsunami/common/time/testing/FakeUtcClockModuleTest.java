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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.tsunami.common.time.UtcClock;
import java.time.Clock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link FakeUtcClockModule}. */
@RunWith(JUnit4.class)
public class FakeUtcClockModuleTest {

  @Test
  public void constructor_withNullFakeClock_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> new FakeUtcClockModule(null));
  }

  @Test
  public void configure_always_bindsToSameInstance() {
    FakeUtcClock fakeUtcClock = FakeUtcClock.create();

    Injector injector = Guice.createInjector(new FakeUtcClockModule(fakeUtcClock));

    assertThat(injector.getInstance(Key.get(Clock.class, UtcClock.class)))
        .isSameInstanceAs(fakeUtcClock);
    assertThat(injector.getInstance(Key.get(Clock.class, UtcClock.class)))
        .isSameInstanceAs(fakeUtcClock);
    assertThat(injector.getInstance(Key.get(FakeUtcClock.class, UtcClock.class)))
        .isSameInstanceAs(fakeUtcClock);
    assertThat(injector.getInstance(Key.get(FakeUtcClock.class, UtcClock.class)))
        .isSameInstanceAs(fakeUtcClock);
  }
}
