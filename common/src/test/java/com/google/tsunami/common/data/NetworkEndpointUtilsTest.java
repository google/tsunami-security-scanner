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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.net.HostAndPort;
import com.google.tsunami.proto.AddressFamily;
import com.google.tsunami.proto.Hostname;
import com.google.tsunami.proto.IpAddress;
import com.google.tsunami.proto.NetworkEndpoint;
import com.google.tsunami.proto.Port;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link NetworkEndpointUtils}. */
@RunWith(JUnit4.class)
public class NetworkEndpointUtilsTest {

  @Test
  public void isIpV6Endpoint_withIpV4Endpoint_returnsFalse() {
    NetworkEndpoint ipV4Endpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.IP)
            .setIpAddress(
                IpAddress.newBuilder().setAddress("1.2.3.4").setAddressFamily(AddressFamily.IPV4))
            .build();
    assertThat(NetworkEndpointUtils.isIpV6Endpoint(ipV4Endpoint)).isFalse();
  }

  @Test
  public void isIpV6Endpoint_withIpV4AndPortEndpoint_returnsFalse() {
    NetworkEndpoint ipV4AndPortEndpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.IP_PORT)
            .setPort(Port.newBuilder().setPortNumber(8888))
            .setIpAddress(
                IpAddress.newBuilder().setAddress("1.2.3.4").setAddressFamily(AddressFamily.IPV4))
            .build();
    assertThat(NetworkEndpointUtils.isIpV6Endpoint(ipV4AndPortEndpoint)).isFalse();
  }

  @Test
  public void isIpV6Endpoint_withIpV6Endpoint_returnsFalse() {
    NetworkEndpoint ipV6Endpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.IP)
            .setIpAddress(
                IpAddress.newBuilder().setAddress("3ffe::1").setAddressFamily(AddressFamily.IPV6))
            .build();
    assertThat(NetworkEndpointUtils.isIpV6Endpoint(ipV6Endpoint)).isTrue();
  }

  @Test
  public void isIpV6Endpoint_withIpV6AndPortEndpoint_returnsFalse() {
    NetworkEndpoint ipV6AndPortEndpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.IP_PORT)
            .setPort(Port.newBuilder().setPortNumber(8888))
            .setIpAddress(
                IpAddress.newBuilder().setAddress("3ffe::1").setAddressFamily(AddressFamily.IPV6))
            .build();
    assertThat(NetworkEndpointUtils.isIpV6Endpoint(ipV6AndPortEndpoint)).isTrue();
  }

  @Test
  public void isIpV6Endpoint_withHostnameEndpoint_returnsFalse() {
    NetworkEndpoint hostnameEndpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.HOSTNAME)
            .setHostname(Hostname.newBuilder().setName("localhost"))
            .build();
    assertThat(NetworkEndpointUtils.isIpV6Endpoint(hostnameEndpoint)).isFalse();
  }

  @Test
  public void isIpV6Endpoint_withHostnameAndPortEndpoint_returnsFalse() {
    NetworkEndpoint hostnameAndPortEndpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.HOSTNAME_PORT)
            .setPort(Port.newBuilder().setPortNumber(8888))
            .setHostname(Hostname.newBuilder().setName("localhost"))
            .build();
    assertThat(NetworkEndpointUtils.isIpV6Endpoint(hostnameAndPortEndpoint)).isFalse();
  }

  @Test
  public void toUriString_withIpV4Endpoint_returnsIpAddress() {
    NetworkEndpoint ipV4Endpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.IP)
            .setIpAddress(
                IpAddress.newBuilder().setAddress("1.2.3.4").setAddressFamily(AddressFamily.IPV4))
            .build();
    assertThat(NetworkEndpointUtils.toUriAuthority(ipV4Endpoint)).isEqualTo("1.2.3.4");
  }

  @Test
  public void toUriString_withIpV6Endpoint_returnsIpAddressWithBracket() {
    NetworkEndpoint ipV6Endpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.IP)
            .setIpAddress(
                IpAddress.newBuilder().setAddress("3ffe::1").setAddressFamily(AddressFamily.IPV6))
            .build();
    assertThat(NetworkEndpointUtils.toUriAuthority(ipV6Endpoint)).isEqualTo("[3ffe::1]");
  }

  @Test
  public void toUriString_withIpV4AndPortEndpoint_returnsIpAddressAndPort() {
    NetworkEndpoint ipV4AndPortEndpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.IP_PORT)
            .setPort(Port.newBuilder().setPortNumber(8888))
            .setIpAddress(
                IpAddress.newBuilder().setAddress("1.2.3.4").setAddressFamily(AddressFamily.IPV4))
            .build();
    assertThat(NetworkEndpointUtils.toUriAuthority(ipV4AndPortEndpoint)).isEqualTo("1.2.3.4:8888");
  }

  @Test
  public void toUriString_withIpV6AndPortEndpoint_returnsIpAddressWithBracketAndPort() {
    NetworkEndpoint ipV6Endpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.IP_PORT)
            .setPort(Port.newBuilder().setPortNumber(8888))
            .setIpAddress(
                IpAddress.newBuilder().setAddress("3ffe::1").setAddressFamily(AddressFamily.IPV6))
            .build();
    assertThat(NetworkEndpointUtils.toUriAuthority(ipV6Endpoint)).isEqualTo("[3ffe::1]:8888");
  }

  @Test
  public void toUriString_withHostnameEndpoint_returnsHostname() {
    NetworkEndpoint hostnameEndpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.HOSTNAME)
            .setHostname(Hostname.newBuilder().setName("localhost"))
            .build();
    assertThat(NetworkEndpointUtils.toUriAuthority(hostnameEndpoint)).isEqualTo("localhost");
  }

  @Test
  public void toUriString_withHostnameAndPortEndpoint_returnsHostnameAndPort() {
    NetworkEndpoint hostnameAndPortEndpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.HOSTNAME_PORT)
            .setPort(Port.newBuilder().setPortNumber(8888))
            .setHostname(Hostname.newBuilder().setName("localhost"))
            .build();
    assertThat(NetworkEndpointUtils.toUriAuthority(hostnameAndPortEndpoint))
        .isEqualTo("localhost:8888");
  }

  @Test
  public void toHostAndPort_withIpAddress_returnsHostWithIp() {
    NetworkEndpoint ipV4Endpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.IP)
            .setIpAddress(
                IpAddress.newBuilder().setAddress("1.2.3.4").setAddressFamily(AddressFamily.IPV4))
            .build();
    assertThat(NetworkEndpointUtils.toHostAndPort(ipV4Endpoint))
        .isEqualTo(HostAndPort.fromHost("1.2.3.4"));
  }

  @Test
  public void toHostAndPort_withIpAddressAndPort_returnsHostWithIpAndPort() {
    NetworkEndpoint ipV4AndPortEndpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.IP_PORT)
            .setPort(Port.newBuilder().setPortNumber(8888))
            .setIpAddress(
                IpAddress.newBuilder().setAddress("1.2.3.4").setAddressFamily(AddressFamily.IPV4))
            .build();
    assertThat(NetworkEndpointUtils.toHostAndPort(ipV4AndPortEndpoint))
        .isEqualTo(HostAndPort.fromParts("1.2.3.4", 8888));
  }

  @Test
  public void toHostAndPort_withHostname_returnsHostWithHostname() {
    NetworkEndpoint hostnameEndpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.HOSTNAME)
            .setHostname(Hostname.newBuilder().setName("localhost"))
            .build();
    assertThat(NetworkEndpointUtils.toHostAndPort(hostnameEndpoint))
        .isEqualTo(HostAndPort.fromHost("localhost"));
  }

  @Test
  public void toHostAndPort_withHostnameAndPort_returnsHostWithHostnameAndPort() {
    NetworkEndpoint hostnameAndPortEndpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.HOSTNAME_PORT)
            .setPort(Port.newBuilder().setPortNumber(8888))
            .setHostname(Hostname.newBuilder().setName("localhost"))
            .build();
    assertThat(NetworkEndpointUtils.toHostAndPort(hostnameAndPortEndpoint))
        .isEqualTo(HostAndPort.fromParts("localhost", 8888));
  }

  @Test
  public void forIp_withIpV4Address_returnsIpV4NetworkEndpoint() {
    assertThat(NetworkEndpointUtils.forIp("1.2.3.4"))
        .isEqualTo(
            NetworkEndpoint.newBuilder()
                .setType(NetworkEndpoint.Type.IP)
                .setIpAddress(
                    IpAddress.newBuilder()
                        .setAddressFamily(AddressFamily.IPV4)
                        .setAddress("1.2.3.4"))
                .build());
  }

  @Test
  public void forIp_withIpV6Address_returnsIpV6NetworkEndpoint() {
    assertThat(NetworkEndpointUtils.forIp("3ffe::1"))
        .isEqualTo(
            NetworkEndpoint.newBuilder()
                .setType(NetworkEndpoint.Type.IP)
                .setIpAddress(
                    IpAddress.newBuilder()
                        .setAddressFamily(AddressFamily.IPV6)
                        .setAddress("3ffe::1"))
                .build());
  }

  @Test
  public void forIpAndPort_withIpV4AddressAndPort_returnsIpV4AndPortNetworkEndpoint() {
    assertThat(NetworkEndpointUtils.forIpAndPort("1.2.3.4", 8888))
        .isEqualTo(
            NetworkEndpoint.newBuilder()
                .setType(NetworkEndpoint.Type.IP_PORT)
                .setPort(Port.newBuilder().setPortNumber(8888))
                .setIpAddress(
                    IpAddress.newBuilder()
                        .setAddressFamily(AddressFamily.IPV4)
                        .setAddress("1.2.3.4"))
                .build());
  }

  @Test
  public void forIpAndPort_withIpV6AddressAndPort_returnsIpV6AndPortNetworkEndpoint() {
    assertThat(NetworkEndpointUtils.forIpAndPort("3ffe::1", 8888))
        .isEqualTo(
            NetworkEndpoint.newBuilder()
                .setType(NetworkEndpoint.Type.IP_PORT)
                .setPort(Port.newBuilder().setPortNumber(8888))
                .setIpAddress(
                    IpAddress.newBuilder()
                        .setAddressFamily(AddressFamily.IPV6)
                        .setAddress("3ffe::1"))
                .build());
  }

  @Test
  public void forHostname_withHostname_returnsHostnameNetworkEndpoint() {
    assertThat(NetworkEndpointUtils.forHostname("localhost"))
        .isEqualTo(
            NetworkEndpoint.newBuilder()
                .setType(NetworkEndpoint.Type.HOSTNAME)
                .setHostname(Hostname.newBuilder().setName("localhost"))
                .build());
  }

  @Test
  public void forHostnameAndPort_withHostnameAndPort_returnsHostnameAndPortNetworkEndpoint() {
    assertThat(NetworkEndpointUtils.forHostnameAndPort("localhost", 8888))
        .isEqualTo(
            NetworkEndpoint.newBuilder()
                .setType(NetworkEndpoint.Type.HOSTNAME_PORT)
                .setPort(Port.newBuilder().setPortNumber(8888))
                .setHostname(Hostname.newBuilder().setName("localhost"))
                .build());
  }

  @Test
  public void forIpAndHostname_returnsIpAndHostnameNetworkEndpoint() {
    assertThat(NetworkEndpointUtils.forIpAndHostname("1.2.3.4", "host.com"))
        .isEqualTo(
            NetworkEndpoint.newBuilder()
                .setType(NetworkEndpoint.Type.IP_HOSTNAME)
                .setIpAddress(
                    IpAddress.newBuilder()
                        .setAddressFamily(AddressFamily.IPV4)
                        .setAddress("1.2.3.4"))
                .setHostname(Hostname.newBuilder().setName("host.com"))
                .build());
  }

  @Test
  public void forIpHostnameAndPort_returnsIpHostnameAndPortNetworkEndpoint() {
    assertThat(NetworkEndpointUtils.forIpHostnameAndPort("1.2.3.4", "host.com", 8888))
        .isEqualTo(
            NetworkEndpoint.newBuilder()
                .setType(NetworkEndpoint.Type.IP_HOSTNAME_PORT)
                .setIpAddress(
                    IpAddress.newBuilder()
                        .setAddressFamily(AddressFamily.IPV4)
                        .setAddress("1.2.3.4"))
                .setHostname(Hostname.newBuilder().setName("host.com"))
                .setPort(Port.newBuilder().setPortNumber(8888))
                .build());
  }

  @Test
  public void forNetworkEndpointAndPort_withIpEndpointAndPort_returnsIpAndPort() {
    NetworkEndpoint ipEndpoint = NetworkEndpointUtils.forIp("1.2.3.4");

    assertThat(NetworkEndpointUtils.forNetworkEndpointAndPort(ipEndpoint, 8888))
        .isEqualTo(
            NetworkEndpoint.newBuilder()
                .setType(NetworkEndpoint.Type.IP_PORT)
                .setPort(Port.newBuilder().setPortNumber(8888))
                .setIpAddress(
                    IpAddress.newBuilder()
                        .setAddressFamily(AddressFamily.IPV4)
                        .setAddress("1.2.3.4"))
                .build());
  }

  @Test
  public void forNetworkEndpointAndPort_withHostnameEndpointAndPort_returnsHostnameAndPort() {
    NetworkEndpoint hostnameEndpoint = NetworkEndpointUtils.forHostname("localhost");

    assertThat(NetworkEndpointUtils.forNetworkEndpointAndPort(hostnameEndpoint, 8888))
        .isEqualTo(
            NetworkEndpoint.newBuilder()
                .setType(NetworkEndpoint.Type.HOSTNAME_PORT)
                .setPort(Port.newBuilder().setPortNumber(8888))
                .setHostname(Hostname.newBuilder().setName("localhost"))
                .build());
  }

  @Test
  public void forIp_withInvalidIp_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> NetworkEndpointUtils.forIp("abc"));
  }

  @Test
  public void forIpAndPort_withInvalidIp_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class, () -> NetworkEndpointUtils.forIpAndPort("abc", 8888));
  }

  @Test
  public void forIpAndPort_withInvalidPort_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class, () -> NetworkEndpointUtils.forIpAndPort("abc", -1));
    assertThrows(
        IllegalArgumentException.class, () -> NetworkEndpointUtils.forIpAndPort("abc", 65536));
  }

  @Test
  public void forHostname_withIpAddress_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> NetworkEndpointUtils.forHostname("1.2.3.4"));
    assertThrows(IllegalArgumentException.class, () -> NetworkEndpointUtils.forHostname("3ffe::1"));
  }

  @Test
  public void forHostnameAndPort_withIpAddress_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> NetworkEndpointUtils.forHostnameAndPort("1.2.3.4", 8888));
    assertThrows(
        IllegalArgumentException.class,
        () -> NetworkEndpointUtils.forHostnameAndPort("3ffe::1", 8888));
  }

  @Test
  public void forHostnameAndPort_withInvalidPort_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class, () -> NetworkEndpointUtils.forHostnameAndPort("abc", -1));
    assertThrows(
        IllegalArgumentException.class,
        () -> NetworkEndpointUtils.forHostnameAndPort("abc", 65536));
  }

  @Test
  public void forNetworkEndpointAndPort_withInvalidEndpointType_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            NetworkEndpointUtils.forNetworkEndpointAndPort(
                NetworkEndpointUtils.forIpAndPort("1.2.3.4", 80), 8888));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            NetworkEndpointUtils.forNetworkEndpointAndPort(
                NetworkEndpointUtils.forHostnameAndPort("localhost", 80), 8888));
  }

  @Test
  public void forNetworkEndpointAndPort_withInvalidPort_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            NetworkEndpointUtils.forNetworkEndpointAndPort(
                NetworkEndpointUtils.forIp("1.2.3.4"), -1));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            NetworkEndpointUtils.forNetworkEndpointAndPort(
                NetworkEndpointUtils.forHostname("localhost"), 65536));
  }
}
