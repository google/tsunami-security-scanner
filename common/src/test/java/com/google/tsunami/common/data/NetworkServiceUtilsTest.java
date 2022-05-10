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
import static com.google.tsunami.common.data.NetworkEndpointUtils.forIpAndPort;

import com.google.tsunami.proto.AddressFamily;
import com.google.tsunami.proto.Hostname;
import com.google.tsunami.proto.IpAddress;
import com.google.tsunami.proto.NetworkEndpoint;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.Port;
import com.google.tsunami.proto.ServiceContext;
import com.google.tsunami.proto.Software;
import com.google.tsunami.proto.TransportProtocol;
import com.google.tsunami.proto.WebServiceContext;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link NetworkServiceUtils}. */
@RunWith(JUnit4.class)
public final class NetworkServiceUtilsTest {

  @Test
  public void isWebService_whenHttpService_returnsTrue() {
    assertThat(
            NetworkServiceUtils.isWebService(
                NetworkService.newBuilder().setServiceName("http").build()))
        .isTrue();
  }

  @Test
  public void isWebService_whenHttpAltService_returnsTrue() {
    assertThat(
        NetworkServiceUtils.isWebService(
            NetworkService.newBuilder().setServiceName("http-alt").build()))
        .isTrue();
  }

  @Test
  public void isWebService_whenHttpProxyService_returnsTrue() {
    assertThat(
        NetworkServiceUtils.isWebService(
            NetworkService.newBuilder().setServiceName("http-proxy").build()))
        .isTrue();
  }

  @Test
  public void isWebService_whenHttpsService_returnsTrue() {
    assertThat(
            NetworkServiceUtils.isWebService(
                NetworkService.newBuilder().setServiceName("https").build()))
        .isTrue();
  }

  @Test
  public void isWebService_whenRadanHttpService_returnsTrue() {
    assertThat(
        NetworkServiceUtils.isWebService(
            NetworkService.newBuilder().setServiceName("radan-http").build()))
        .isTrue();
  }

  @Test
  public void isWebService_whenSslHttpService_returnsTrue() {
    assertThat(
            NetworkServiceUtils.isWebService(
                NetworkService.newBuilder().setServiceName("ssl/http").build()))
        .isTrue();
  }

  @Test
  public void isWebService_whenSslHttpsService_returnsTrue() {
    assertThat(
            NetworkServiceUtils.isWebService(
                NetworkService.newBuilder().setServiceName("ssl/https").build()))
        .isTrue();
  }

  @Test
  public void isWebService_whenCapitalizedHttpService_ignoresCaseAndReturnsTrue() {
    assertThat(
            NetworkServiceUtils.isWebService(
                NetworkService.newBuilder().setServiceName("HTTP").build()))
        .isTrue();
  }

  @Test
  public void isWebService_whenNonWebService_returnsFalse() {
    assertThat(
            NetworkServiceUtils.isWebService(
                NetworkService.newBuilder().setServiceName("ssh").build()))
        .isFalse();
  }

  @Test
  public void isPlainHttp_whenPlainHttpService_returnsTrue() {
    assertThat(
            NetworkServiceUtils.isPlainHttp(
                NetworkService.newBuilder().setServiceName("http").build()))
        .isTrue();
  }

  @Test
  public void isPlainHttp_whenHttpAltService_returnsTrue() {
    assertThat(
        NetworkServiceUtils.isPlainHttp(
            NetworkService.newBuilder().setServiceName("http-alt").build()))
        .isTrue();
  }

  @Test
  public void isPlainHttp_whenHttpsService_returnsFalse() {
    assertThat(
            NetworkServiceUtils.isPlainHttp(
                NetworkService.newBuilder().setServiceName("https").build()))
        .isFalse();
  }

  @Test
  public void isPlainHttp_whenRadanHttpService_returnsTrue() {
    assertThat(
        NetworkServiceUtils.isPlainHttp(
            NetworkService.newBuilder().setServiceName("radan-http").build()))
        .isTrue();
  }

  @Test
  public void isPlainHttp_whenNonWebService_returnsFalse() {
    assertThat(
            NetworkServiceUtils.isPlainHttp(
                NetworkService.newBuilder().setServiceName("ssh").build()))
        .isFalse();
  }

  @Test
  public void getServiceName_whenNonWebService_returnsServiceName() {
    assertThat(
            NetworkServiceUtils.getServiceName(
                NetworkService.newBuilder()
                    .setNetworkEndpoint(forIpAndPort("127.0.0.1", 22))
                    .setServiceName("ssh")
                    .build()))
        .isEqualTo("ssh");
  }

  @Test
  public void getServiceName_whenWebServiceNoSoftware_returnsServiceName() {
    assertThat(
        NetworkServiceUtils.getServiceName(
            NetworkService.newBuilder()
                .setNetworkEndpoint(forIpAndPort("127.0.0.1", 22))
                .setServiceName("http")
                .build()))
        .isEqualTo("http");
  }

  @Test
  public void getServiceName_whenWebServiceWithSoftware_returnsServiceName() {
    assertThat(
            NetworkServiceUtils.getServiceName(
                NetworkService.newBuilder()
                    .setNetworkEndpoint(forIpAndPort("127.0.0.1", 22))
                    .setServiceName("http")
                    .setSoftware(Software.newBuilder().setName("WordPress"))
                    .build()))
        .isEqualTo("wordpress");
  }

  @Test
  public void buildWebApplicationRootUrl_whenHttpWithoutRoot_buildsExpectedUrl() {
    assertThat(
            NetworkServiceUtils.buildWebApplicationRootUrl(
                NetworkService.newBuilder()
                    .setNetworkEndpoint(forIpAndPort("127.0.0.1", 8080))
                    .setServiceName("http")
                    .build()))
        .isEqualTo("http://127.0.0.1:8080/");
  }

  @Test
  public void buildWebApplicationRootUrl_whenHttpsWithoutRoot_buildsExpectedUrl() {
    assertThat(
        NetworkServiceUtils.buildWebApplicationRootUrl(
            NetworkService.newBuilder()
                .setNetworkEndpoint(forIpAndPort("127.0.0.1", 8443))
                .setServiceName("ssl/https")
                .setServiceContext(
                    ServiceContext.newBuilder()
                        .setWebServiceContext(
                            WebServiceContext.newBuilder().setApplicationRoot("test_root")))
                .build()))
        .isEqualTo("https://127.0.0.1:8443/test_root/");
  }

  @Test
  public void buildWebApplicationRootUrl_whenHttpWithRootPath_buildsUrlWithExpectedRoot() {
    assertThat(
            NetworkServiceUtils.buildWebApplicationRootUrl(
                NetworkService.newBuilder()
                    .setNetworkEndpoint(forIpAndPort("127.0.0.1", 8080))
                    .setServiceName("http")
                    .setServiceContext(
                        ServiceContext.newBuilder()
                            .setWebServiceContext(
                                WebServiceContext.newBuilder().setApplicationRoot("/test_root")))
                    .build()))
        .isEqualTo("http://127.0.0.1:8080/test_root/");
  }

  @Test
  public void buildWebApplicationRootUrl_whenRootPathNoLeadingSlash_appendsLeadingSlash() {
    assertThat(
        NetworkServiceUtils.buildWebApplicationRootUrl(
            NetworkService.newBuilder()
                .setNetworkEndpoint(forIpAndPort("127.0.0.1", 8080))
                .setServiceName("http")
                .setServiceContext(
                    ServiceContext.newBuilder()
                        .setWebServiceContext(
                            WebServiceContext.newBuilder().setApplicationRoot("test_root")))
                .build()))
        .isEqualTo("http://127.0.0.1:8080/test_root/");
  }

  @Test
  public void buildWebApplicationRootUrl_whenHttpServiceOnPort80_removesTrailingPortFromUrl() {
    assertThat(
        NetworkServiceUtils.buildWebApplicationRootUrl(
            NetworkService.newBuilder()
                .setNetworkEndpoint(forIpAndPort("127.0.0.1", 80))
                .setServiceName("http")
                .setServiceContext(
                    ServiceContext.newBuilder()
                        .setWebServiceContext(
                            WebServiceContext.newBuilder().setApplicationRoot("test_root")))
                .build()))
        .isEqualTo("http://127.0.0.1/test_root/");
  }

  @Test
  public void buildWebApplicationRootUrl_whenHttpsServiceOnPort443_removesTrailingPortFromUrl() {
    assertThat(
        NetworkServiceUtils.buildWebApplicationRootUrl(
            NetworkService.newBuilder()
                .setNetworkEndpoint(forIpAndPort("127.0.0.1", 443))
                .setServiceName("ssl/https")
                .setServiceContext(
                    ServiceContext.newBuilder()
                        .setWebServiceContext(
                            WebServiceContext.newBuilder().setApplicationRoot("test_root")))
                .build()))
        .isEqualTo("https://127.0.0.1/test_root/");
  }

  @Test
  public void buildUriNetworkService_returnsNetworkService() throws IOException {

    URL url = new URL("https://localhost/function1");
    String hostname = url.getHost();
    String ipaddress = InetAddress.getByName(hostname).getHostAddress();
    InetAddress inetAddress = InetAddress.getByName(url.getHost());
    AddressFamily addressFamily =
        inetAddress instanceof Inet4Address ? AddressFamily.IPV4 : AddressFamily.IPV6;

    NetworkEndpoint networkEndpoint =
        NetworkEndpoint.newBuilder()
            .setType(NetworkEndpoint.Type.IP_HOSTNAME_PORT)
            .setIpAddress(
                IpAddress.newBuilder().setAddressFamily(addressFamily).setAddress(ipaddress))
            .setPort(Port.newBuilder().setPortNumber(443))
            .setHostname(Hostname.newBuilder().setName("localhost"))
            .build();

    NetworkService networkService =
        NetworkService.newBuilder()
            .setNetworkEndpoint(networkEndpoint)
            .setTransportProtocol(TransportProtocol.TCP)
            .setServiceName("https")
            .setServiceContext(
                ServiceContext.newBuilder()
                    .setWebServiceContext(
                        WebServiceContext.newBuilder().setApplicationRoot("/function1")))
            .build();

    assertThat(NetworkServiceUtils.buildUriNetworkService("https://localhost/function1"))
        .isEqualTo(networkService);
  }
}
