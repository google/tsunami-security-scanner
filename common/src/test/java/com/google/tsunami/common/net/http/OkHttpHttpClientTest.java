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
package com.google.tsunami.common.net.http;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.CONTENT_LENGTH;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.HOST;
import static com.google.common.net.HttpHeaders.LOCATION;
import static com.google.common.net.HttpHeaders.USER_AGENT;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.tsunami.common.net.http.HttpRequest.get;
import static com.google.tsunami.common.net.http.HttpRequest.head;
import static com.google.tsunami.common.net.http.HttpRequest.post;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.google.common.net.MediaType;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.protobuf.ByteString;
import com.google.tsunami.common.data.NetworkEndpointUtils;
import com.google.tsunami.proto.NetworkService;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link OkHttpHttpClient}. */
@RunWith(JUnit4.class)
public final class OkHttpHttpClientTest {
  private static final String TESTING_KEYSTORE = "testdata/tsunami_test_server.p12";
  private static final char[] TESTING_KEYSTORE_PASSWORD = "tsunamitest".toCharArray();

  private MockWebServer mockWebServer;
  @Inject private HttpClient httpClient;

  @Before
  public void setUp() {
    mockWebServer = new MockWebServer();
    Guice.createInjector(new HttpClientModule.Builder().build()).injectMembers(this);
  }

  @After
  public void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  public void sendAsIs_always_returnsExpectedHttpResponse()
      throws IOException, InterruptedException {
    mockWebServer.setDispatcher(new SendAsIsTestDispatcher());
    mockWebServer.start();
    String expectedResponseBody = SendAsIsTestDispatcher.buildBody("GET", "");

    HttpUrl baseUrl = mockWebServer.url("/");
    String requestUrl =
        new URL(
                baseUrl.scheme(),
                baseUrl.host(),
                baseUrl.port(),
                "/send-as-is/%2e%2e/%2e%2e/etc/passwd")
            .toString();

    HttpResponse response = httpClient.sendAsIs(get(requestUrl).withEmptyHeaders().build());

    assertThat(mockWebServer.takeRequest().getPath())
        .isEqualTo("/send-as-is/%2e%2e/%2e%2e/etc/passwd");
    assertThat(response)
        .isEqualTo(
            HttpResponse.builder()
                .setStatus(HttpStatus.OK)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
                        // MockWebServer always adds this response header.
                        .addHeader(CONTENT_LENGTH, String.valueOf(expectedResponseBody.length()))
                        .build())
                .setBodyBytes(ByteString.copyFrom(expectedResponseBody, UTF_8))
                .build());
  }

  @Test
  public void sendAsIs_withPostRequest_returnsExpectedHttpResponse()
      throws IOException, InterruptedException {
    mockWebServer.setDispatcher(new SendAsIsTestDispatcher());
    mockWebServer.start();
    String requestBody = "POST BODY";
    String expectedResponseBody = SendAsIsTestDispatcher.buildBody("POST", requestBody);

    HttpUrl baseUrl = mockWebServer.url("/");
    String requestUrl =
        new URL(baseUrl.scheme(), baseUrl.host(), baseUrl.port(), "/send-as-is/%2e%2e/%2e%2e/path")
            .toString();

    HttpResponse response =
        httpClient.sendAsIs(
            post(requestUrl)
                .setRequestBody(ByteString.copyFrom(requestBody, UTF_8))
                .withEmptyHeaders()
                .build());

    assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/send-as-is/%2e%2e/%2e%2e/path");
    assertThat(response)
        .isEqualTo(
            HttpResponse.builder()
                .setStatus(HttpStatus.OK)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
                        // MockWebServer always adds this response header.
                        .addHeader(CONTENT_LENGTH, String.valueOf(expectedResponseBody.length()))
                        .build())
                .setBodyBytes(ByteString.copyFrom(expectedResponseBody, UTF_8))
                .build());
  }

  @Test
  public void send_always_canonicalizesRequestUrl() throws IOException, InterruptedException {
    String responseBody = "test response";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.code())
            .setHeader(CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
            .setBody(responseBody));
    mockWebServer.start();

    HttpUrl baseUrl = mockWebServer.url("/");
    String requestUrl =
        new URL(baseUrl.scheme(), baseUrl.host(), baseUrl.port(), "/%2e%2e/%2e%2e/etc/passwd")
            .toString();

    httpClient.send(get(requestUrl).withEmptyHeaders().build());

    assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/etc/passwd");
  }

  @Test
  public void send_whenGetRequest_returnsExpectedHttpResponse() throws IOException {
    String responseBody = "test response";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.code())
            .setHeader(CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
            .setBody(responseBody));
    mockWebServer.start();

    String requestUrl = mockWebServer.url("/test/get").toString();

    HttpResponse response = httpClient.send(get(requestUrl).withEmptyHeaders().build());

    assertThat(response)
        .isEqualTo(
            HttpResponse.builder()
                .setStatus(HttpStatus.OK)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
                        // MockWebServer always adds this response header.
                        .addHeader(CONTENT_LENGTH, String.valueOf(responseBody.length()))
                        .build())
                .setBodyBytes(ByteString.copyFrom(responseBody, UTF_8))
                .setResponseUrl(HttpUrl.parse(requestUrl))
                .build());
  }

  @Test
  public void sendAsync_whenGetRequest_returnsExpectedHttpResponse()
      throws IOException, ExecutionException, InterruptedException {
    String responseBody = "test response";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.code())
            .setHeader(CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
            .setBody(responseBody));
    mockWebServer.start();

    String requestUrl = mockWebServer.url("/test/get").toString();

    HttpResponse response = httpClient.sendAsync(get(requestUrl).withEmptyHeaders().build()).get();

    assertThat(response)
        .isEqualTo(
            HttpResponse.builder()
                .setStatus(HttpStatus.OK)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
                        // MockWebServer always adds this response header.
                        .addHeader(CONTENT_LENGTH, String.valueOf(responseBody.length()))
                        .build())
                .setBodyBytes(ByteString.copyFrom(responseBody, UTF_8))
                .setResponseUrl(HttpUrl.parse(requestUrl))
                .build());
  }

  @Test
  public void send_whenHeadRequest_returnsHttpResponseWithoutBody() throws IOException {
    String responseBody = "test response";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.code())
            .setHeader(CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
            .setBody(responseBody));
    mockWebServer.start();

    String requestUrl = mockWebServer.url("/test/head").toString();

    HttpResponse response = httpClient.send(head(requestUrl).withEmptyHeaders().build());

    assertThat(response)
        .isEqualTo(
            HttpResponse.builder()
                .setStatus(HttpStatus.OK)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
                        // MockWebServer always adds this response header.
                        .addHeader(CONTENT_LENGTH, String.valueOf(responseBody.length()))
                        .build())
                .setBodyBytes(Optional.empty())
                .setResponseUrl(HttpUrl.parse(requestUrl))
                .build());
  }

  @Test
  public void sendAsync_whenHeadRequest_returnsHttpResponseWithoutBody()
      throws IOException, ExecutionException, InterruptedException {
    String responseBody = "test response";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.code())
            .setHeader(CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
            .setBody(responseBody));
    mockWebServer.start();

    String requestUrl = mockWebServer.url("/test/head").toString();

    HttpResponse response = httpClient.sendAsync(head(requestUrl).withEmptyHeaders().build()).get();

    assertThat(response)
        .isEqualTo(
            HttpResponse.builder()
                .setStatus(HttpStatus.OK)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
                        // MockWebServer always adds this response header.
                        .addHeader(CONTENT_LENGTH, String.valueOf(responseBody.length()))
                        .build())
                .setBodyBytes(Optional.empty())
                .setResponseUrl(HttpUrl.parse(requestUrl))
                .build());
  }

  @Test
  public void send_whenPostRequest_returnsExpectedHttpResponse() throws IOException {
    String responseBody = "{ \"test\": \"json\" }";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.code())
            .setHeader(CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
            .setBody(responseBody));
    mockWebServer.start();

    String requestUrl = mockWebServer.url("/test/post").toString();

    HttpResponse response =
        httpClient.send(
            post(requestUrl)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(ACCEPT, MediaType.JSON_UTF_8.toString())
                        .build())
                .build());

    assertThat(response)
        .isEqualTo(
            HttpResponse.builder()
                .setStatus(HttpStatus.OK)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
                        // MockWebServer always adds this response header.
                        .addHeader(CONTENT_LENGTH, String.valueOf(responseBody.length()))
                        .build())
                .setBodyBytes(ByteString.copyFrom(responseBody, UTF_8))
                .setResponseUrl(HttpUrl.parse(requestUrl))
                .build());
  }

  @Test
  public void sendAsync_whenPostRequest_returnsExpectedHttpResponse()
      throws IOException, ExecutionException, InterruptedException {
    String responseBody = "{ \"test\": \"json\" }";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.code())
            .setHeader(CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
            .setBody(responseBody));
    mockWebServer.start();

    String requestUrl = mockWebServer.url("/test/post").toString();

    HttpResponse response =
        httpClient
            .sendAsync(
                post(requestUrl)
                    .setHeaders(
                        HttpHeaders.builder()
                            .addHeader(ACCEPT, MediaType.JSON_UTF_8.toString())
                            .build())
                    .build())
            .get();

    assertThat(response)
        .isEqualTo(
            HttpResponse.builder()
                .setStatus(HttpStatus.OK)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
                        // MockWebServer always adds this response header.
                        .addHeader(CONTENT_LENGTH, String.valueOf(responseBody.length()))
                        .build())
                .setBodyBytes(ByteString.copyFrom(responseBody, UTF_8))
                .setResponseUrl(HttpUrl.parse(requestUrl))
                .build());
  }

  @Test
  public void send_whenPostRequestWithEmptyHeaders_returnsExpectedHttpResponse()
      throws IOException {
    String responseBody = "{ \"test\": \"json\" }";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.code())
            .setHeader(CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
            .setBody(responseBody));
    mockWebServer.start();

    String requestUrl = mockWebServer.url("/test/post").toString();

    HttpResponse response = httpClient.send(post(requestUrl).withEmptyHeaders().build());

    assertThat(response)
        .isEqualTo(
            HttpResponse.builder()
                .setStatus(HttpStatus.OK)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
                        // MockWebServer always adds this response header.
                        .addHeader(CONTENT_LENGTH, String.valueOf(responseBody.length()))
                        .build())
                .setBodyBytes(ByteString.copyFrom(responseBody, UTF_8))
                .setResponseUrl(HttpUrl.parse(requestUrl))
                .build());
  }

  @Test
  public void sendAsync_whenPostRequestWithEmptyHeaders_returnsExpectedHttpResponse()
      throws IOException, ExecutionException, InterruptedException {
    String responseBody = "{ \"test\": \"json\" }";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.code())
            .setHeader(CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
            .setBody(responseBody));
    mockWebServer.start();

    String requestUrl = mockWebServer.url("/test/post").toString();

    HttpResponse response = httpClient.sendAsync(post(requestUrl).withEmptyHeaders().build()).get();

    assertThat(response)
        .isEqualTo(
            HttpResponse.builder()
                .setStatus(HttpStatus.OK)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
                        // MockWebServer always adds this response header.
                        .addHeader(CONTENT_LENGTH, String.valueOf(responseBody.length()))
                        .build())
                .setBodyBytes(ByteString.copyFrom(responseBody, UTF_8))
                .setResponseUrl(HttpUrl.parse(requestUrl))
                .build());
  }

  @Test
  public void send_whenFollowRedirect_returnsFinalHttpResponse() throws IOException {
    String responseBody = "test response";
    mockWebServer.setDispatcher(new RedirectDispatcher(responseBody));
    mockWebServer.start();

    HttpResponse response =
        httpClient
            .modify()
            .setFollowRedirects(true)
            .build()
            .send(
                get(mockWebServer.url(RedirectDispatcher.REDIRECT_PATH).toString())
                    .withEmptyHeaders()
                    .build());

    HttpUrl redirectDestinationUrl =
        HttpUrl.parse(mockWebServer.url(RedirectDispatcher.REDIRECT_DESTINATION_PATH).toString());

    assertThat(response)
        .isEqualTo(
            HttpResponse.builder()
                .setStatus(HttpStatus.OK)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(CONTENT_LENGTH, String.valueOf(responseBody.length()))
                        .build())
                .setBodyBytes(ByteString.copyFrom(responseBody, UTF_8))
                .setResponseUrl(redirectDestinationUrl)
                .build());
  }

  @Test
  public void sendAsync_whenFollowRedirect_returnsFinalHttpResponse()
      throws IOException, ExecutionException, InterruptedException {
    String responseBody = "test response";
    mockWebServer.setDispatcher(new RedirectDispatcher(responseBody));
    mockWebServer.start();

    HttpUrl redirectDestinationUrl =
        HttpUrl.parse(mockWebServer.url(RedirectDispatcher.REDIRECT_DESTINATION_PATH).toString());

    HttpResponse response =
        httpClient
            .modify()
            .setFollowRedirects(true)
            .build()
            .sendAsync(
                get(mockWebServer.url(RedirectDispatcher.REDIRECT_PATH).toString())
                    .withEmptyHeaders()
                    .build())
            .get();

    assertThat(response)
        .isEqualTo(
            HttpResponse.builder()
                .setStatus(HttpStatus.OK)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(CONTENT_LENGTH, String.valueOf(responseBody.length()))
                        .build())
                .setBodyBytes(ByteString.copyFrom(responseBody, UTF_8))
                .setResponseUrl(redirectDestinationUrl)
                .build());
  }

  @Test
  public void send_whenNotFollowRedirect_returnsFinalHttpResponse() throws IOException {
    String responseBody = "test response";
    mockWebServer.setDispatcher(new RedirectDispatcher(responseBody));
    mockWebServer.start();

    String redirectingUrl = mockWebServer.url(RedirectDispatcher.REDIRECT_PATH).toString();

    HttpResponse response =
        httpClient
            .modify()
            .setFollowRedirects(false)
            .build()
            .send(get(redirectingUrl).withEmptyHeaders().build());

    assertThat(response.status()).isEqualTo(HttpStatus.FOUND);
    assertThat(response.headers())
        .isEqualTo(
            HttpHeaders.builder()
                .addHeader(CONTENT_LENGTH, "0")
                .addHeader(LOCATION, RedirectDispatcher.REDIRECT_DESTINATION_PATH)
                .build());
    assertThat(response.bodyString()).hasValue("");
    assertThat(response)
        .isEqualTo(
            HttpResponse.builder()
                .setStatus(HttpStatus.FOUND)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(CONTENT_LENGTH, "0")
                        .addHeader(LOCATION, RedirectDispatcher.REDIRECT_DESTINATION_PATH)
                        .build())
                .setBodyBytes(ByteString.EMPTY)
                .setResponseUrl(HttpUrl.parse(redirectingUrl))
                .build());
  }

  @Test
  public void sendAsync_whenNotFollowRedirect_returnsFinalHttpResponse()
      throws IOException, ExecutionException, InterruptedException {
    String responseBody = "test response";
    mockWebServer.setDispatcher(new RedirectDispatcher(responseBody));
    mockWebServer.start();

    String redirectingUrl = mockWebServer.url(RedirectDispatcher.REDIRECT_PATH).toString();

    HttpResponse response =
        httpClient
            .modify()
            .setFollowRedirects(false)
            .build()
            .sendAsync(get(redirectingUrl).withEmptyHeaders().build())
            .get();

    assertThat(response.status()).isEqualTo(HttpStatus.FOUND);
    assertThat(response.headers())
        .isEqualTo(
            HttpHeaders.builder()
                .addHeader(CONTENT_LENGTH, "0")
                .addHeader(LOCATION, RedirectDispatcher.REDIRECT_DESTINATION_PATH)
                .build());
    assertThat(response.bodyString()).hasValue("");
    assertThat(response)
        .isEqualTo(
            HttpResponse.builder()
                .setStatus(HttpStatus.FOUND)
                .setHeaders(
                    HttpHeaders.builder()
                        .addHeader(CONTENT_LENGTH, "0")
                        .addHeader(LOCATION, RedirectDispatcher.REDIRECT_DESTINATION_PATH)
                        .build())
                .setBodyBytes(ByteString.EMPTY)
                .setResponseUrl(HttpUrl.parse(redirectingUrl))
                .build());
  }

  @Test
  public void send_whenNoUserAgentInRequest_setsCorrectUserAgentHeader() throws IOException {
    mockWebServer.setDispatcher(new UserAgentTestDispatcher());
    mockWebServer.start();

    HttpResponse response =
        httpClient.send(
            get(mockWebServer.url(UserAgentTestDispatcher.USERAGENT_TEST_PATH).toString())
                .withEmptyHeaders()
                .build());

    assertThat(response.status()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void send_whenUserAgentSetInRequest_overridesUserAgentHeader() throws IOException {
    mockWebServer.setDispatcher(new UserAgentTestDispatcher());
    mockWebServer.start();

    HttpResponse response =
        httpClient.send(
            get(mockWebServer.url(UserAgentTestDispatcher.USERAGENT_TEST_PATH).toString())
                .setHeaders(
                    HttpHeaders.builder().addHeader(USER_AGENT, "User Agent In Request").build())
                .build());

    assertThat(response.status()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void send_whenRequestFailed_throwsException() {
    assertThrows(
        IOException.class,
        () -> httpClient.send(get("http://unknownhost/path").withEmptyHeaders().build()));
  }

  @Test
  public void sendAsync_whenRequestFailed_returnsFutureWithException() {
    ListenableFuture<HttpResponse> responseFuture =
        httpClient.sendAsync(get("http://unknownhost/path").withEmptyHeaders().build());

    ExecutionException ex = assertThrows(ExecutionException.class, responseFuture::get);
    assertThat(ex).hasCauseThat().isInstanceOf(IOException.class);
  }

  @Test
  public void send_whenHostnameAndIpInRequest_useHostnameAsProxy() throws IOException {
    InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
    String host = "host.com";
    mockWebServer.setDispatcher(new HostnameTestDispatcher(host));
    mockWebServer.start(loopbackAddress, 0);
    int port = mockWebServer.url("/").port();

    NetworkService networkService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(
                NetworkEndpointUtils.forIpHostnameAndPort(
                    loopbackAddress.getHostAddress(), host, port))
            .build();

    // The request to host.com should be sent through mockWebServer's IP.
    HttpResponse response =
        httpClient.send(
            get(String.format("http://host.com:%d/test/get", port)).withEmptyHeaders().build(),
            networkService);

    assertThat(response.status()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void send_whenInvalidCertificatesAreIgnored_getResponseWithoutException()
      throws GeneralSecurityException, IOException {
    InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
    String host = "host.com";
    MockWebServer mockWebServer = startMockWebServerWithSsl(loopbackAddress);
    int port = mockWebServer.url("/").port();
    NetworkService networkService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(
                NetworkEndpointUtils.forIpHostnameAndPort(
                    loopbackAddress.getHostAddress(), host, port))
            .build();

    HttpClientCliOptions cliOptions = new HttpClientCliOptions();
    HttpClientConfigProperties configProperties = new HttpClientConfigProperties();
    cliOptions.trustAllCertificates = configProperties.trustAllCertificates = true;
    HttpClient httpClient =
        Guice.createInjector(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    install(new HttpClientModule.Builder().build());
                    bind(HttpClientCliOptions.class).toInstance(cliOptions);
                    bind(HttpClientConfigProperties.class).toInstance(configProperties);
                  }
                })
            .getInstance(HttpClient.class);

    HttpResponse response =
        httpClient.send(
            get(String.format("https://%s:%d", host, port)).withEmptyHeaders().build(),
            networkService);
    assertThat(response.bodyString()).hasValue("body");

    mockWebServer.shutdown();
  }

  @Test
  public void send_whenInvalidCertificatesAreNotIgnored_throws()
      throws GeneralSecurityException, IOException {
    InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
    String host = "host.com";
    MockWebServer mockWebServer = startMockWebServerWithSsl(loopbackAddress);
    int port = mockWebServer.url("/").port();
    NetworkService networkService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(
                NetworkEndpointUtils.forIpHostnameAndPort(
                    loopbackAddress.getHostAddress(), host, port))
            .build();

    HttpClientCliOptions cliOptions = new HttpClientCliOptions();
    HttpClientConfigProperties configProperties = new HttpClientConfigProperties();
    cliOptions.trustAllCertificates = configProperties.trustAllCertificates = false;
    HttpClient httpClient =
        Guice.createInjector(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    install(new HttpClientModule.Builder().build());
                    bind(HttpClientCliOptions.class).toInstance(cliOptions);
                    bind(HttpClientConfigProperties.class).toInstance(configProperties);
                  }
                })
            .getInstance(HttpClient.class);

    assertThrows(
        SSLException.class,
        () ->
            httpClient.send(
                get(String.format("https://%s:%d", host, port)).withEmptyHeaders().build(),
                networkService));

    mockWebServer.shutdown();
  }

  private MockWebServer startMockWebServerWithSsl(InetAddress serverAddress)
      throws GeneralSecurityException, IOException {
    MockWebServer mockWebServer = new MockWebServer();
    mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.code()).setBody("body"));
    mockWebServer.useHttps(getTestingSslSocketFactory(), false);
    mockWebServer.start(serverAddress, 0);
    return mockWebServer;
  }

  private SSLSocketFactory getTestingSslSocketFactory()
      throws GeneralSecurityException, IOException {
    final KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(getClass().getResourceAsStream(TESTING_KEYSTORE), TESTING_KEYSTORE_PASSWORD);
    keyManagerFactory.init(keyStore, TESTING_KEYSTORE_PASSWORD);
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
    return sslContext.getSocketFactory();
  }

  static final class RedirectDispatcher extends Dispatcher {
    static final String REDIRECT_PATH = "/redirect";
    static final String REDIRECT_DESTINATION_PATH = "/redirect-dest";

    private final String responseBody;

    RedirectDispatcher(String responseBody) {
      this.responseBody = checkNotNull(responseBody);
    }

    @Override
    public MockResponse dispatch(RecordedRequest recordedRequest) {
      switch (recordedRequest.getPath()) {
        case REDIRECT_PATH:
          return new MockResponse()
              .setResponseCode(HttpStatus.FOUND.code())
              .setHeader(LOCATION, "/redirect-dest");
        case REDIRECT_DESTINATION_PATH:
          return new MockResponse().setResponseCode(HttpStatus.OK.code()).setBody(responseBody);
        default:
          return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.code());
      }
    }
  }

  static final class UserAgentTestDispatcher extends Dispatcher {
    static final String USERAGENT_TEST_PATH = "/useragent-test";

    @Override
    public MockResponse dispatch(RecordedRequest recordedRequest) {
      if (recordedRequest.getPath().equals(USERAGENT_TEST_PATH)
          && nullToEmpty(recordedRequest.getHeader(USER_AGENT)).equals("TsunamiSecurityScanner")) {
        return new MockResponse().setResponseCode(HttpStatus.OK.code());
      }
      return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.code());
    }
  }

  static final class HostnameTestDispatcher extends Dispatcher {
    private final String expectedHost;

    HostnameTestDispatcher(String expectedHost) {
      this.expectedHost = checkNotNull(expectedHost);
    }

    @Override
    public MockResponse dispatch(RecordedRequest recordedRequest) {
      if (nullToEmpty(recordedRequest.getHeader(HOST)).startsWith(expectedHost)) {
        return new MockResponse().setResponseCode(HttpStatus.OK.code());
      }
      return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.code());
    }
  }

  static final class SendAsIsTestDispatcher extends Dispatcher {
    static final String SEND_AS_IS_PATH = "/send-as-is/";

    static String buildBody(String method, String requestBody) {
      return String.format("Method: %s\nRequest Body: %s", method, requestBody);
    }

    @Override
    public MockResponse dispatch(RecordedRequest recordedRequest) {
      if (recordedRequest.getPath().startsWith(SEND_AS_IS_PATH)) {
        return new MockResponse()
            .setHeader(CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
            .setBody(buildBody(recordedRequest.getMethod(), recordedRequest.getBody().readUtf8()))
            .setResponseCode(HttpStatus.OK.code());
      }
      return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.code());
    }
  }
}
