/*
 * Copyright 2026 Google LLC
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.flogger.GoogleLogger;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Default implementation of {@link TsunamiSocketFactory} that creates TCP sockets.
 *
 * <p>This implementation wraps the standard Java {@link SocketFactory} and {@link SSLSocketFactory}
 * to ensure that all created sockets have proper timeout settings configured, preventing plugins
 * from hanging indefinitely.
 */
public final class DefaultTsunamiSocketFactory implements TsunamiSocketFactory {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private final SocketFactory socketFactory;
  private final SSLSocketFactory sslSocketFactory;
  private final Duration defaultConnectTimeout;
  private final Duration defaultReadTimeout;

  /**
   * Creates a new DefaultTsunamiSocketFactory.
   *
   * @param socketFactory the underlying socket factory to use for plain TCP connections
   * @param sslSocketFactory the underlying SSL socket factory to use for SSL/TLS connections
   * @param defaultConnectTimeout the default timeout for establishing connections
   * @param defaultReadTimeout the default timeout for read operations
   */
  public DefaultTsunamiSocketFactory(
      SocketFactory socketFactory,
      SSLSocketFactory sslSocketFactory,
      Duration defaultConnectTimeout,
      Duration defaultReadTimeout) {
    this.socketFactory = checkNotNull(socketFactory);
    this.sslSocketFactory = checkNotNull(sslSocketFactory);
    this.defaultConnectTimeout = checkNotNull(defaultConnectTimeout);
    this.defaultReadTimeout = checkNotNull(defaultReadTimeout);

    checkArgument(!defaultConnectTimeout.isNegative(), "Connect timeout cannot be negative");
    checkArgument(!defaultReadTimeout.isNegative(), "Read timeout cannot be negative");

    logger.atInfo().log(
        "TsunamiSocketFactory initialized with connect timeout: %s, read timeout: %s",
        defaultConnectTimeout, defaultReadTimeout);
  }

  @Override
  public Socket createSocket(String host, int port) throws IOException {
    return createSocket(host, port, defaultConnectTimeout, defaultReadTimeout);
  }

  @Override
  public Socket createSocket(String host, int port, Duration timeout) throws IOException {
    return createSocket(host, port, timeout, timeout);
  }

  @Override
  public Socket createSocket(String host, int port, Duration connectTimeout, Duration readTimeout)
      throws IOException {
    checkNotNull(host);
    checkArgument(port > 0 && port <= 65535, "Port must be between 1 and 65535");
    checkNotNull(connectTimeout);
    checkNotNull(readTimeout);

    logger.atFine().log(
        "Creating socket to %s:%d with connect timeout %s, read timeout %s",
        host, port, connectTimeout, readTimeout);

    Socket socket = socketFactory.createSocket();
    configureAndConnect(socket, new InetSocketAddress(host, port), connectTimeout, readTimeout);
    return socket;
  }

  @Override
  public Socket createSocket(InetAddress address, int port) throws IOException {
    return createSocket(address, port, defaultConnectTimeout, defaultReadTimeout);
  }

  @Override
  public Socket createSocket(InetAddress address, int port, Duration timeout) throws IOException {
    return createSocket(address, port, timeout, timeout);
  }

  @Override
  public Socket createSocket(
      InetAddress address, int port, Duration connectTimeout, Duration readTimeout)
      throws IOException {
    checkNotNull(address);
    checkArgument(port > 0 && port <= 65535, "Port must be between 1 and 65535");
    checkNotNull(connectTimeout);
    checkNotNull(readTimeout);

    logger.atFine().log(
        "Creating socket to %s:%d with connect timeout %s, read timeout %s",
        address.getHostAddress(), port, connectTimeout, readTimeout);

    Socket socket = socketFactory.createSocket();
    configureAndConnect(socket, new InetSocketAddress(address, port), connectTimeout, readTimeout);
    return socket;
  }

  @Override
  public Socket createUnconnectedSocket() throws IOException {
    Socket socket = socketFactory.createSocket();
    socket.setSoTimeout((int) defaultReadTimeout.toMillis());
    logger.atFine().log("Created unconnected socket with read timeout %s", defaultReadTimeout);
    return socket;
  }

  @Override
  public SSLSocket createSslSocket(String host, int port) throws IOException {
    return createSslSocket(host, port, defaultConnectTimeout, defaultReadTimeout);
  }

  @Override
  public SSLSocket createSslSocket(String host, int port, Duration timeout) throws IOException {
    return createSslSocket(host, port, timeout, timeout);
  }

  @Override
  public SSLSocket createSslSocket(
      String host, int port, Duration connectTimeout, Duration readTimeout) throws IOException {
    checkNotNull(host);
    checkArgument(port > 0 && port <= 65535, "Port must be between 1 and 65535");
    checkNotNull(connectTimeout);
    checkNotNull(readTimeout);

    logger.atFine().log(
        "Creating SSL socket to %s:%d with connect timeout %s, read timeout %s",
        host, port, connectTimeout, readTimeout);

    // Create a plain socket first, connect with timeout, then wrap with SSL
    Socket plainSocket = socketFactory.createSocket();
    configureAndConnect(
        plainSocket, new InetSocketAddress(host, port), connectTimeout, readTimeout);

    SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(plainSocket, host, port, true);
    sslSocket.setSoTimeout((int) readTimeout.toMillis());
    sslSocket.startHandshake();

    return sslSocket;
  }

  @Override
  public SSLSocket createSslSocket(InetAddress address, int port) throws IOException {
    return createSslSocket(address, port, defaultConnectTimeout, defaultReadTimeout);
  }

  @Override
  public SSLSocket createSslSocket(InetAddress address, int port, Duration timeout)
      throws IOException {
    return createSslSocket(address, port, timeout, timeout);
  }

  @Override
  public SSLSocket createSslSocket(
      InetAddress address, int port, Duration connectTimeout, Duration readTimeout)
      throws IOException {
    checkNotNull(address);
    checkArgument(port > 0 && port <= 65535, "Port must be between 1 and 65535");
    checkNotNull(connectTimeout);
    checkNotNull(readTimeout);

    logger.atFine().log(
        "Creating SSL socket to %s:%d with connect timeout %s, read timeout %s",
        address.getHostAddress(), port, connectTimeout, readTimeout);

    // Create a plain socket first, connect with timeout, then wrap with SSL
    Socket plainSocket = socketFactory.createSocket();
    configureAndConnect(
        plainSocket, new InetSocketAddress(address, port), connectTimeout, readTimeout);

    SSLSocket sslSocket =
        (SSLSocket)
            sslSocketFactory.createSocket(plainSocket, address.getHostAddress(), port, true);
    sslSocket.setSoTimeout((int) readTimeout.toMillis());
    sslSocket.startHandshake();

    return sslSocket;
  }

  @Override
  public SSLSocket wrapWithSsl(Socket socket, String host, int port, boolean autoClose)
      throws IOException {
    checkNotNull(socket);
    checkNotNull(host);
    checkArgument(port > 0 && port <= 65535, "Port must be between 1 and 65535");

    logger.atFine().log("Wrapping existing socket with SSL for host %s:%d", host, port);

    SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(socket, host, port, autoClose);
    // Preserve the timeout from the original socket if set, otherwise use default
    int originalTimeout = socket.getSoTimeout();
    if (originalTimeout > 0) {
      sslSocket.setSoTimeout(originalTimeout);
    } else {
      sslSocket.setSoTimeout((int) defaultReadTimeout.toMillis());
    }
    sslSocket.startHandshake();

    return sslSocket;
  }

  @Override
  public Duration getDefaultConnectTimeout() {
    return defaultConnectTimeout;
  }

  @Override
  public Duration getDefaultReadTimeout() {
    return defaultReadTimeout;
  }

  /**
   * Configures socket options and connects to the specified address with timeout.
   *
   * @param socket the socket to configure and connect
   * @param address the address to connect to
   * @param connectTimeout the timeout for establishing the connection
   * @param readTimeout the timeout for read operations
   * @throws IOException if an I/O error occurs
   */
  private void configureAndConnect(
      Socket socket, InetSocketAddress address, Duration connectTimeout, Duration readTimeout)
      throws IOException {
    // Set read timeout before connecting
    socket.setSoTimeout((int) readTimeout.toMillis());

    // Enable TCP keep-alive to detect dead connections
    socket.setKeepAlive(true);

    // Disable Nagle's algorithm for better latency in security scanning
    socket.setTcpNoDelay(true);

    // Connect with timeout
    socket.connect(address, (int) connectTimeout.toMillis());

    logger.atFine().log(
        "Socket connected to %s with SO_TIMEOUT=%dms", address, readTimeout.toMillis());
  }
}
