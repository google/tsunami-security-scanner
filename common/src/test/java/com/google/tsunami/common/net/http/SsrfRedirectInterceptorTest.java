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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.net.InetAddress;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class SsrfRedirectInterceptorTest {

  private final MockWebServer mockWebServer = new MockWebServer();

  private final OkHttpClient client =
      new OkHttpClient.Builder()
          .followRedirects(true)
          .addNetworkInterceptor(new SsrfRedirectInterceptor())
          .build();

  @After
  public void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  public void nonRedirectResponse_isPassedThrough() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
    mockWebServer.start();

    okhttp3.Response response =
        client.newCall(new okhttp3.Request.Builder().url(mockWebServer.url("/")).build()).execute();

    assertThat(response.code()).isEqualTo(200);
    response.close();
  }

  @Test
  public void redirectToPublicIp_isAllowed() throws Exception {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(302).setHeader("Location", "http://8.8.8.8/"));
    mockWebServer.start();

    // The redirect itself is allowed; the connection to 8.8.8.8 may fail but that's fine.
    // We just verify the interceptor doesn't throw.
    try {
      client.newCall(new okhttp3.Request.Builder().url(mockWebServer.url("/")).build()).execute();
    } catch (IOException e) {
      // Connection refused to 8.8.8.8 is expected in test, but SSRF block message is not.
      assertThat(e.getMessage()).doesNotContain("SSRF protection");
    }
  }

  @Test
  public void redirectToLoopback_isBlocked() throws Exception {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(302).setHeader("Location", "http://127.0.0.1/"));
    mockWebServer.start();

    IOException exception =
        assertThrows(
            IOException.class,
            () ->
                client
                    .newCall(
                        new okhttp3.Request.Builder().url(mockWebServer.url("/test")).build())
                    .execute());

    assertThat(exception.getMessage()).contains("SSRF protection");
  }

  @Test
  public void redirectToMetadataIp_isBlocked() throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(302)
            .setHeader("Location", "http://169.254.169.254/latest/meta-data/"));
    mockWebServer.start();

    IOException exception =
        assertThrows(
            IOException.class,
            () ->
                client
                    .newCall(
                        new okhttp3.Request.Builder().url(mockWebServer.url("/test")).build())
                    .execute());

    assertThat(exception.getMessage()).contains("SSRF protection");
  }

  @Test
  public void redirectToMetadataHostname_isBlocked() throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(302)
            .setHeader("Location", "http://metadata.google.internal/computeMetadata/v1/"));
    mockWebServer.start();

    IOException exception =
        assertThrows(
            IOException.class,
            () ->
                client
                    .newCall(
                        new okhttp3.Request.Builder().url(mockWebServer.url("/test")).build())
                    .execute());

    assertThat(exception.getMessage()).contains("SSRF protection");
  }

  @Test
  public void redirectToPrivateNetwork10_isBlocked() throws Exception {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(301).setHeader("Location", "http://10.0.0.1/"));
    mockWebServer.start();

    IOException exception =
        assertThrows(
            IOException.class,
            () ->
                client
                    .newCall(
                        new okhttp3.Request.Builder().url(mockWebServer.url("/test")).build())
                    .execute());

    assertThat(exception.getMessage()).contains("SSRF protection");
  }

  @Test
  public void redirectToPrivateNetwork192_isBlocked() throws Exception {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(307).setHeader("Location", "http://192.168.1.1/"));
    mockWebServer.start();

    IOException exception =
        assertThrows(
            IOException.class,
            () ->
                client
                    .newCall(
                        new okhttp3.Request.Builder().url(mockWebServer.url("/test")).build())
                    .execute());

    assertThat(exception.getMessage()).contains("SSRF protection");
  }

  @Test
  public void redirectToPrivateNetwork172_isBlocked() throws Exception {
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(308).setHeader("Location", "http://172.16.0.1/"));
    mockWebServer.start();

    IOException exception =
        assertThrows(
            IOException.class,
            () ->
                client
                    .newCall(
                        new okhttp3.Request.Builder().url(mockWebServer.url("/test")).build())
                    .execute());

    assertThat(exception.getMessage()).contains("SSRF protection");
  }

  @Test
  public void isBlockedAddress_loopback() throws Exception {
    assertThat(SsrfRedirectInterceptor.isBlockedAddress(InetAddress.getByName("127.0.0.1")))
        .isTrue();
    assertThat(SsrfRedirectInterceptor.isBlockedAddress(InetAddress.getByName("::1"))).isTrue();
  }

  @Test
  public void isBlockedAddress_linkLocal() throws Exception {
    assertThat(SsrfRedirectInterceptor.isBlockedAddress(InetAddress.getByName("169.254.169.254")))
        .isTrue();
    assertThat(SsrfRedirectInterceptor.isBlockedAddress(InetAddress.getByName("fe80::1")))
        .isTrue();
  }

  @Test
  public void isBlockedAddress_siteLocal() throws Exception {
    assertThat(SsrfRedirectInterceptor.isBlockedAddress(InetAddress.getByName("10.0.0.1")))
        .isTrue();
    assertThat(SsrfRedirectInterceptor.isBlockedAddress(InetAddress.getByName("172.16.0.1")))
        .isTrue();
    assertThat(SsrfRedirectInterceptor.isBlockedAddress(InetAddress.getByName("192.168.1.1")))
        .isTrue();
  }

  @Test
  public void isBlockedAddress_uniqueLocalIpv6() throws Exception {
    assertThat(SsrfRedirectInterceptor.isBlockedAddress(InetAddress.getByName("fd00::1")))
        .isTrue();
    assertThat(SsrfRedirectInterceptor.isBlockedAddress(InetAddress.getByName("fc00::1")))
        .isTrue();
  }

  @Test
  public void isBlockedAddress_publicIp_notBlocked() throws Exception {
    assertThat(SsrfRedirectInterceptor.isBlockedAddress(InetAddress.getByName("8.8.8.8")))
        .isFalse();
    assertThat(
            SsrfRedirectInterceptor.isBlockedAddress(
                InetAddress.getByName("2001:4860:4860::8888")))
        .isFalse();
  }
}
