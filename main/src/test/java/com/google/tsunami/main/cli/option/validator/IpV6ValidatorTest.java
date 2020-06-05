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

import com.google.common.collect.ImmutableList;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link IpV6Validator}. */
@RunWith(JUnit4.class)
public class IpV6ValidatorTest extends IpValidatorTest {

  @Override
  protected String flagName() {
    return "ip-v6-target";
  }

  @Override
  protected IpValidator getValidator() {
    return new IpV6Validator();
  }

  @Override
  protected ImmutableList<String> validIps() {
    return ImmutableList.of(
        "0:0:0:0:0:0:0:1",
        "fe80::a",
        "fe80::1",
        "fe80::2",
        "fe80::42",
        "fe80::3dd0:7f8e:57b7:34d5",
        "fe80:3dd0:7f8e:57b7:0:0:0:0",
        "::4:0:0:0:ffff",
        "0:0:3::ffff",
        "7::0.128.0.127");
  }

  @Override
  protected ImmutableList<String> invalidIps() {
    return ImmutableList.of(
        "", "bogus_string", "1234", "127.0.0.1", "www.google.com", "[1:2e]", "[fe80:a");
  }
}
