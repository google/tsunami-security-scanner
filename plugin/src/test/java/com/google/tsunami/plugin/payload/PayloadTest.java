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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.inject.Guice;
import com.google.protobuf.ByteString;
import com.google.tsunami.common.net.http.HttpClient;
import com.google.tsunami.common.net.http.HttpClientModule;
import com.google.tsunami.plugin.TcsClient;
import com.google.tsunami.proto.PayloadAttributes;
import com.google.tsunami.proto.PayloadGeneratorConfig;
import java.security.SecureRandom;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Payload} class */
@RunWith(JUnit4.class)
public final class PayloadTest {

  private static final PayloadGeneratorConfig CONFIG = PayloadGeneratorConfig.getDefaultInstance();
  private static final PayloadSecretGenerator secretGenerator =
      new PayloadSecretGenerator(new SecureRandom());
  private static final PayloadAttributes PAYLOAD_ATTRIBUTES =
      PayloadAttributes.getDefaultInstance();
  @Inject private HttpClient httpClient;
  private TcsClient callbackServerClient;
  private Payload payload;

  @Before
  public void setUp() {
    Guice.createInjector(new HttpClientModule.Builder().build()).injectMembers(this);

    callbackServerClient = new TcsClient("", 0, "", httpClient);
    payload = new TestPayload(CONFIG, callbackServerClient, secretGenerator);
  }

  @Test
  public void setPayload_correctlySetsPayload() {
    Validator validator = (unused) -> false;
    BasePayload basePayload = BasePayload.create("my-payload", validator, PAYLOAD_ATTRIBUTES);
    payload.setBasePayload(basePayload);

    assertEquals(basePayload.getPayloadString(), payload.getPayload());
    assertEquals(basePayload.getValidator(), validator);
  }

  @Test
  public void checkIfExecuted_executesValidator() throws NoCallbackServerException {
    TestValidatorIsCalledValidator testValidator = new TestValidatorIsCalledValidator();
    BasePayload basePayload = BasePayload.create("my-payload", testValidator, PAYLOAD_ATTRIBUTES);
    payload.setBasePayload(basePayload);

    payload.checkIfExecuted(Optional.empty());
    assertTrue(testValidator.wasCalled);
  }

  @Test
  public void getPayloadAttributes_returnsPayloadAttributes() {
    Validator validator = (unused) -> false;
    BasePayload basePayload = BasePayload.create("my-payload", validator, PAYLOAD_ATTRIBUTES);
    payload.setBasePayload(basePayload);

    assertEquals(payload.getPayloadAttributes(), PAYLOAD_ATTRIBUTES);
  }

  private static final class TestPayload extends Payload {

    TestPayload(
        PayloadGeneratorConfig config,
        TcsClient callbackServerClient,
        PayloadSecretGenerator secretGenerator) {
      super(config, callbackServerClient, secretGenerator);
    }

    @Override
    void initialize() {}
  }

  private static final class TestValidatorIsCalledValidator implements Validator {
    public boolean wasCalled = false;

    @Override
    public boolean isExecuted(Optional<ByteString> input) {
      wasCalled = true;
      return false;
    }
  }
}
