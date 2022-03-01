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
import com.google.tsunami.plugin.payload.testing.PayloadTestHelper;
import com.google.tsunami.proto.PayloadGeneratorConfig;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
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
  private static final PayloadGeneratorConfig LINUX_REFLECTIVE_RCE_CONFIG =
      PayloadGeneratorConfig.newBuilder()
          .setVulnerabilityType(PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE)
          .setInterpretationEnvironment(
              PayloadGeneratorConfig.InterpretationEnvironment.LINUX_SHELL)
          .setExecutionEnvironment(
              PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT)
          .build();
  private static final PayloadGeneratorConfig JAVA_REFLECTIVE_RCE_CONFIG =
      PayloadGeneratorConfig.newBuilder()
          .setVulnerabilityType(PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE)
          .setInterpretationEnvironment(PayloadGeneratorConfig.InterpretationEnvironment.JAVA)
          .setExecutionEnvironment(
              PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT)
          .build();
  private static final PayloadGeneratorConfig ANY_SSRF_CONFIG =
      PayloadGeneratorConfig.newBuilder()
          .setVulnerabilityType(PayloadGeneratorConfig.VulnerabilityType.SSRF)
          .setInterpretationEnvironment(
              PayloadGeneratorConfig.InterpretationEnvironment.INTERPRETATION_ANY)
          .setExecutionEnvironment(PayloadGeneratorConfig.ExecutionEnvironment.EXEC_ANY).build();
  private static final String CORRECT_PRINTF =
      "printf %s%s%s TSUNAMI_PAYLOAD_START ffffffffffffffff TSUNAMI_PAYLOAD_END";

  @Before
  public void setUp() throws IOException {
    mockCallbackServer = new MockWebServer();
    mockCallbackServer.start();
    Guice.createInjector(
            new HttpClientModule.Builder().build(),
            FakePayloadGeneratorModule.builder()
                .setCallbackServer(mockCallbackServer)
                .setSecureRng(testSecureRandom)
                .build())
        .injectMembers(this);
  }

  @Test
  public void isCallbackServerEnabled_withConfiguredCallbackServer_returnsTrue() {
    assertTrue(payloadGenerator.isCallbackServerEnabled());
  }

  @Test
  public void isCallbackServerEnabled_withUnconfiguredCallbackServer_returnsFalse() {
    // Replace PayloadGenerator with a version without a configured callback server
    Guice.createInjector(
            new HttpClientModule.Builder().build(),
            FakePayloadGeneratorModule.builder().setSecureRng(testSecureRandom).build())
        .injectMembers(this);

    assertFalse(payloadGenerator.isCallbackServerEnabled());
  }

  @Test
  public void generate_withLinuxConfiguration_andCallbackServer_returnsCurlPayload() {
    Payload payload =
        payloadGenerator.generate(
            LINUX_REFLECTIVE_RCE_CONFIG.toBuilder().setUseCallbackServer(true).build());

    assertThat(payload.getPayload()).contains("curl");
    assertThat(payload.getPayload()).contains(mockCallbackServer.getHostName());
    assertThat(payload.getPayload()).contains(Integer.toString(mockCallbackServer.getPort(), 10));
    assertTrue(payload.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void
      checkIfExecuted_withLinuxConfiguration_andCallbackServer_andExecutedCallbackUrl_returnsTrue()
          throws IOException {

    mockCallbackServer.enqueue(PayloadTestHelper.generateMockSuccessfulCallbackResponse());
    Payload payload =
        payloadGenerator.generate(
            LINUX_REFLECTIVE_RCE_CONFIG.toBuilder().setUseCallbackServer(true).build());

    assertTrue(payload.checkIfExecuted());
  }

  @Test
  public void
      checkIfExecuted_withLinuxConfiguration_andCallbackServer_andNotExecutedCallbackUrl_returnsFalse() {

    mockCallbackServer.enqueue(PayloadTestHelper.generateMockUnsuccessfulCallbackResponse());
    Payload payload =
        payloadGenerator.generate(
            LINUX_REFLECTIVE_RCE_CONFIG.toBuilder().setUseCallbackServer(true).build());

    assertFalse(payload.checkIfExecuted());
  }

  @Test
  public void getPayload_withLinuxConfiguration_andNoCallbackServer_returnsPrintfPayload() {
    Payload payload =
        payloadGenerator.generate(
            LINUX_REFLECTIVE_RCE_CONFIG.toBuilder().setUseCallbackServer(false).build());

    assertThat(payload.getPayload()).contains(CORRECT_PRINTF);
    assertFalse(payload.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void
      getPayload_withLinuxConfiguration_andUnconfiguredCallbackServer_returnsPrintfPayload() {

    // Replace PayloadGenerator with a version without a configured callback server
    Guice.createInjector(
            new HttpClientModule.Builder().build(),
            FakePayloadGeneratorModule.builder().setSecureRng(testSecureRandom).build())
        .injectMembers(this);

    Payload payload =
        payloadGenerator.generate(
            LINUX_REFLECTIVE_RCE_CONFIG.toBuilder().setUseCallbackServer(true).build());

    assertThat(payload.getPayload()).contains(CORRECT_PRINTF);
    assertFalse(payload.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void
      checkIfExecuted_withLinuxConfiguration_andNoCallbackServer_andCorrectInput_returnsTrue() {
    Payload payload =
        payloadGenerator.generate(
            LINUX_REFLECTIVE_RCE_CONFIG.toBuilder().setUseCallbackServer(false).build());

    assertTrue(
        payload.checkIfExecuted(
            ByteString.copyFromUtf8(
                "RANDOMOUTPUTTSUNAMI_PAYLOAD_STARTffffffffffffffffTSUNAMI_PAYLOAD_END")));
  }

  @Test
  public void
      checkIfExecuted_withLinuxConfiguration_andNoCallbackServer_andIncorectInput_returnsFalse() {
    Payload payload =
        payloadGenerator.generate(
            LINUX_REFLECTIVE_RCE_CONFIG.toBuilder().setUseCallbackServer(false).build());

    assertFalse(payload.checkIfExecuted(ByteString.copyFromUtf8(CORRECT_PRINTF)));
  }

  @Test
  public void getPayload_withJavaConfiguration_andNoCallbackServer_returnsPrintfPayload() {
    Payload payload = payloadGenerator.generate(JAVA_REFLECTIVE_RCE_CONFIG);

    assertThat(payload.getPayload())
        .contains(
            "String.format(\"%s%s%s\", \"TSUNAMI_PAYLOAD_START\", \"ffffffffffffffff\","
                + " \"TSUNAMI_PAYLOAD_END\")");
    assertFalse(payload.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void
      checkIfExecuted_withJavaConfiguration_andNoCallbackServer_andCorrectInput_returnsTrue() {
    Payload payload = payloadGenerator.generate(JAVA_REFLECTIVE_RCE_CONFIG);

    assertTrue(
        payload.checkIfExecuted(
            ByteString.copyFromUtf8(
                "RANDOMOUTPUTTSUNAMI_PAYLOAD_STARTffffffffffffffffTSUNAMI_PAYLOAD_END")));
  }

  @Test
  public void
      checkIfExecuted_withJavaConfiguration_andNoCallbackServer_andIncorrectInput_returnsFalse() {
    Payload payload = payloadGenerator.generate(JAVA_REFLECTIVE_RCE_CONFIG);

    assertFalse(
        payload.checkIfExecuted(
            ByteString.copyFromUtf8("TSUNAMI_PAYLOAD_START ffffffffffffffff TSUNAMI_PAYLOAD_END")));
  }

  @Test
  public void getPayload_withSsrfConfiguration_andCallbackServer_returnsCallbackUrl() {
    Payload payload = payloadGenerator.generate(ANY_SSRF_CONFIG.toBuilder().setUseCallbackServer(true).build());

    assertTrue(payload.getPayloadAttributes().getUsesCallbackServer());
    assertThat(payload.getPayload()).contains(mockCallbackServer.getHostName());
    assertThat(payload.getPayload()).contains(Integer.toString(mockCallbackServer.getPort(), 10));
  }

  @Test
  public void checkIfExecuted_withSsrfConfiguration_andCallbackServer_andExecutedUrl_returnsTrue()
      throws IOException {
    mockCallbackServer.enqueue(PayloadTestHelper.generateMockSuccessfulCallbackResponse());
    Payload payload =
        payloadGenerator.generate(ANY_SSRF_CONFIG.toBuilder().setUseCallbackServer(true).build());

    assertTrue(payload.checkIfExecuted());
  }

  @Test
  public void getPayload_withSsrfConfiguration_andCallbackServer_andNotExecutedUrl_returnsFalse() {
    mockCallbackServer.enqueue(PayloadTestHelper.generateMockUnsuccessfulCallbackResponse());
    Payload payload =
        payloadGenerator.generate(ANY_SSRF_CONFIG.toBuilder().setUseCallbackServer(true).build());

    assertFalse(payload.checkIfExecuted());
  }

  @Test
  public void
      checkIfExecuted_withSsrfConfiguration_andNoCallbackServer_andCorrectInput_returnsTrue() {
    Payload payload =
        payloadGenerator.generate(ANY_SSRF_CONFIG.toBuilder().setUseCallbackServer(false).build());

    assertTrue(payload.checkIfExecuted("<title>Error 404 (Not Found)!!1</title>"));
  }

  @Test
  public void
      checkIfExecuted_withSsrfConfiguration_andNoCallbackServer_andIncorrectInput_returnsFalse() {
    Payload payload =
        payloadGenerator.generate(ANY_SSRF_CONFIG.toBuilder().setUseCallbackServer(false).build());

    assertFalse(payload.checkIfExecuted("404 not found"));
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
