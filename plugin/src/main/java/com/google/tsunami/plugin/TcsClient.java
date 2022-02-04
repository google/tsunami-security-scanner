/*
 * Copyright 2021 Google LLC
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
package com.google.tsunami.plugin;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.tsunami.common.net.UrlUtils.removeTrailingSlashes;

import com.google.common.flogger.GoogleLogger;
import com.google.common.net.HostAndPort;
import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import com.google.protobuf.util.JsonFormat;
import com.google.tsunami.callbackserver.common.CbidGenerator;
import com.google.tsunami.callbackserver.common.CbidProcessor;
import com.google.tsunami.callbackserver.common.Sha3CbidGenerator;
import com.google.tsunami.callbackserver.proto.PollingResult;
import com.google.tsunami.common.net.http.HttpClient;
import com.google.tsunami.common.net.http.HttpHeaders;
import com.google.tsunami.common.net.http.HttpRequest;
import com.google.tsunami.common.net.http.HttpResponse;
import java.io.IOException;
import java.util.Optional;

/** TcsClient for generating oob payload and retriving result */
public final class TcsClient {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  private static final JsonFormat.Parser jsonParser = JsonFormat.parser();

  private final String callbackAddress;
  private final int callbackPort;
  private final String pollingBaseUrl;

  private final CbidGenerator cbidGenerator;
  private final HttpClient httpClient;

  public TcsClient(
      String callbackAddress, int callbackPort, String pollingBaseUrl, HttpClient httpClient) {
    this.callbackAddress = checkNotNull(callbackAddress);
    this.callbackPort = callbackPort;
    this.pollingBaseUrl = checkNotNull(pollingBaseUrl);
    this.cbidGenerator = new Sha3CbidGenerator();
    this.httpClient = checkNotNull(httpClient);
  }

  /**
   * Checks whether the callback server is configured. Detectors should use this to determine if
   * they can use the callback server for detecting vulnerabilities
   *
   * @return whether the callback server is enabled
   */
  public boolean isCallbackServerEnabled() {
    // only return false when all config fields are empty so that improper config (e.g., missing
    // certain fields) can be exposed. Note that {@link TcsClient} already checks that all the
    // class variables are not null.
    return !this.callbackAddress.isEmpty()
        && isValidPortNumber(this.callbackPort)
        && !this.pollingBaseUrl.isEmpty();
  }

  public String getCallbackUri(String secretString) {
    String cbid = cbidGenerator.generate(secretString);

    if (!isValidPortNumber(callbackPort)) {
      throw new AssertionError("Invalid callbackPort number specified");
    }

    HostAndPort hostAndPort =
        callbackPort == 80
            ? HostAndPort.fromHost(callbackAddress)
            : HostAndPort.fromParts(callbackAddress, callbackPort);

    // check if the specified address is raw IP or domain
    if (InetAddresses.isInetAddress(callbackAddress)) {
      return CbidProcessor.addCbidToUrl(cbid, hostAndPort);
    } else if (InternetDomainName.isValid(callbackAddress)) {
      return CbidProcessor.addCbidToSubdomain(cbid, hostAndPort);
    }
    // Should never reach here
    throw new AssertionError("Unrecognized address format, should be Ip address or valid domain");
  }

  public boolean hasOobLog(String secretString) {
    // making a blocking call to get result
    Optional<PollingResult> result = sendPollingRequest(secretString);

    if (result.isPresent()) {
      // In the future we may refactor hasOobLog() to return finer grained info about what kind
      // of oob is logged
      return result.get().getHasDnsInteraction() || result.get().getHasHttpInteraction();
    } else {
      // we may choose to retry sendPollingRequest() if oob interactions do arrive late.
      return false;
    }
  }

  private Optional<PollingResult> sendPollingRequest(String secretString) {
    HttpRequest request =
        HttpRequest.get(
                String.format("%s/?secret=%s", removeTrailingSlashes(pollingBaseUrl), secretString))
            .setHeaders(HttpHeaders.builder().addHeader("Cache-Control", "no-cache").build())
            .build();
    try {
      HttpResponse response = httpClient.send(request);
      if (response.status().isSuccess()) {
        PollingResult.Builder result = PollingResult.newBuilder();
        jsonParser.merge(response.bodyString().get(), result);
        return Optional.of(result.build());
      } else {
        logger.atInfo().log("OOB server returned %s", response.status().code());
      }
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Polling request failed");
    }
    return Optional.empty();
  }

  private static boolean isValidPortNumber(int port) {
    return port > 0 && port < 65536;
  }
}
