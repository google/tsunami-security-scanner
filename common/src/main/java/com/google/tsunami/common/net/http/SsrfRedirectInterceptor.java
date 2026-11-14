/*
 * Copyright 2024 Google LLC
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
package com.google.tsunami.common.net.http;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import okhttp3.Interceptor;
import okhttp3.Response;

final class SsrfRedirectInterceptor implements Interceptor {

  private static final ImmutableSet<Integer> REDIRECT_CODES =
      ImmutableSet.of(301, 302, 303, 307, 308);

  private static final ImmutableSet<String> BLOCKED_HOSTNAMES =
      ImmutableSet.of("metadata.google.internal");

  @Override
  public Response intercept(Chain chain) throws IOException {
    Response response = chain.proceed(chain.request());

    if (!REDIRECT_CODES.contains(response.code())) {
      return response;
    }

    String location = response.header("Location");
    if (location == null) {
      return response;
    }

    okhttp3.HttpUrl redirectUrl = response.request().url().resolve(location);
    if (redirectUrl == null) {
      return response;
    }

    String host = redirectUrl.host();

    if (BLOCKED_HOSTNAMES.contains(host.toLowerCase(java.util.Locale.ROOT))) {
      response.close();
      throw new IOException(
          "SSRF protection: redirect to blocked hostname " + host + " is not allowed");
    }

    InetAddress addr;
    try {
      addr = InetAddress.getByName(host);
    } catch (UnknownHostException e) {
      return response;
    }

    if (isBlockedAddress(addr)) {
      response.close();
      throw new IOException(
          "SSRF protection: redirect to blocked address " + addr.getHostAddress()
              + " is not allowed");
    }

    return response;
  }

  static boolean isBlockedAddress(InetAddress addr) {
    return addr.isLoopbackAddress()
        || addr.isLinkLocalAddress()
        || addr.isSiteLocalAddress()
        || isUniqueLocal(addr);
  }

  private static boolean isUniqueLocal(InetAddress addr) {
    byte[] bytes = addr.getAddress();
    if (bytes.length == 16) {
      // fc00::/7
      return (bytes[0] & 0xFE) == 0xFC;
    }
    return false;
  }
}
