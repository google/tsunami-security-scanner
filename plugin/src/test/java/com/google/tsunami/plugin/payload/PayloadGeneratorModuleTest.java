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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.inject.Guice;
import com.google.tsunami.common.net.http.HttpClient;
import com.google.tsunami.common.net.http.HttpClientModule;
import com.google.tsunami.plugin.TcsClient;
import com.google.tsunami.plugin.TcsConfigProperties;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Tests for {@link PayloadGeneratorModule} */
@RunWith(Parameterized.class)
public final class PayloadGeneratorModuleTest {

  @Inject private HttpClient httpClient;

  @Before
  public void setUp() {
    Guice.createInjector(new HttpClientModule.Builder().build()).injectMembers(this);
  }

  @Test
  public void providesTcsClient_withNoConfig_returnsInvalidTcsClient() {
    PayloadGeneratorModule module = new PayloadGeneratorModule(new SecureRandom());
    TcsConfigProperties config = new TcsConfigProperties();
    config.callbackAddress = null;
    config.callbackPort = null;
    config.pollingUri = null;

    TcsClient client = module.providesTcsClient(config, httpClient);

    assertFalse(client.isCallbackServerEnabled());
  }

  @Test
  public void providesTcsClient_withGoodConfig_returnsValidTcsClient() {
    PayloadGeneratorModule module = new PayloadGeneratorModule(new SecureRandom());
    TcsConfigProperties config = new TcsConfigProperties();
    config.callbackAddress = "mydomain.com";
    config.callbackPort = 1111;
    config.pollingUri = "mydomain.com";

    TcsClient client = module.providesTcsClient(config, httpClient);

    assertTrue(client.isCallbackServerEnabled());
  }

  @Parameter(0)
  public String callbackAddress;

  @Parameter(1)
  public Integer callbackPort;

  @Parameter(2)
  public String pollingUri;

  @Parameter(3)
  public Class<Throwable> exceptionClass;

  @Parameters
  public static List<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {null, 1, "mydomain.com", NullPointerException.class},
          {"mydomain.com", null, "mydomain.com", NullPointerException.class},
          {"mydomain.com", 1, null, NullPointerException.class},
          {"mydomain.com", 0, "mydomain.com", IllegalArgumentException.class},
          {"a bad address", 1, "mydomain.com", IllegalArgumentException.class},
        });
  }

  @Test
  public void providesTcsClient_withBadConfig_throwsException() {
    PayloadGeneratorModule module = new PayloadGeneratorModule(new SecureRandom());
    TcsConfigProperties config = new TcsConfigProperties();
    config.callbackAddress = this.callbackAddress;
    config.callbackPort = this.callbackPort;
    config.pollingUri = this.pollingUri;

    assertThrows(this.exceptionClass, () -> module.providesTcsClient(config, httpClient));
  }

}
