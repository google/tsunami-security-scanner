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

import com.google.inject.AbstractModule;
import com.google.tsunami.plugin.TcsConfigProperties;
import com.google.tsunami.plugin.payload.PayloadGenerator;
import com.google.tsunami.plugin.payload.PayloadGeneratorModule;
import java.security.SecureRandom;
import okhttp3.mockwebserver.MockWebServer;

/** Guice module for interacting with {@link PayloadGenerator} in tests */
public final class FakePayloadGeneratorModule extends AbstractModule {
  private final TcsConfigProperties config;
  private final SecureRandom secureRng;

  public FakePayloadGeneratorModule(
      MockWebServer callbackServer, boolean callbackServerIsEnabled, SecureRandom secureRng) {
    this.config = new TcsConfigProperties();
    this.secureRng = secureRng;

    if (callbackServerIsEnabled) {
      this.config.callbackAddress = callbackServer.getHostName();
      this.config.callbackPort = callbackServer.getPort();
      this.config.pollingUri = callbackServer.url("/").toString();
    } else {
      this.config.callbackAddress = null;
      this.config.callbackPort = null;
      this.config.pollingUri = null;
    }
  }

  @Override
  protected void configure() {
    install(new PayloadGeneratorModule(this.secureRng));
    bind(TcsConfigProperties.class).toInstance(this.config);
  }
}
