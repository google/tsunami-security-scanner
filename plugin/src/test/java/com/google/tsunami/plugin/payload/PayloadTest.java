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

import com.google.protobuf.ByteString;
import com.google.tsunami.proto.PayloadAttributes;
import com.google.tsunami.proto.PayloadGeneratorConfig;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Payload} class */
@RunWith(JUnit4.class)
public final class PayloadTest {

  private static final PayloadGeneratorConfig CONFIG = PayloadGeneratorConfig.getDefaultInstance();
  private static final PayloadAttributes PAYLOAD_ATTRIBUTES =
      PayloadAttributes.getDefaultInstance();

  @Test
  public void getPayload_returnsPayloadString() {
    Validator validator = (unused) -> false;
    Payload payload = new Payload("my-payload", validator, PAYLOAD_ATTRIBUTES, CONFIG);

    assertEquals("my-payload", payload.getPayload());
  }

  @Test
  public void checkIfExecuted_withNoParameter_executesValidator() {
    TestValidatorIsCalledValidator testValidator = new TestValidatorIsCalledValidator();
    Payload payload = new Payload("my-payload", testValidator, PAYLOAD_ATTRIBUTES, CONFIG);

    payload.checkIfExecuted();
    assertTrue(testValidator.wasCalled);
  }

  @Test
  public void checkIfExecuted_withString_executesValidator() {
    TestValidatorIsCalledValidator testValidator = new TestValidatorIsCalledValidator();
    Payload payload = new Payload("my-payload", testValidator, PAYLOAD_ATTRIBUTES, CONFIG);

    payload.checkIfExecuted("my-input");
    assertTrue(testValidator.wasCalled);
  }

  @Test
  public void checkIfExecuted_withByteString_executesValidator() {
    TestValidatorIsCalledValidator testValidator = new TestValidatorIsCalledValidator();
    Payload payload = new Payload("my-payload", testValidator, PAYLOAD_ATTRIBUTES, CONFIG);

    payload.checkIfExecuted(ByteString.copyFromUtf8("my-input"));
    assertTrue(testValidator.wasCalled);
  }

  @Test
  public void checkIfExecuted_withOptional_executesValidator() {
    TestValidatorIsCalledValidator testValidator = new TestValidatorIsCalledValidator();
    Payload payload = new Payload("my-payload", testValidator, PAYLOAD_ATTRIBUTES, CONFIG);

    payload.checkIfExecuted(Optional.empty());
    assertTrue(testValidator.wasCalled);
  }

  @Test
  public void getPayloadAttributes_returnsPayloadAttributes() {
    Validator validator = (unused) -> false;
    Payload payload = new Payload("my-payload", validator, PAYLOAD_ATTRIBUTES, CONFIG);

    assertEquals(payload.getPayloadAttributes(), PAYLOAD_ATTRIBUTES);
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
