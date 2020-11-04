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
package com.google.tsunami.common.data;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.net.HostAndPort;
import com.google.common.net.InetAddresses;
import com.google.tsunami.proto.AddressFamily;
import com.google.tsunami.proto.Hostname;
import com.google.tsunami.proto.IpAddress;
import com.google.tsunami.proto.NetworkEndpoint;
import com.google.tsunami.proto.Port;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

/** Static utility methods pertaining to {@link NetworkEndpoint} proto buffer. */
public final class NetworkEndpointUtils {
  public static final int MAX_PORT_NUMBER = 65535;

  private NetworkEndpointUtils() {}

  public static boolean hasIpAddress(NetworkEndpoint networkEndpoint) {
    return networkEndpoint.getType().equals(NetworkEndpoint.Type.IP)
        || networkEndpoint.getType().equals(NetworkEndpoint.Type.IP_PORT)
        || networkEndpoint.getType().equals(NetworkEndpoint.Type.IP_HOSTNAME)
        || networkEndpoint.getType().equals(NetworkEndpoint.Type.IP_HOSTNAME_PORT);
  }

  public static boolean hasHostname(NetworkEndpoint networkEndpoint) {
    return networkEndpoint.getType().equals(NetworkEndpoint.Type.HOSTNAME)
        || networkEndpoint.getType().equals(NetworkEndpoint.Type.HOSTNAME_PORT)
        || networkEndpoint.getType().equals(NetworkEndpoint.Type.IP_HOSTNAME)
        || networkEndpoint.getType().equals(NetworkEndpoint.Type.IP_HOSTNAME_PORT);
  }

  public static boolean hasPort(NetworkEndpoint networkEndpoint) {
    return networkEndpoint.getType().equals(NetworkEndpoint.Type.IP_PORT)
        || networkEndpoint.getType().equals(NetworkEndpoint.Type.HOSTNAME_PORT)
        || networkEndpoint.getType().equals(NetworkEndpoint.Type.IP_HOSTNAME_PORT);
  }

  public static boolean isIpV6Endpoint(NetworkEndpoint networkEndpoint) {
    return hasIpAddress(networkEndpoint)
        && networkEndpoint.getIpAddress().getAddressFamily().equals(AddressFamily.IPV6);
  }

  /**
   * Converts the given {@link NetworkEndpoint} to its uri authority representation.
   *
   * <p>For example:
   *
   * <ul>
   *   <li>ip_v4 = "1.2.3.4" -&gt; uri = "1.2.3.4"
   *   <li>ip_v6 = "3ffe::1" -&gt; uri = "[3ffe::1]"
   *   <li>host = "localhost" -&gt; url = "localhost"
   *   <li>ip_v4 = "1.2.3.4", port = 8888 -&gt; uri = "1.2.3.4:8888"
   *   <li>ip_v6 = "3ffe::1", port = 8888 -&gt; uri = "[3ffe::1]:8888"
   *   <li>host = "localhost", port = 8888 -&gt; url = "localhost:8888"
   * </ul>
   */
  public static String toUriAuthority(NetworkEndpoint networkEndpoint) {
    return toHostAndPort(networkEndpoint).toString();
  }

  public static HostAndPort toHostAndPort(NetworkEndpoint networkEndpoint) {
    switch (networkEndpoint.getType()) {
      case IP:
        return HostAndPort.fromHost(networkEndpoint.getIpAddress().getAddress());
      case IP_PORT:
        return HostAndPort.fromParts(
            networkEndpoint.getIpAddress().getAddress(), networkEndpoint.getPort().getPortNumber());
      case HOSTNAME:
      case IP_HOSTNAME:
        return HostAndPort.fromHost(networkEndpoint.getHostname().getName());
      case HOSTNAME_PORT:
      case IP_HOSTNAME_PORT:
        return HostAndPort.fromParts(
            networkEndpoint.getHostname().getName(), networkEndpoint.getPort().getPortNumber());
      case UNRECOGNIZED:
      case TYPE_UNSPECIFIED:
        throw new AssertionError("Type for NetworkEndpoint must be specified.");
    }

    throw new AssertionError(
        String.format(
            "Should never happen. Unchecked NetworkEndpoint type: %s", networkEndpoint.getType()));
  }

  /** Returns a {@link NetworkEndpoint} proto buffer object from the given ip address. */
  public static NetworkEndpoint forIp(String ipAddress) {
    checkArgument(InetAddresses.isInetAddress(ipAddress), "'%s' is not an IP address.", ipAddress);

    return NetworkEndpoint.newBuilder()
        .setType(NetworkEndpoint.Type.IP)
        .setIpAddress(
            IpAddress.newBuilder()
                .setAddressFamily(ipAddressFamily(ipAddress))
                .setAddress(ipAddress))
        .build();
  }

  /** Returns a {@link NetworkEndpoint} proto buffer object from the given ip address and port. */
  public static NetworkEndpoint forIpAndPort(String ipAddress, int port) {
    checkArgument(InetAddresses.isInetAddress(ipAddress), "'%s' is not an IP address.", ipAddress);
    checkArgument(
        0 <= port && port <= MAX_PORT_NUMBER,
        "Port out of range. Expected [0, %s], actual %s.",
        MAX_PORT_NUMBER,
        port);

    return forIp(ipAddress).toBuilder()
        .setType(NetworkEndpoint.Type.IP_PORT)
        .setPort(Port.newBuilder().setPortNumber(port))
        .build();
  }

  /** Returns a {@link NetworkEndpoint} proto buffer object from the given hostname. */
  public static NetworkEndpoint forHostname(String hostname) {
    checkArgument(
        !InetAddresses.isInetAddress(hostname), "Expected hostname, got IP address '%s'", hostname);

    return NetworkEndpoint.newBuilder()
        .setType(NetworkEndpoint.Type.HOSTNAME)
        .setHostname(Hostname.newBuilder().setName(hostname))
        .build();
  }

  /**
   * Returns a {@link NetworkEndpoint} proto buffer object from the given ip address and hostname.
   */
  public static NetworkEndpoint forIpAndHostname(String ipAddress, String hostname) {
    return forIp(ipAddress).toBuilder()
        .setType(NetworkEndpoint.Type.IP_HOSTNAME)
        .setHostname(Hostname.newBuilder().setName(hostname))
        .build();
  }

  /** Returns a {@link NetworkEndpoint} proto buffer object from the given hostname and port. */
  public static NetworkEndpoint forHostnameAndPort(String hostname, int port) {
    checkArgument(
        0 <= port && port <= MAX_PORT_NUMBER,
        "Port out of range. Expected [0, %s], actual %s.",
        MAX_PORT_NUMBER,
        port);

    return forHostname(hostname).toBuilder()
        .setType(NetworkEndpoint.Type.HOSTNAME_PORT)
        .setPort(Port.newBuilder().setPortNumber(port))
        .build();
  }

  /**
   * Returns a {@link NetworkEndpoint} proto buffer object from the given ip address, hostname and
   * port.
   */
  public static NetworkEndpoint forIpHostnameAndPort(String ipAddress, String hostname, int port) {
    checkArgument(
        0 <= port && port <= MAX_PORT_NUMBER,
        "Port out of range. Expected [0, %s], actual %s.",
        MAX_PORT_NUMBER,
        port);

    return forIpAndHostname(ipAddress, hostname).toBuilder()
        .setType(NetworkEndpoint.Type.IP_HOSTNAME_PORT)
        .setPort(Port.newBuilder().setPortNumber(port))
        .build();
  }

  /**
   * Returns a {@link NetworkEndpoint} proto buffer object from the given {@code networkEndpoint}
   * and port. The {@code networkEndpoint} parameter cannot contain any port information, otherwise
   * {@link IllegalArgumentException} is thrown.
   */
  public static NetworkEndpoint forNetworkEndpointAndPort(
      NetworkEndpoint networkEndpoint, int port) {
    checkNotNull(networkEndpoint);
    checkArgument(
        0 <= port && port <= MAX_PORT_NUMBER,
        "Port out of range. Expected [0, %s], actual %s.",
        MAX_PORT_NUMBER,
        port);

    switch (networkEndpoint.getType()) {
      case IP:
        return networkEndpoint.toBuilder()
            .setType(NetworkEndpoint.Type.IP_PORT)
            .setPort(Port.newBuilder().setPortNumber(port))
            .build();
      case HOSTNAME:
        return networkEndpoint.toBuilder()
            .setType(NetworkEndpoint.Type.HOSTNAME_PORT)
            .setPort(Port.newBuilder().setPortNumber(port))
            .build();
      case IP_HOSTNAME:
        return networkEndpoint.toBuilder()
            .setType(NetworkEndpoint.Type.IP_HOSTNAME_PORT)
            .setPort(Port.newBuilder().setPortNumber(port))
            .build();
      case IP_PORT:
      case HOSTNAME_PORT:
      case IP_HOSTNAME_PORT:
      case UNRECOGNIZED:
      case TYPE_UNSPECIFIED:
        throw new IllegalArgumentException("Invalid NetworkEndpoint type.");
    }
    throw new AssertionError(
        String.format(
            "Should never happen. Unchecked NetworkEndpoint type: %s", networkEndpoint.getType()));
  }

  public static AddressFamily ipAddressFamily(String ipAddress) {
    InetAddress inetAddress = InetAddresses.forString(ipAddress);

    if (inetAddress instanceof Inet4Address) {
      return AddressFamily.IPV4;
    } else if (inetAddress instanceof Inet6Address) {
      return AddressFamily.IPV6;
    } else {
      throw new AssertionError(String.format("Unknown IP address family for IP '%s'", ipAddress));
    }
  }
}
