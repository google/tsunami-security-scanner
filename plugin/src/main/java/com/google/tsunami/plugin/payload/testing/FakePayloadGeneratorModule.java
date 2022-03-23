/*
 * Copyright 2022 Google LLC
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

package com.google.tsunami.plugin.payload.testing;

import com.google.auto.value.AutoBuilder;
import com.google.inject.AbstractModule;
import com.google.tsunami.plugin.TcsConfigProperties;
import com.google.tsunami.plugin.payload.PayloadGenerator;
import com.google.tsunami.plugin.payload.PayloadGeneratorModule;
import java.security.SecureRandom;
import java.util.Optional;
import okhttp3.mockwebserver.MockWebServer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Guice module for interacting with {@link PayloadGenerator} in tests. Use {@link
 * FakePayloadGeneratorModule.Builder} instead of this directly.
 */
public final class FakePayloadGeneratorModule extends AbstractModule {
  private final TcsConfigProperties tcsConfig = new TcsConfigProperties();
  private final SecureRandom secureRng;

  /**
   * @param callbackServer - if supplied, enables the payload generator to use the callback server.
   *     If this behavior is unwanted, leave this empty.
   * @param secureRng - if you do not need control over the output of {@link SecureRandom#nextBytes}
   *     in tests, leave this empty.
   */
  FakePayloadGeneratorModule(
      Optional<MockWebServer> callbackServer, Optional<SecureRandom> secureRng) {

    this.tcsConfig.callbackAddress = callbackServer.map(c -> c.getHostName()).orElse(null);
    this.tcsConfig.callbackPort = callbackServer.map(c -> c.getPort()).orElse(null);
    this.tcsConfig.pollingUri = callbackServer.map(c -> c.url("/").toString()).orElse(null);
    this.secureRng = secureRng.orElse(new SecureRandom());
  }

  @Override
  protected void configure() {
    install(new PayloadGeneratorModule(secureRng));
    bind(TcsConfigProperties.class).toInstance(tcsConfig);
  }

  /**
   * Creates a builder for the {@link FakePayloadGeneratorModule}.
   *
   * @return a builder for configuring the module
   */
  public static Builder builder() {
    return Builder.builder();
  }

  static FakePayloadGeneratorModule build(
      @Nullable MockWebServer callbackServer, @Nullable SecureRandom secureRng) {
    return new FakePayloadGeneratorModule(
        Optional.ofNullable(callbackServer), Optional.ofNullable(secureRng));
  }

  /** Configures {@link FakePayloadGeneratorModule}. */
  @AutoBuilder(callMethod = "build")
  public abstract static class Builder {
    public static Builder builder() {
      return new AutoBuilder_FakePayloadGeneratorModule_Builder();
    }

    public abstract Builder setCallbackServer(MockWebServer callbackServer);

    public abstract Builder setSecureRng(SecureRandom secureRng);

    public abstract FakePayloadGeneratorModule build();
  }
}
