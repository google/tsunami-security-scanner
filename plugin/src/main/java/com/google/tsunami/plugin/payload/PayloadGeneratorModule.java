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
package com.google.tsunami.plugin.payload;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.tsunami.common.net.http.HttpClient;
import com.google.tsunami.plugin.TcsClient;
import com.google.tsunami.plugin.TcsConfigProperties;
import java.security.SecureRandom;

/** Guice module for installing {@link PayloadGenerator}. */
public final class PayloadGeneratorModule extends AbstractModule {
  private final SecureRandom secureRng;

  public PayloadGeneratorModule(SecureRandom secureRng) {
    this.secureRng = secureRng;
  }

  @Provides
  TcsClient providesTcsClient(TcsConfigProperties config, HttpClient httpClient) {
    // when all tcs config are not set, we provide an invalid {@link TcsClient}
    // so that {@link TcsClient#isCallbackServerEnabled} returns false.
    if (config.callbackAddress == null
        && config.callbackPort == null
        && config.pollingUri == null) {
      return new TcsClient("", 0, "", checkNotNull(httpClient));
    }

    checkNotNull(config.callbackAddress);
    checkNotNull(config.callbackPort);
    checkNotNull(config.pollingUri);
    checkArgument(
        InetAddresses.isInetAddress(config.callbackAddress)
            || InternetDomainName.isValid(config.callbackAddress),
        "Invalid callback address specified");
    checkArgument(
        config.callbackPort > 0 && config.callbackPort < 65536, "Invalid port number specified");

    return new TcsClient(
        config.callbackAddress, config.callbackPort, config.pollingUri, checkNotNull(httpClient));
  }

  @Provides
  PayloadSecretGenerator providesPayloadSecretGenerator() {
    return new PayloadSecretGenerator(this.secureRng);
  }
}
