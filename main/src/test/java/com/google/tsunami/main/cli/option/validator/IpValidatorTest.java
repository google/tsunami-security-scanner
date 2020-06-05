/*
 * Copyright 2020 Google LLC
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
package com.google.tsunami.main.cli.option.validator;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.beust.jcommander.ParameterException;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

/** Base class for IP validators. */
public abstract class IpValidatorTest {

  @Test
  public void validate_withValidIpValue_doesNotThrows() {
    for (String validIp : validIps()) {
      try {
        getValidator().validate(flagName(), validIp);
      } catch (ParameterException e) {
        throw new AssertionError("Unexpected ParameterException for IP: " + validIp, e);
      }
    }
  }

  @Test
  public void validate_withInvalidIpValue_throwsParameterException() {
    for (String invalidIp : invalidIps()) {
      ParameterException exception =
          assertThrows(
              ParameterException.class, () -> getValidator().validate(flagName(), invalidIp));
      assertThat(exception)
          .hasMessageThat()
          .isEqualTo(
              String.format(
                  "Parameter %s should point to a valid IP v%d address, got '%s'",
                  flagName(), getValidator().ipVersion(), invalidIp));
    }
  }

  protected abstract String flagName();

  protected abstract IpValidator getValidator();

  protected abstract ImmutableList<String> validIps();

  protected abstract ImmutableList<String> invalidIps();
}
