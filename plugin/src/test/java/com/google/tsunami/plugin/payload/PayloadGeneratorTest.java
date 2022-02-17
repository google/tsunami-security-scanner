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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
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

/** Tests for {@link PayloadGenerator}. */
@RunWith(JUnit4.class)
public final class PayloadGeneratorTest {

  @Inject private PayloadGenerator payloadGenerator;
  private MockWebServer mockCallbackServer;
  private final SecureRandom testSecureRandom =
      new SecureRandom() {
        @Override
        public void nextBytes(byte[] bytes) {
          Arrays.fill(bytes, (byte) 0xFF);
        }
      };
  private final PayloadGeneratorConfig.Builder defaultLinuxPayloadConfig =
      PayloadGeneratorConfig.newBuilder()
          .setVulnerabilityType(PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE)
          .setInterpretationEnvironment(
              PayloadGeneratorConfig.InterpretationEnvironment.LINUX_SHELL)
          .setExecutionEnvironment(
              PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT);

  private static final String CORRECT_PRINTF =
      "printf %s%s%s TSUNAMI_PAYLOAD_START ffffffffffffffff TSUNAMI_PAYLOAD_END";

  @Before
  public void setUp() throws IOException {
    mockCallbackServer = new MockWebServer();
    mockCallbackServer.start();
    Guice.createInjector(
            new HttpClientModule.Builder().build(),
            new FakePayloadGeneratorModule(mockCallbackServer, true, testSecureRandom))
        .injectMembers(this);
  }

  @Test
  public void generate_withLinuxConfiguration_andCallbackServer_returnsCurlPayload()
      throws NotImplementedException {
    Payload p =
        payloadGenerator.generate(defaultLinuxPayloadConfig.setUseCallbackServer(true).build());

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

    assertThat(p.getPayload()).contains(CORRECT_PRINTF);
    assertFalse(p.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void getPayload_withLinuxConfiguration_andUnconfiguredCallbackServer_returnsPrintfPayload()
      throws NotImplementedException {

    // Replace PayloadGenerator with a version without a configured callback server
    Guice.createInjector(
            new HttpClientModule.Builder().build(),
            new FakePayloadGeneratorModule(mockCallbackServer, false, this.testSecureRandom))
        .injectMembers(this);

    Payload p =
        payloadGenerator.generate(defaultLinuxPayloadConfig.setUseCallbackServer(true).build());

    assertThat(p.getPayload()).contains(CORRECT_PRINTF);
    assertFalse(p.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void
      checkIfExecuted_withLinuxConfiguration_andNoCallbackServer_andCorrectInput_returnsTrue()
          throws NotImplementedException, NoCallbackServerException {

    Payload p =
        payloadGenerator.generate(defaultLinuxPayloadConfig.setUseCallbackServer(false).build());

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

    assertFalse(p.checkIfExecuted(Optional.of(ByteString.copyFromUtf8(CORRECT_PRINTF))));
  }

  @Test
  public void generate_withoutVulnerabilityType_throwsNotImplementedException() {
    assertThrows(
        NotImplementedException.class,
        () ->
            payloadGenerator.generate(
                PayloadGeneratorConfig.newBuilder()
                    .setInterpretationEnvironment(
                        PayloadGeneratorConfig.InterpretationEnvironment.LINUX_SHELL)
                    .setExecutionEnvironment(
                        PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT)
                    .build()));
  }

  @Test
  public void generate_withoutInterpretationEnvironment_throwsNotImplementedException() {
    assertThrows(
        NotImplementedException.class,
        () ->
            payloadGenerator.generate(
                PayloadGeneratorConfig.newBuilder()
                    .setVulnerabilityType(PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE)
                    .setExecutionEnvironment(
                        PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT)
                    .build()));
  }

  @Test
  public void generate_withoutExecutionEnvironment_throwsNotImplementedException() {
    assertThrows(
        NotImplementedException.class,
        () ->
            payloadGenerator.generate(
                PayloadGeneratorConfig.newBuilder()
                    .setVulnerabilityType(PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE)
                    .setInterpretationEnvironment(
                        PayloadGeneratorConfig.InterpretationEnvironment.LINUX_SHELL)
                    .build()));
  }

  @Test
  public void generate_withoutConfig_throwsNotImplementedException() {
    assertThrows(
        NotImplementedException.class,
        () -> payloadGenerator.generate(PayloadGeneratorConfig.getDefaultInstance()));
  }

}
