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

import java.time.Duration;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link FakeUtcClock}. */
@RunWith(JUnit4.class)
public class FakeUtcClockTest {
  private static final Instant TEST_INSTANT = Instant.ofEpochMilli(1927081738591L);
  private static final Duration TEST_DURATION = Duration.ofSeconds(20);

  @Test
  public void setNow_always_setsClockToGivenInstant() {
    assertThat(FakeUtcClock.create().setNow(TEST_INSTANT).instant()).isEqualTo(TEST_INSTANT);
  }

  @Test
  public void setNow_whenCallWithNull_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> FakeUtcClock.create().setNow(null));
  }

  @Test
  public void advance_always_advancesGivenDuration() {
    FakeUtcClock fakeUtcClock = FakeUtcClock.create().setNow(TEST_INSTANT);

    FakeUtcClock advancedClock = fakeUtcClock.advance(TEST_DURATION);

    assertThat(advancedClock.instant()).isEqualTo(TEST_INSTANT.plus(TEST_DURATION));
  }

  @Test
  public void advance_whenCalledWithNull_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> FakeUtcClock.create().advance(null));
  }
}
