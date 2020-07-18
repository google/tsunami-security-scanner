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

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Strings;
import com.google.common.net.InetAddresses;
import java.net.InetAddress;

/** Base command line flag validator for an IP address. */
public abstract class IpValidator implements IParameterValidator {

  @Override
  public void validate(String name, String value) {
    if (value.contains("/")) {
      String[] parts = value.split("/");
      int subnetMask = Integer.parseInt(parts[1]);
      if (subnetMask < 0 || ((ipVersion() == 4 && subnetMask > 32)  || (ipVersion() == 6 && (subnetMask > 64)))) {
        throw new ParameterException(
                String.format(
                        "Parameter %s should point to an IP v%d address with a valid subnet mask, got '%s'",
                        name, ipVersion(), value));
      }
      value = parts[0];
    }
    if (Strings.isNullOrEmpty(value)
        || !InetAddresses.isInetAddress(value)
        || !shouldAccept(InetAddresses.forString(value))) {
      throw new ParameterException(
          String.format(
              "Parameter %s should point to a valid IP v%d address, got '%s'",
              name, ipVersion(), value));
    }
  }

  protected abstract int ipVersion();

  protected abstract boolean shouldAccept(InetAddress inetAddress);
}
