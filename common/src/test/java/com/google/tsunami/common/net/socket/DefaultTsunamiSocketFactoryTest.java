/*
 * Copyright 2025 Google LLC
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
package com.google.tsunami.common.net.socket;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

/** Unit tests for {@link DefaultTsunamiSocketFactory}. */
@RunWith(JUnit4.class)
public final class DefaultTsunamiSocketFactoryTest {

  private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

  private SocketFactory mockSocketFactory;
  private SSLSocketFactory mockSslSocketFactory;
  private Socket mockSocket;
  private SSLSocket mockSslSocket;
  private DefaultTsunamiSocketFactory tsunamiSocketFactory;

  @Before
  public void setUp() throws IOException {
    mockSocketFactory = mock(SocketFactory.class);
    mockSslSocketFactory = mock(SSLSocketFactory.class);
    mockSocket = mock(Socket.class);
    mockSslSocket = mock(SSLSocket.class);

    when(mockSocketFactory.createSocket()).thenReturn(mockSocket);
    when(mockSslSocketFactory.createSocket(any(Socket.class), anyString(), anyInt(), anyBoolean()))
        .thenReturn(mockSslSocket);

    tsunamiSocketFactory =
        new DefaultTsunamiSocketFactory(
            mockSocketFactory, mockSslSocketFactory, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }

  @Test
  public void constructor_withNullSocketFactory_throwsException() {
    assertThrows(
        NullPointerException.class,
        () ->
            new DefaultTsunamiSocketFactory(
                null, mockSslSocketFactory, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT));
  }

  @Test
  public void constructor_withNullSslSocketFactory_throwsException() {
    assertThrows(
        NullPointerException.class,
        () ->
            new DefaultTsunamiSocketFactory(
                mockSocketFactory, null, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT));
  }

  @Test
  public void constructor_withNegativeConnectTimeout_throwsException() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DefaultTsunamiSocketFactory(
                mockSocketFactory,
                mockSslSocketFactory,
                Duration.ofSeconds(-1),
                DEFAULT_READ_TIMEOUT));
  }

  @Test
  public void constructor_withNegativeReadTimeout_throwsException() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DefaultTsunamiSocketFactory(
                mockSocketFactory,
                mockSslSocketFactory,
                DEFAULT_CONNECT_TIMEOUT,
                Duration.ofSeconds(-1)));
  }

  @Test
  public void createSocket_withHostAndPort_setsTimeoutsAndConnects() throws IOException {
    tsunamiSocketFactory.createSocket("example.com", 80);

    verify(mockSocket).setSoTimeout((int) DEFAULT_READ_TIMEOUT.toMillis());
    verify(mockSocket).setKeepAlive(true);
    verify(mockSocket).setTcpNoDelay(true);

    ArgumentCaptor<InetSocketAddress> addressCaptor =
        ArgumentCaptor.forClass(InetSocketAddress.class);
    ArgumentCaptor<Integer> timeoutCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(mockSocket).connect(addressCaptor.capture(), timeoutCaptor.capture());

    assertThat(addressCaptor.getValue().getHostString()).isEqualTo("example.com");
    assertThat(addressCaptor.getValue().getPort()).isEqualTo(80);
    assertThat(timeoutCaptor.getValue()).isEqualTo((int) DEFAULT_CONNECT_TIMEOUT.toMillis());
  }

  @Test
  public void createSocket_withCustomTimeouts_usesProvidedValues() throws IOException {
    Duration customConnect = Duration.ofSeconds(5);
    Duration customRead = Duration.ofSeconds(15);

    tsunamiSocketFactory.createSocket("example.com", 443, customConnect, customRead);

    verify(mockSocket).setSoTimeout((int) customRead.toMillis());

    ArgumentCaptor<Integer> timeoutCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(mockSocket).connect(any(), timeoutCaptor.capture());
    assertThat(timeoutCaptor.getValue()).isEqualTo((int) customConnect.toMillis());
  }

  @Test
  public void createSocket_withInetAddress_setsTimeoutsAndConnects() throws IOException {
    InetAddress address = InetAddress.getLoopbackAddress();

    tsunamiSocketFactory.createSocket(address, 8080);

    verify(mockSocket).setSoTimeout((int) DEFAULT_READ_TIMEOUT.toMillis());
    verify(mockSocket).setKeepAlive(true);
    verify(mockSocket).setTcpNoDelay(true);

    ArgumentCaptor<InetSocketAddress> addressCaptor =
        ArgumentCaptor.forClass(InetSocketAddress.class);
    verify(mockSocket).connect(addressCaptor.capture(), anyInt());

    assertThat(addressCaptor.getValue().getAddress()).isEqualTo(address);
    assertThat(addressCaptor.getValue().getPort()).isEqualTo(8080);
  }

  @Test
  public void createSocket_withInvalidPort_throwsException() {
    assertThrows(
        IllegalArgumentException.class, () -> tsunamiSocketFactory.createSocket("example.com", 0));

    assertThrows(
        IllegalArgumentException.class,
        () -> tsunamiSocketFactory.createSocket("example.com", -1));

    assertThrows(
        IllegalArgumentException.class,
        () -> tsunamiSocketFactory.createSocket("example.com", 65536));
  }

  @Test
  public void createSocket_withNullHost_throwsException() {
    assertThrows(
        NullPointerException.class, () -> tsunamiSocketFactory.createSocket((String) null, 80));
  }

  @Test
  public void createUnconnectedSocket_setsReadTimeout() throws IOException {
    Socket socket = tsunamiSocketFactory.createUnconnectedSocket();

    assertThat(socket).isEqualTo(mockSocket);
    verify(mockSocket).setSoTimeout((int) DEFAULT_READ_TIMEOUT.toMillis());
  }

  @Test
  public void createSslSocket_withHostAndPort_createsAndConfigures() throws IOException {
    tsunamiSocketFactory.createSslSocket("secure.example.com", 443);

    // Verify plain socket is created and connected first
    verify(mockSocketFactory).createSocket();
    verify(mockSocket).connect(any(InetSocketAddress.class), anyInt());

    // Verify SSL wrapping
    verify(mockSslSocketFactory)
        .createSocket(mockSocket, "secure.example.com", 443, true);
    verify(mockSslSocket).setSoTimeout((int) DEFAULT_READ_TIMEOUT.toMillis());
    verify(mockSslSocket).startHandshake();
  }

  @Test
  public void createSslSocket_withCustomTimeouts_usesProvidedValues() throws IOException {
    Duration customConnect = Duration.ofSeconds(5);
    Duration customRead = Duration.ofSeconds(15);

    tsunamiSocketFactory.createSslSocket("secure.example.com", 443, customConnect, customRead);

    verify(mockSocket).setSoTimeout((int) customRead.toMillis());
    verify(mockSslSocket).setSoTimeout((int) customRead.toMillis());

    ArgumentCaptor<Integer> connectTimeoutCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(mockSocket).connect(any(), connectTimeoutCaptor.capture());
    assertThat(connectTimeoutCaptor.getValue()).isEqualTo((int) customConnect.toMillis());
  }

  @Test
  public void createSslSocket_withInetAddress_createsAndConfigures() throws IOException {
    InetAddress address = InetAddress.getLoopbackAddress();

    tsunamiSocketFactory.createSslSocket(address, 443);

    verify(mockSocketFactory).createSocket();
    verify(mockSocket).connect(any(InetSocketAddress.class), anyInt());
    verify(mockSslSocketFactory)
        .createSocket(mockSocket, address.getHostAddress(), 443, true);
    verify(mockSslSocket).startHandshake();
  }

  @Test
  public void wrapWithSsl_wrapsExistingSocket() throws IOException {
    when(mockSocket.getSoTimeout()).thenReturn(5000);

    tsunamiSocketFactory.wrapWithSsl(mockSocket, "example.com", 443, true);

    verify(mockSslSocketFactory).createSocket(mockSocket, "example.com", 443, true);
    verify(mockSslSocket).setSoTimeout(5000);
    verify(mockSslSocket).startHandshake();
  }

  @Test
  public void wrapWithSsl_withZeroOriginalTimeout_usesDefault() throws IOException {
    when(mockSocket.getSoTimeout()).thenReturn(0);

    tsunamiSocketFactory.wrapWithSsl(mockSocket, "example.com", 443, true);

    verify(mockSslSocket).setSoTimeout((int) DEFAULT_READ_TIMEOUT.toMillis());
  }

  @Test
  public void wrapWithSsl_withNullSocket_throwsException() {
    assertThrows(
        NullPointerException.class,
        () -> tsunamiSocketFactory.wrapWithSsl(null, "example.com", 443, true));
  }

  @Test
  public void wrapWithSsl_withInvalidPort_throwsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> tsunamiSocketFactory.wrapWithSsl(mockSocket, "example.com", 0, true));
  }

  @Test
  public void getDefaultConnectTimeout_returnsConfiguredValue() {
    assertThat(tsunamiSocketFactory.getDefaultConnectTimeout()).isEqualTo(DEFAULT_CONNECT_TIMEOUT);
  }

  @Test
  public void getDefaultReadTimeout_returnsConfiguredValue() {
    assertThat(tsunamiSocketFactory.getDefaultReadTimeout()).isEqualTo(DEFAULT_READ_TIMEOUT);
  }
}
