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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.time.Duration;
import javax.net.ssl.SSLSocket;

/**
 * A socket factory API for creating TCP sockets with enforced default configurations.
 *
 * <p>This API provides a normalized way to create sockets from Tsunami plugins, ensuring that
 * proper timeouts are always configured. This prevents plugins from hanging indefinitely when
 * servers don't respond.
 *
 * <p>Plugins should use this factory instead of directly creating sockets through
 * {@link javax.net.SocketFactory} or {@link javax.net.ssl.SSLSocketFactory} to ensure consistent
 * behavior and proper timeout handling.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Inject
 * TsunamiSocketFactory socketFactory;
 *
 * // Create a socket with default timeouts
 * Socket socket = socketFactory.createSocket("example.com", 80);
 *
 * // Create a socket with custom timeouts
 * Socket socket = socketFactory.createSocket("example.com", 80,
 *     Duration.ofSeconds(5), Duration.ofSeconds(10));
 *
 * // Create an SSL socket
 * SSLSocket sslSocket = socketFactory.createSslSocket("example.com", 443);
 * }</pre>
 */
public interface TsunamiSocketFactory {

  /**
   * Creates a TCP socket connected to the specified host and port with default timeouts.
   *
   * <p>The socket will be configured with the default connect and read timeouts specified in the
   * Tsunami configuration. If no configuration is provided, sensible defaults will be used.
   *
   * @param host the host to connect to
   * @param port the port to connect to
   * @return a connected socket with enforced timeouts
   * @throws IOException if an I/O error occurs while creating the socket
   */
  Socket createSocket(String host, int port) throws IOException;

  /**
   * Creates a TCP socket connected to the specified host and port with a single timeout.
   *
   * <p>The specified timeout will be used for both connection establishment and read operations.
   *
   * @param host the host to connect to
   * @param port the port to connect to
   * @param timeout the timeout for both connection and read operations
   * @return a connected socket with the specified timeout
   * @throws IOException if an I/O error occurs while creating the socket
   */
  Socket createSocket(String host, int port, Duration timeout) throws IOException;

  /**
   * Creates a TCP socket connected to the specified host and port with custom timeouts.
   *
   * @param host the host to connect to
   * @param port the port to connect to
   * @param connectTimeout the timeout for establishing the connection
   * @param readTimeout the timeout for read operations (SO_TIMEOUT)
   * @return a connected socket with the specified timeouts
   * @throws IOException if an I/O error occurs while creating the socket
   */
  Socket createSocket(String host, int port, Duration connectTimeout, Duration readTimeout)
      throws IOException;

  /**
   * Creates a TCP socket connected to the specified address and port with default timeouts.
   *
   * @param address the IP address to connect to
   * @param port the port to connect to
   * @return a connected socket with enforced timeouts
   * @throws IOException if an I/O error occurs while creating the socket
   */
  Socket createSocket(InetAddress address, int port) throws IOException;

  /**
   * Creates a TCP socket connected to the specified address and port with a single timeout.
   *
   * <p>The specified timeout will be used for both connection establishment and read operations.
   *
   * @param address the IP address to connect to
   * @param port the port to connect to
   * @param timeout the timeout for both connection and read operations
   * @return a connected socket with the specified timeout
   * @throws IOException if an I/O error occurs while creating the socket
   */
  Socket createSocket(InetAddress address, int port, Duration timeout) throws IOException;

  /**
   * Creates a TCP socket connected to the specified address and port with custom timeouts.
   *
   * @param address the IP address to connect to
   * @param port the port to connect to
   * @param connectTimeout the timeout for establishing the connection
   * @param readTimeout the timeout for read operations (SO_TIMEOUT)
   * @return a connected socket with the specified timeouts
   * @throws IOException if an I/O error occurs while creating the socket
   */
  Socket createSocket(
      InetAddress address, int port, Duration connectTimeout, Duration readTimeout)
      throws IOException;

  /**
   * Creates an unconnected TCP socket with default timeouts configured.
   *
   * <p>The returned socket will have SO_TIMEOUT set to the default read timeout. The caller is
   * responsible for connecting the socket, which should be done with a timeout.
   *
   * @return an unconnected socket with default read timeout configured
   * @throws IOException if an I/O error occurs while creating the socket
   */
  Socket createUnconnectedSocket() throws IOException;

  /**
   * Creates an SSL/TLS socket connected to the specified host and port with default timeouts.
   *
   * <p>The socket will be configured with the default connect and read timeouts. SSL/TLS handshake
   * will be performed automatically.
   *
   * @param host the host to connect to
   * @param port the port to connect to
   * @return a connected SSL socket with enforced timeouts
   * @throws IOException if an I/O error occurs while creating the socket
   */
  SSLSocket createSslSocket(String host, int port) throws IOException;

  /**
   * Creates an SSL/TLS socket connected to the specified host and port with a single timeout.
   *
   * <p>The specified timeout will be used for both connection establishment and read operations.
   *
   * @param host the host to connect to
   * @param port the port to connect to
   * @param timeout the timeout for both connection and read operations
   * @return a connected SSL socket with the specified timeout
   * @throws IOException if an I/O error occurs while creating the socket
   */
  SSLSocket createSslSocket(String host, int port, Duration timeout) throws IOException;

  /**
   * Creates an SSL/TLS socket connected to the specified host and port with custom timeouts.
   *
   * @param host the host to connect to
   * @param port the port to connect to
   * @param connectTimeout the timeout for establishing the connection
   * @param readTimeout the timeout for read operations (SO_TIMEOUT)
   * @return a connected SSL socket with the specified timeouts
   * @throws IOException if an I/O error occurs while creating the socket
   */
  SSLSocket createSslSocket(String host, int port, Duration connectTimeout, Duration readTimeout)
      throws IOException;

  /**
   * Creates an SSL/TLS socket connected to the specified address and port with default timeouts.
   *
   * @param address the IP address to connect to
   * @param port the port to connect to
   * @return a connected SSL socket with enforced timeouts
   * @throws IOException if an I/O error occurs while creating the socket
   */
  SSLSocket createSslSocket(InetAddress address, int port) throws IOException;

  /**
   * Creates an SSL/TLS socket connected to the specified address and port with a single timeout.
   *
   * <p>The specified timeout will be used for both connection establishment and read operations.
   *
   * @param address the IP address to connect to
   * @param port the port to connect to
   * @param timeout the timeout for both connection and read operations
   * @return a connected SSL socket with the specified timeout
   * @throws IOException if an I/O error occurs while creating the socket
   */
  SSLSocket createSslSocket(InetAddress address, int port, Duration timeout) throws IOException;

  /**
   * Creates an SSL/TLS socket connected to the specified address and port with custom timeouts.
   *
   * @param address the IP address to connect to
   * @param port the port to connect to
   * @param connectTimeout the timeout for establishing the connection
   * @param readTimeout the timeout for read operations (SO_TIMEOUT)
   * @return a connected SSL socket with the specified timeouts
   * @throws IOException if an I/O error occurs while creating the socket
   */
  SSLSocket createSslSocket(
      InetAddress address, int port, Duration connectTimeout, Duration readTimeout)
      throws IOException;

  /**
   * Wraps an existing socket with SSL/TLS.
   *
   * <p>This method is useful when you need to upgrade a plain TCP connection to SSL/TLS, such as
   * when implementing STARTTLS protocols.
   *
   * @param socket the existing socket to wrap
   * @param host the hostname for SNI (Server Name Indication)
   * @param port the port number
   * @param autoClose whether the underlying socket should be closed when the SSL socket is closed
   * @return an SSL socket wrapping the existing socket
   * @throws IOException if an I/O error occurs while creating the SSL socket
   */
  SSLSocket wrapWithSsl(Socket socket, String host, int port, boolean autoClose) throws IOException;

  /**
   * Returns the default connect timeout configured for this factory.
   *
   * @return the default connect timeout
   */
  Duration getDefaultConnectTimeout();

  /**
   * Returns the default read timeout configured for this factory.
   *
   * @return the default read timeout
   */
  Duration getDefaultReadTimeout();
}
