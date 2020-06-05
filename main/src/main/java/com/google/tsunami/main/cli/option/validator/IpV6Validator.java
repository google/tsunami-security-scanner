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

import java.net.Inet6Address;
import java.net.InetAddress;

/** Command line flag validator for an IP v6 address. */
public class IpV6Validator extends IpValidator {

  @Override
  protected int ipVersion() {
    return 6;
  }

  @Override
  protected boolean shouldAccept(InetAddress inetAddress) {
    return inetAddress instanceof Inet6Address;
  }
}
