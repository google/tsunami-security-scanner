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
package com.google.tsunami.plugin.payload;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.inject.Guice;
import com.google.protobuf.ByteString;
import com.google.tsunami.common.net.http.HttpClientModule;
import com.google.tsunami.plugin.payload.testing.FakePayloadGeneratorModule;
import com.google.tsunami.proto.PayloadGeneratorConfig;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;
import javax.inject.Inject;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ReflectiveRcePayload}. */
@RunWith(JUnit4.class)
public final class ReflectiveRcePayloadTest {

  @Inject private PayloadGenerator payloadGenerator;
  private MockWebServer mockCallbackServer;
  private PayloadGeneratorConfig.Builder defaultLinuxPayloadConfig;

  // Stub the nextBytes call to give a predictable output to assert against.
  private final SecureRandom mockSecureRandom =
      new SecureRandom() {
        @Override
        public void nextBytes(byte[] bytes) {
          Arrays.fill(bytes, (byte) 0xFF);
        }
      };

  private static final String CORRECT_PRINTF =
      "printf %s%s%s TSUNAMI_PAYLOAD_START ffffffffffffffff TSUNAMI_PAYLOAD_END";

  @Before
  public void setUp() throws IOException {

    mockCallbackServer = new MockWebServer();
    mockCallbackServer.start();

    defaultLinuxPayloadConfig =
        PayloadGeneratorConfig.newBuilder()
            .setVulnerabilityType(PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE)
            .setInterpretationEnvironment(
                PayloadGeneratorConfig.InterpretationEnvironment.LINUX_SHELL)
            .setExecutionEnvironment(
                PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT);

    Guice.createInjector(
            new HttpClientModule.Builder().build(),
            new FakePayloadGeneratorModule(mockCallbackServer, true, mockSecureRandom))
        .injectMembers(this);
  }

  @Test
  public void getPayload_withLinuxConfiguration_andCallbackServer_returnsCurlPayload()
      throws NotImplementedException {
    Payload p =
        payloadGenerator.generate(defaultLinuxPayloadConfig.setUseCallbackServer(true).build());

    p.initialize();

    assertThat(p.getPayload()).contains("curl");
    assertThat(p.getPayload()).contains(mockCallbackServer.getHostName());
    assertThat(p.getPayload()).contains(Integer.toString(mockCallbackServer.getPort(), 10));
    assertTrue(p.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void getPayload_withLinuxConfiguration_andNoCallbackServer_returnsPrintfPayload()
      throws NotImplementedException {

    Payload p =
        payloadGenerator.generate(defaultLinuxPayloadConfig.setUseCallbackServer(false).build());

    p.initialize();

    assertThat(p.getPayload()).contains(CORRECT_PRINTF);
    assertFalse(p.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void getPayload_withLinuxConfiguration_andUnconfiguredCallbackServer_returnsPrintfPayload()
      throws NotImplementedException {

    // Replace PayloadGenerator with a version without a configured callback server
    Guice.createInjector(
            new HttpClientModule.Builder().build(),
            new FakePayloadGeneratorModule(mockCallbackServer, false, mockSecureRandom))
        .injectMembers(this);

    Payload p =
        payloadGenerator.generate(defaultLinuxPayloadConfig.setUseCallbackServer(true).build());

    p.initialize();

    assertThat(p.getPayload()).contains(CORRECT_PRINTF);
    assertFalse(p.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void
      checkIfExecuted_withLinuxConfiguration_andNoCallbackServer_andCorrectInput_returnsTrue()
          throws NotImplementedException, NoCallbackServerException {

    Payload p =
        payloadGenerator.generate(defaultLinuxPayloadConfig.setUseCallbackServer(false).build());

    p.initialize();

    assertTrue(
        p.checkIfExecuted(
            Optional.of(
                ByteString.copyFromUtf8(
                    "RANDOMOUTPUTTSUNAMI_PAYLOAD_STARTffffffffffffffffTSUNAMI_PAYLOAD_END"))));
  }

  @Test
  public void
      checkIfExecuted_withLinuxConfiguration_andNoCallbackServer_andIncorectInput_returnsFalse()
          throws NotImplementedException, NoCallbackServerException {
    Payload p =
        payloadGenerator.generate(defaultLinuxPayloadConfig.setUseCallbackServer(false).build());

    p.initialize();

    assertFalse(p.checkIfExecuted(Optional.of(ByteString.copyFromUtf8(CORRECT_PRINTF))));
  }
}
