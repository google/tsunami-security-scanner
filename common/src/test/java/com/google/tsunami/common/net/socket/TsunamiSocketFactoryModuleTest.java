/*
 * Copyright 2025 Google LLC
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
package com.google.tsunami.common.net.socket;

import static com.google.common.truth.Truth.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.tsunami.common.net.socket.TsunamiSocketFactoryModule.ConnectTimeoutSeconds;
import com.google.tsunami.common.net.socket.TsunamiSocketFactoryModule.ReadTimeoutSeconds;
import com.google.tsunami.common.net.socket.TsunamiSocketFactoryModule.TrustAllCertificates;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link TsunamiSocketFactoryModule}. */
@RunWith(JUnit4.class)
public final class TsunamiSocketFactoryModuleTest {

  private TsunamiSocketFactoryCliOptions cliOptions;
  private TsunamiSocketFactoryConfigProperties configProperties;

  @Before
  public void setUp() {
    cliOptions = new TsunamiSocketFactoryCliOptions();
    configProperties = new TsunamiSocketFactoryConfigProperties();
  }

  @Test
  public void provideTsunamiSocketFactory_returnsNonNullFactory() throws Exception {
    Injector injector = Guice.createInjector(getTestingModule());

    TsunamiSocketFactory factory = injector.getInstance(TsunamiSocketFactory.class);

    assertThat(factory).isNotNull();
    assertThat(factory).isInstanceOf(DefaultTsunamiSocketFactory.class);
  }

  @Test
  public void provideTsunamiSocketFactory_withDefaultConfig_usesDefaultTimeouts() throws Exception {
    Injector injector = Guice.createInjector(getTestingModule());

    TsunamiSocketFactory factory = injector.getInstance(TsunamiSocketFactory.class);

    assertThat(factory.getDefaultConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
    assertThat(factory.getDefaultReadTimeout()).isEqualTo(Duration.ofSeconds(30));
  }

  @Test
  public void provideConnectTimeoutSeconds_withCliOption_usesCliValue() {
    cliOptions.connectTimeoutSeconds = 20;
    configProperties.connectTimeoutSeconds = 15;

    Injector injector = Guice.createInjector(getTestingModule());

    assertThat(injector.getInstance(Key.get(int.class, ConnectTimeoutSeconds.class))).isEqualTo(20);
  }

  @Test
  public void provideConnectTimeoutSeconds_withConfigOnly_usesConfigValue() {
    configProperties.connectTimeoutSeconds = 15;

    Injector injector = Guice.createInjector(getTestingModule());

    assertThat(injector.getInstance(Key.get(int.class, ConnectTimeoutSeconds.class))).isEqualTo(15);
  }

  @Test
  public void provideConnectTimeoutSeconds_withNoConfig_usesDefault() {
    Injector injector = Guice.createInjector(getTestingModule());

    assertThat(injector.getInstance(Key.get(int.class, ConnectTimeoutSeconds.class))).isEqualTo(10);
  }

  @Test
  public void provideReadTimeoutSeconds_withCliOption_usesCliValue() {
    cliOptions.readTimeoutSeconds = 60;
    configProperties.readTimeoutSeconds = 45;

    Injector injector = Guice.createInjector(getTestingModule());

    assertThat(injector.getInstance(Key.get(int.class, ReadTimeoutSeconds.class))).isEqualTo(60);
  }

  @Test
  public void provideReadTimeoutSeconds_withConfigOnly_usesConfigValue() {
    configProperties.readTimeoutSeconds = 45;

    Injector injector = Guice.createInjector(getTestingModule());

    assertThat(injector.getInstance(Key.get(int.class, ReadTimeoutSeconds.class))).isEqualTo(45);
  }

  @Test
  public void provideReadTimeoutSeconds_withNoConfig_usesDefault() {
    Injector injector = Guice.createInjector(getTestingModule());

    assertThat(injector.getInstance(Key.get(int.class, ReadTimeoutSeconds.class))).isEqualTo(30);
  }

  @Test
  public void provideTrustAllCertificates_withCliOptionTrue_returnsTrue() {
    cliOptions.trustAllCertificates = true;
    configProperties.trustAllCertificates = false;

    Injector injector = Guice.createInjector(getTestingModule());

    assertThat(injector.getInstance(Key.get(boolean.class, TrustAllCertificates.class))).isTrue();
  }

  @Test
  public void provideTrustAllCertificates_withCliOptionFalse_returnsFalse() {
    cliOptions.trustAllCertificates = false;

    Injector injector = Guice.createInjector(getTestingModule());

    assertThat(injector.getInstance(Key.get(boolean.class, TrustAllCertificates.class))).isFalse();
  }

  @Test
  public void provideTrustAllCertificates_withConfigOnly_usesConfigValue() {
    configProperties.trustAllCertificates = false;

    Injector injector = Guice.createInjector(getTestingModule());

    assertThat(injector.getInstance(Key.get(boolean.class, TrustAllCertificates.class))).isFalse();
  }

  @Test
  public void provideTrustAllCertificates_withNoConfig_usesDefaultTrue() {
    Injector injector = Guice.createInjector(getTestingModule());

    assertThat(injector.getInstance(Key.get(boolean.class, TrustAllCertificates.class))).isTrue();
  }

  @Test
  public void provideTsunamiSocketFactory_withCustomConfig_usesCustomValues() throws Exception {
    cliOptions.connectTimeoutSeconds = 5;
    cliOptions.readTimeoutSeconds = 15;

    Injector injector = Guice.createInjector(getTestingModule());
    TsunamiSocketFactory factory = injector.getInstance(TsunamiSocketFactory.class);

    assertThat(factory.getDefaultConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
    assertThat(factory.getDefaultReadTimeout()).isEqualTo(Duration.ofSeconds(15));
  }

  private AbstractModule getTestingModule() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        install(new TsunamiSocketFactoryModule());
        bind(TsunamiSocketFactoryCliOptions.class).toInstance(cliOptions);
        bind(TsunamiSocketFactoryConfigProperties.class).toInstance(configProperties);
      }
    };
  }
}
