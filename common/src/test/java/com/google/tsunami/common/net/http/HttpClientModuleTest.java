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
package com.google.tsunami.common.net.http;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.time.Duration;
import okhttp3.OkHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link HttpClientModule}. */
@RunWith(JUnit4.class)
public final class HttpClientModuleTest {

  @Test
  public void provideOkHttpClient_always_createsSingleton() {
    Injector injector =
        Guice.createInjector(new HttpClientModule.Builder().setMaxRequests(10).build());

    OkHttpClient okHttpClient = injector.getInstance(OkHttpClient.class);
    OkHttpClient okHttpClient2 = injector.getInstance(OkHttpClient.class);

    assertThat(okHttpClient).isSameInstanceAs(okHttpClient2);
  }

  @Test
  public void setConnectionPoolMaxIdle_whenNonPositiveMaxIdle_throwsIllegalArgumentException() {
    HttpClientModule.Builder builder = new HttpClientModule.Builder();

    assertThrows(IllegalArgumentException.class, () -> builder.setConnectionPoolMaxIdle(-1));
    assertThrows(IllegalArgumentException.class, () -> builder.setConnectionPoolMaxIdle(0));
  }

  @Test
  public void
      setConnectionPoolKeepAliveDuration_whenNegativeDuration_throwsIllegalArgumentException() {
    HttpClientModule.Builder builder = new HttpClientModule.Builder();

    assertThrows(
        IllegalArgumentException.class,
        () -> builder.setConnectionPoolKeepAliveDuration(Duration.ofMillis(-1)));
  }

  @Test
  public void setMaxRequests_whenPositiveRequests_setsValueToDispatcher() {
    Injector injector =
        Guice.createInjector(new HttpClientModule.Builder().setMaxRequests(10).build());

    OkHttpClient okHttpClient = injector.getInstance(OkHttpClient.class);

    assertThat(okHttpClient.dispatcher().getMaxRequests()).isEqualTo(10);
  }

  @Test
  public void setMaxRequests_whenNonPositiveRequests_throwsIllegalArgumentException() {
    HttpClientModule.Builder builder = new HttpClientModule.Builder();

    assertThrows(IllegalArgumentException.class, () -> builder.setMaxRequests(-1));
    assertThrows(IllegalArgumentException.class, () -> builder.setMaxRequests(0));
  }

  @Test
  public void setMaxRequestsPerHost_whenPositiveRequests_setsValueToDispatcher() {
    Injector injector =
        Guice.createInjector(new HttpClientModule.Builder().setMaxRequestsPerHost(10).build());

    OkHttpClient okHttpClient = injector.getInstance(OkHttpClient.class);

    assertThat(okHttpClient.dispatcher().getMaxRequestsPerHost()).isEqualTo(10);
  }

  @Test
  public void setMaxRequestsPerHost_whenNonPositiveRequests_throwsIllegalArgumentException() {
    HttpClientModule.Builder builder = new HttpClientModule.Builder();

    assertThrows(IllegalArgumentException.class, () -> builder.setMaxRequestsPerHost(-1));
    assertThrows(IllegalArgumentException.class, () -> builder.setMaxRequestsPerHost(0));
  }

  @Test
  public void setFollowRedirects_always_setsValueToClient() {
    Injector injector =
        Guice.createInjector(new HttpClientModule.Builder().setFollowRedirects(true).build());

    OkHttpClient okHttpClient = injector.getInstance(OkHttpClient.class);

    assertThat(okHttpClient.followRedirects()).isTrue();
  }
}
