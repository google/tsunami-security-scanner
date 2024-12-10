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
import java.security.SecureRandom;
import java.util.Arrays;
import javax.inject.Inject;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link PayloadGenerator} for cases which do not utilize the callback server. */
@RunWith(JUnit4.class)
public final class PayloadGeneratorWithoutCallbackServerTest {

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
  private static final PayloadGeneratorConfig LINUX_ARBITRARY_FILE_WRITE_CRON_CONFIG =
      PayloadGeneratorConfig.newBuilder()
          .setVulnerabilityType(PayloadGeneratorConfig.VulnerabilityType.ARBITRARY_FILE_WRITE)
          .setInterpretationEnvironment(
              PayloadGeneratorConfig.InterpretationEnvironment.LINUX_ROOT_CRONTAB)
          .setExecutionEnvironment(
              PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT)
          .build();
  private static final PayloadGeneratorConfig LINUX_BLIND_RCE_FILE_READ_CONFIG =
      PayloadGeneratorConfig.newBuilder()
          .setVulnerabilityType(PayloadGeneratorConfig.VulnerabilityType.BLIND_RCE_FILE_READ)
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
  private static final PayloadGeneratorConfig JSP_REFLECTIVE_RCE_CONFIG =
      PayloadGeneratorConfig.newBuilder()
          .setVulnerabilityType(PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE)
          .setInterpretationEnvironment(PayloadGeneratorConfig.InterpretationEnvironment.JSP)
          .setExecutionEnvironment(
              PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT)
          .build();
  private static final PayloadGeneratorConfig WINDOWS_REFLECTIVE_RCE_CONFIG =
      PayloadGeneratorConfig.newBuilder()
          .setVulnerabilityType(PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE)
          .setInterpretationEnvironment(
              PayloadGeneratorConfig.InterpretationEnvironment.WINDOWS_SHELL)
          .setExecutionEnvironment(
              PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT)
          .build();
  private static final PayloadGeneratorConfig ANY_SSRF_CONFIG =
      PayloadGeneratorConfig.newBuilder()
          .setVulnerabilityType(PayloadGeneratorConfig.VulnerabilityType.SSRF)
          .setInterpretationEnvironment(
              PayloadGeneratorConfig.InterpretationEnvironment.INTERPRETATION_ANY)
          .setExecutionEnvironment(PayloadGeneratorConfig.ExecutionEnvironment.EXEC_ANY)
          .build();
  private static final String CORRECT_PRINTF =
      "printf %s%s%s TSUNAMI_PAYLOAD_START ffffffffffffffff TSUNAMI_PAYLOAD_END";
  private static final String CORRECT_CURL_TRACE =
      "curl --trace /tmp/tsunami-rce -- tsunami-rce-ffffffffffffffff";
  private static final String CORRECT_WINDOWS_ECHO =
      "powershell -Command \"echo TSUNAMI_PAYLOAD_START$(echo"
          + " ffffffffffffffff)TSUNAMI_PAYLOAD_END\"";

  @Before
  public void setUp() {
    Guice.createInjector(
            new HttpClientModule.Builder().build(),
            FakePayloadGeneratorModule.builder().setSecureRng(testSecureRandom).build())
        .injectMembers(this);
  }

  @Test
  public void isCallbackServerEnabled_returnsFalse() {
    assertFalse(payloadGenerator.isCallbackServerEnabled());
  }

  @Test
  public void getNonCallbackPayload_withLinuxConfiguration_returnsPrintfPayload() {
    Payload payload = payloadGenerator.generateNoCallback(LINUX_REFLECTIVE_RCE_CONFIG);

    assertThat(payload.getPayload()).isEqualTo(CORRECT_PRINTF);
    assertFalse(payload.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void getPayload_withLinuxConfiguration_returnsPrintfPayload() {
    Payload payload = payloadGenerator.generate(LINUX_REFLECTIVE_RCE_CONFIG);

    assertThat(payload.getPayload()).isEqualTo(CORRECT_PRINTF);
    assertFalse(payload.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void checkIfExecuted_withLinuxConfiguration_andCorrectInput_returnsTrue() {
    Payload payload = payloadGenerator.generate(LINUX_REFLECTIVE_RCE_CONFIG);

    assertTrue(
        payload.checkIfExecuted(
            ByteString.copyFromUtf8(
                "RANDOMOUTPUTTSUNAMI_PAYLOAD_STARTffffffffffffffffTSUNAMI_PAYLOAD_END")));
  }

  @Test
  public void checkIfExecuted_withLinuxConfiguration_andIncorectInput_returnsFalse() {
    Payload payload = payloadGenerator.generate(LINUX_REFLECTIVE_RCE_CONFIG);

    assertFalse(payload.checkIfExecuted(ByteString.copyFromUtf8(CORRECT_PRINTF)));
  }

  @Test
  public void generateNonCallbackPayload_withCrontabConfiguration_throwsNotImplementedException() {

    assertThrows(
        NotImplementedException.class,
        () -> payloadGenerator.generateNoCallback(LINUX_ARBITRARY_FILE_WRITE_CRON_CONFIG));
  }

  @Test
  public void getNonCallbackPayload_withBlindRceReadConfiguration_returnsCurlTracePayload() {
    Payload payload = payloadGenerator.generateNoCallback(LINUX_BLIND_RCE_FILE_READ_CONFIG);

    assertThat(payload.getPayload()).isEqualTo(CORRECT_CURL_TRACE);
    assertFalse(payload.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void getPayload_withBlindRceReadConfiguration_returnsCurlTracePayload() {
    Payload payload = payloadGenerator.generate(LINUX_BLIND_RCE_FILE_READ_CONFIG);

    assertThat(payload.getPayload()).isEqualTo(CORRECT_CURL_TRACE);
    assertFalse(payload.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void checkIfExecuted_withBlindRceReadConfiguration_andCorrectInput_returnsTrue() {
    Payload payload = payloadGenerator.generate(LINUX_BLIND_RCE_FILE_READ_CONFIG);

    assertTrue(
        payload.checkIfExecuted(
            ByteString.copyFromUtf8("RANDOMOUTPUTtsunami-rce-ffffffffffffffff")));
  }

  @Test
  public void checkIfExecuted_withBlindRceReadConfiguration_andIncorectInput_returnsFalse() {
    Payload payload = payloadGenerator.generate(LINUX_BLIND_RCE_FILE_READ_CONFIG);

    assertFalse(payload.checkIfExecuted(ByteString.copyFromUtf8("RANDOMINPUT")));
  }

  @Test
  public void getNonCallbackPayload_withWindowsConfiguration_returnsPrintfPayload() {
    Payload payload = payloadGenerator.generateNoCallback(WINDOWS_REFLECTIVE_RCE_CONFIG);

    assertThat(payload.getPayload()).isEqualTo(CORRECT_WINDOWS_ECHO);
    assertFalse(payload.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void getPayload_withWindowsConfiguration_returnsEchoPayload() {
    Payload payload = payloadGenerator.generate(WINDOWS_REFLECTIVE_RCE_CONFIG);

    assertThat(payload.getPayload()).isEqualTo(CORRECT_WINDOWS_ECHO);
    assertFalse(payload.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void checkIfExecuted_withWindowsConfiguration_andCorrectInput_returnsTrue() {
    Payload payload = payloadGenerator.generate(WINDOWS_REFLECTIVE_RCE_CONFIG);

    assertTrue(
        payload.checkIfExecuted(
            ByteString.copyFromUtf8(
                "RANDOMOUTPUTTSUNAMI_PAYLOAD_STARTffffffffffffffffTSUNAMI_PAYLOAD_END")));
  }

  @Test
  public void checkIfExecuted_withWindowsConfiguration_andIncorectInput_returnsFalse() {
    Payload payload = payloadGenerator.generate(WINDOWS_REFLECTIVE_RCE_CONFIG);

    assertFalse(payload.checkIfExecuted(ByteString.copyFromUtf8(CORRECT_PRINTF)));
  }

  @Test
  public void getPayload_withJavaConfiguration_returnsPrintfPayload() {
    Payload payload = payloadGenerator.generate(JAVA_REFLECTIVE_RCE_CONFIG);

    assertThat(payload.getPayload()).isEqualTo(
            "String.format(\"%s%s%s\", \"TSUNAMI_PAYLOAD_START\", \"ffffffffffffffff\","
                + " \"TSUNAMI_PAYLOAD_END\")");
    assertFalse(payload.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void checkIfExecuted_withJavaConfiguration_andCorrectInput_returnsTrue() {
    Payload payload = payloadGenerator.generate(JAVA_REFLECTIVE_RCE_CONFIG);

    assertTrue(
        payload.checkIfExecuted(
            ByteString.copyFromUtf8(
                "RANDOMOUTPUTTSUNAMI_PAYLOAD_STARTffffffffffffffffTSUNAMI_PAYLOAD_END")));
  }

  @Test
  public void checkIfExecuted_withJavaConfiguration_andIncorrectInput_returnsFalse() {
    Payload payload = payloadGenerator.generate(JAVA_REFLECTIVE_RCE_CONFIG);

    assertFalse(
        payload.checkIfExecuted(
            ByteString.copyFromUtf8("TSUNAMI_PAYLOAD_START ffffffffffffffff TSUNAMI_PAYLOAD_END")));
  }

  @Test
  public void getPayload_withJspConfiguration_returnsPrintfPayload() {
    Payload payload = payloadGenerator.generate(JSP_REFLECTIVE_RCE_CONFIG);

    assertThat(payload.getPayload())
        .isEqualTo(
            "<% out.print(String.format(\"%s%s%s\",\"TSUNAMI_PAYLOAD_START\", \"ffffffffffffffff\","
                + " \"TSUNAMI_PAYLOAD_END\")); %>");
    assertFalse(payload.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void checkIfExecuted_withJspConfiguration_andCorrectInput_returnsTrue() {
    Payload payload = payloadGenerator.generate(JSP_REFLECTIVE_RCE_CONFIG);

    assertTrue(
        payload.checkIfExecuted(
            ByteString.copyFromUtf8(
                "RANDOMOUTPUTTSUNAMI_PAYLOAD_STARTffffffffffffffffTSUNAMI_PAYLOAD_END")));
  }

  @Test
  public void checkIfExecuted_withJspConfiguration_andIncorrectInput_returnsFalse() {
    Payload payload = payloadGenerator.generate(JSP_REFLECTIVE_RCE_CONFIG);

    assertFalse(
        payload.checkIfExecuted(
            ByteString.copyFromUtf8("TSUNAMI_PAYLOAD_START ffffffffffffffff TSUNAMI_PAYLOAD_END")));
  }

  @Test
  public void getPayload_withSsrfConfiguration_returnsGooglePayload() {
    Payload payload = payloadGenerator.generate(ANY_SSRF_CONFIG);

    assertThat(payload.getPayload()).isEqualTo("http://public-firing-range.appspot.com/");
    assertFalse(payload.getPayloadAttributes().getUsesCallbackServer());
  }

  @Test
  public void checkIfExecuted_withSsrfConfiguration_andCorrectInput_returnsTrue() {
    Payload payload = payloadGenerator.generate(ANY_SSRF_CONFIG);

    assertTrue(payload.checkIfExecuted("<h1>What is the Firing Range?</h1>"));
  }

  @Test
  public void checkIfExecuted_withSsrfConfiguration_andIncorrectInput_returnsFalse() {
    Payload payload = payloadGenerator.generate(ANY_SSRF_CONFIG);

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
