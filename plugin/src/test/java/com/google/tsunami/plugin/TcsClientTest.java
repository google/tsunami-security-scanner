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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.inject.Guice;
import com.google.protobuf.util.JsonFormat;
import com.google.tsunami.callbackserver.proto.PollingResult;
import com.google.tsunami.common.net.http.HttpClient;
import com.google.tsunami.common.net.http.HttpClientModule;
import com.google.tsunami.common.net.http.HttpStatus;
import java.io.IOException;
import javax.inject.Inject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link TcsClient}. */
@RunWith(JUnit4.class)
public final class TcsClientTest {
  private static final String SECRET = "a3d9ed89deadbeef";
  private static final String CBID = "04041e8898e739ca33a250923e24f59ca41a8373f8cf6a45a1275f3b";
  private static final String VALID_IPV4_ADDRESS = "127.0.0.1";
  private static final String VALID_IPV6_ADDRESS = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
  private static final int VALID_PORT = 8000;
  private static final String VALID_DOMAIN = "valid.co";
  private static final String VALID_URL = "http://valid.co";
  private static final String INVALID_ADDRESS = "http://invalid-address.com";

  @Inject private HttpClient httpClient;

  private TcsClient client;

  @Before
  public void setup() {
    Guice.createInjector(new HttpClientModule.Builder().build()).injectMembers(this);
  }

  @Test
  public void getCallbackUri_validIpv4Address_returnsUriWithCbidInPath() {
    client = new TcsClient(VALID_IPV4_ADDRESS, VALID_PORT, VALID_URL, httpClient);

    String url = client.getCallbackUri(SECRET);

    String expectedUriString =
        String.format("http://%s:%d/%s", VALID_IPV4_ADDRESS, VALID_PORT, CBID);
    assertThat(url).isEqualTo(expectedUriString);
  }

  @Test
  public void getCallbackUri_validIpv6Address_returnsUriWithCbidInPath() {
    client = new TcsClient(VALID_IPV6_ADDRESS, VALID_PORT, VALID_URL, httpClient);

    String url = client.getCallbackUri(SECRET);

    String expectedUriString =
        String.format("http://[%s]:%d/%s", VALID_IPV6_ADDRESS, VALID_PORT, CBID);
    assertThat(url).isEqualTo(expectedUriString);
  }

  @Test
  public void getCallbackUri_validDomainAddress_returnsUriWithCbidInSubdomain() {
    client = new TcsClient(VALID_DOMAIN, VALID_PORT, VALID_URL, httpClient);

    String url = client.getCallbackUri(SECRET);

    String expectedUriString = String.format("%s.%s:%d", CBID, VALID_DOMAIN, VALID_PORT);
    assertThat(url).isEqualTo(expectedUriString);
  }

  @Test
  public void getCallbackUri_callbackPortIs80_returnsUriWithoutPortNum() {
    client = new TcsClient(VALID_IPV4_ADDRESS, 80, VALID_URL, httpClient);

    String url = client.getCallbackUri(SECRET);

    String expectedUriString = String.format("http://%s/%s", VALID_IPV4_ADDRESS, CBID);
    assertThat(url).isEqualTo(expectedUriString);
  }

  @Test
  public void getCallbackUri_invalidAddress_throwsError() {
    client = new TcsClient(INVALID_ADDRESS, VALID_PORT, VALID_URL, httpClient);

    assertThrows(IllegalArgumentException.class, () -> client.getCallbackUri(SECRET));
  }

  @Test
  public void getCallbackUri_invalidCallbackPort_throwsError() {
    client = new TcsClient(VALID_DOMAIN, 100000, VALID_URL, httpClient);
    assertThrows(AssertionError.class, () -> client.getCallbackUri(SECRET));
  }

  @Test
  public void isVulnerable_sendsValidPollingRequest() throws IOException, InterruptedException {
    MockWebServer mockWebServer = new MockWebServer();
    mockWebServer.start();
    client = new TcsClient(VALID_DOMAIN, VALID_PORT, mockWebServer.url("/").toString(), httpClient);

    client.hasOobLog(SECRET);

    assertThat(mockWebServer.takeRequest().getPath())
        .isEqualTo(String.format("/?secret=%s", SECRET));
    mockWebServer.shutdown();
  }

  @Test
  public void isVulnerable_validLogRecordWithHttpLogged_returnsTrue() throws IOException {
    PollingResult log = PollingResult.newBuilder().setHasHttpInteraction(true).build();
    String body = JsonFormat.printer().preservingProtoFieldNames().print(log);
    MockWebServer mockWebServer = new MockWebServer();
    mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.code()).setBody(body));
    client = new TcsClient(VALID_DOMAIN, VALID_PORT, mockWebServer.url("/").toString(), httpClient);

    boolean detectionResult = client.hasOobLog(SECRET);

    assertThat(detectionResult).isTrue();
    mockWebServer.shutdown();
  }

  @Test
  public void isVulnerable_validLogRecordWithNothingLogged_returnsFalse() throws IOException {
    PollingResult log = PollingResult.getDefaultInstance();
    String body = JsonFormat.printer().preservingProtoFieldNames().print(log);
    MockWebServer mockWebServer = new MockWebServer();
    mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.code()).setBody(body));
    client = new TcsClient(VALID_DOMAIN, VALID_PORT, mockWebServer.url("/").toString(), httpClient);

    boolean detectionResult = client.hasOobLog(SECRET);

    assertThat(detectionResult).isFalse();
    mockWebServer.shutdown();
  }

  @Test
  public void isVulnerable_noLogRecordFetched_returnsFalse() throws IOException {
    MockWebServer mockWebServer = new MockWebServer();
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.code()).setBody(""));
    client = new TcsClient(VALID_DOMAIN, VALID_PORT, mockWebServer.url("/").toString(), httpClient);

    boolean detectionResult = client.hasOobLog(SECRET);

    assertThat(detectionResult).isFalse();
    mockWebServer.shutdown();
  }

  @Test
  public void isVulnerable_requestFailed_returnsFalse() {
    client = new TcsClient(VALID_DOMAIN, VALID_PORT, "http://unknownhost/path", httpClient);

    boolean detectionResult = client.hasOobLog(SECRET);

    assertThat(detectionResult).isFalse();
  }
}
