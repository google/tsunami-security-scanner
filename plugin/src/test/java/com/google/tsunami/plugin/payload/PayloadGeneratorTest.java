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
import static org.junit.Assert.assertThrows;

import com.google.inject.Guice;
import com.google.tsunami.common.net.http.HttpClientModule;
import com.google.tsunami.proto.PayloadGeneratorConfig;
import java.security.SecureRandom;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link PayloadGenerator}. */
@RunWith(JUnit4.class)
public final class PayloadGeneratorTest {

  @Inject private PayloadGenerator payloadGenerator;

  @Before
  public void setUp() {
    Guice.createInjector(
            new HttpClientModule.Builder().build(), new PayloadGeneratorModule(new SecureRandom()))
        .injectMembers(this);
  }

  @Test
  public void generate_withReflectiveRce_returnsReflectiveRcePayload()
      throws NotImplementedException {
    Payload p =
        payloadGenerator.generate(
            PayloadGeneratorConfig.newBuilder()
                .setVulnerabilityType(PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE)
                .setInterpretationEnvironment(
                    PayloadGeneratorConfig.InterpretationEnvironment.LINUX_SHELL)
                .setExecutionEnvironment(
                    PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT)
                .build());

    assertThat(p).isInstanceOf(ReflectiveRcePayload.class);
    // Verify that the payload was initialized
    assertThat(p.getPayload()).isNotEmpty();
  }

  @Test
  public void generate_withoutVulnerabilityType_throwsNotImplementedException() {
    assertThrows(
        NotImplementedException.class,
        () -> payloadGenerator.generate(PayloadGeneratorConfig.getDefaultInstance()));
  }
}
