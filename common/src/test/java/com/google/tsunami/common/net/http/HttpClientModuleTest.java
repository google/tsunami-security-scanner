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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.tsunami.common.net.http.HttpRequest.get;
import static org.junit.Assert.assertThrows;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link HttpClientModule}. */
@RunWith(JUnit4.class)
public final class HttpClientModuleTest {
  private static final String TESTING_KEYSTORE = "testdata/tsunami_test_server.p12";
  private static final char[] TESTING_KEYSTORE_PASSWORD = "tsunamitest".toCharArray();

  private final HttpClientCliOptions cliOptions = new HttpClientCliOptions();
  private final HttpClientConfigProperties configProperties = new HttpClientConfigProperties();

  @Test
  public void provideOkHttpClient_always_createsSingleton() {
    Injector injector =
        Guice.createInjector(new HttpClientModule.Builder().setMaxRequests(10).build());

    OkHttpClient okHttpClient = injector.getInstance(OkHttpClient.class);
    OkHttpClient okHttpClient2 = injector.getInstance(OkHttpClient.class);

    assertThat(okHttpClient).isSameInstanceAs(okHttpClient2);
  }

  @Test
  public void setConnectionPoolMaxIdle_whenNonPositiveMaxIdle_throwsIllegalArgumentException() {
    HttpClientModule.Builder builder = new HttpClientModule.Builder();

    assertThrows(IllegalArgumentException.class, () -> builder.setConnectionPoolMaxIdle(-1));
    assertThrows(IllegalArgumentException.class, () -> builder.setConnectionPoolMaxIdle(0));
  }

  @Test
  public void
      setConnectionPoolKeepAliveDuration_whenNegativeDuration_throwsIllegalArgumentException() {
    HttpClientModule.Builder builder = new HttpClientModule.Builder();

    assertThrows(
        IllegalArgumentException.class,
        () -> builder.setConnectionPoolKeepAliveDuration(Duration.ofMillis(-1)));
  }

  @Test
  public void setMaxRequests_whenPositiveRequests_setsValueToDispatcher() {
    Injector injector =
        Guice.createInjector(new HttpClientModule.Builder().setMaxRequests(10).build());

    OkHttpClient okHttpClient = injector.getInstance(OkHttpClient.class);

    assertThat(okHttpClient.dispatcher().getMaxRequests()).isEqualTo(10);
  }

  @Test
  public void setMaxRequests_whenNonPositiveRequests_throwsIllegalArgumentException() {
    HttpClientModule.Builder builder = new HttpClientModule.Builder();

    assertThrows(IllegalArgumentException.class, () -> builder.setMaxRequests(-1));
    assertThrows(IllegalArgumentException.class, () -> builder.setMaxRequests(0));
  }

  @Test
  public void setMaxRequestsPerHost_whenPositiveRequests_setsValueToDispatcher() {
    Injector injector =
        Guice.createInjector(new HttpClientModule.Builder().setMaxRequestsPerHost(10).build());

    OkHttpClient okHttpClient = injector.getInstance(OkHttpClient.class);

    assertThat(okHttpClient.dispatcher().getMaxRequestsPerHost()).isEqualTo(10);
  }

  @Test
  public void setMaxRequestsPerHost_whenNonPositiveRequests_throwsIllegalArgumentException() {
    HttpClientModule.Builder builder = new HttpClientModule.Builder();

    assertThrows(IllegalArgumentException.class, () -> builder.setMaxRequestsPerHost(-1));
    assertThrows(IllegalArgumentException.class, () -> builder.setMaxRequestsPerHost(0));
  }

  @Test
  public void setFollowRedirects_always_setsValueToClient() {
    Injector injector =
        Guice.createInjector(new HttpClientModule.Builder().setFollowRedirects(true).build());

    OkHttpClient okHttpClient = injector.getInstance(OkHttpClient.class);

    assertThat(okHttpClient.followRedirects()).isTrue();
  }

  @Test
  public void setTrustAllCertificates_whenFalseAndCertIsInvalid_throws()
      throws GeneralSecurityException, IOException {
    cliOptions.trustAllCertificates = false;
    configProperties.trustAllCertificates = false;
    HttpClient httpClient =
        Guice.createInjector(getTrustAllCertificatesTestingModule()).getInstance(HttpClient.class);
    MockWebServer mockWebServer = startMockWebServerWithSsl();

    // The certificate used in test is a self-signed one. HttpClient will reject it unless the
    // certificate is explicitly trusted.
    assertThrows(
        SSLHandshakeException.class,
        () -> httpClient.send(get(mockWebServer.url("/")).withEmptyHeaders().build()));

    mockWebServer.shutdown();
  }

  @Test
  public void setTrustAllCertificates_whenCliOptionEnabledAndCertIsInvalid_ignoresCertError()
      throws GeneralSecurityException, IOException {
    cliOptions.trustAllCertificates = true;
    configProperties.trustAllCertificates = false;
    HttpClient httpClient =
        Guice.createInjector(getTrustAllCertificatesTestingModule()).getInstance(HttpClient.class);
    MockWebServer mockWebServer = startMockWebServerWithSsl();

    HttpResponse response = httpClient.send(get(mockWebServer.url("/")).withEmptyHeaders().build());
    assertThat(response.bodyString()).hasValue("body");

    mockWebServer.shutdown();
  }

  @Test
  public void
      setTrustAllCertificates_whenConfigPropropertyEnabledAndCertIsInvalid_ignoresCertError()
          throws GeneralSecurityException, IOException {
    cliOptions.trustAllCertificates = false;
    configProperties.trustAllCertificates = true;
    HttpClient httpClient =
        Guice.createInjector(getTrustAllCertificatesTestingModule()).getInstance(HttpClient.class);
    MockWebServer mockWebServer = startMockWebServerWithSsl();

    HttpResponse response = httpClient.send(get(mockWebServer.url("/")).withEmptyHeaders().build());
    assertThat(response.bodyString()).hasValue("body");

    mockWebServer.shutdown();
  }

  private AbstractModule getTrustAllCertificatesTestingModule() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        install(new HttpClientModule.Builder().build());
        bind(HttpClientCliOptions.class).toInstance(cliOptions);
        bind(HttpClientConfigProperties.class).toInstance(configProperties);
      }
    };
  }

  private MockWebServer startMockWebServerWithSsl() throws GeneralSecurityException, IOException {
    MockWebServer mockWebServer = new MockWebServer();
    mockWebServer.useHttps(getTestingSslSocketFactory(), false);
    mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.code()).setBody("body"));
    mockWebServer.start();
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
}
