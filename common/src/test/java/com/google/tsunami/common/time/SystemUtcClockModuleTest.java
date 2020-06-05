/*
 * Copyright 2019 Google LLC
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
package com.google.tsunami.common.time;

import static com.google.common.truth.Truth.assertThat;

import com.google.inject.Guice;
import com.google.inject.Key;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link SystemUtcClockModule}. */
@RunWith(JUnit4.class)
public class SystemUtcClockModuleTest {

  @Test
  public void configure_always_bindsClockToSystemUtc() {
    Clock clock =
        Guice.createInjector(new SystemUtcClockModule())
            .getInstance(Key.get(Clock.class, UtcClock.class));

    assertThat(clock).isNotNull();
    assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    // A hacky way of testing the instance is a SystemClock.
    assertThat(clock.toString()).contains("SystemClock");
  }
}
