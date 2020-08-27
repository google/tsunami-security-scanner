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

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableMap;
import com.google.tsunami.proto.NetworkService;
import java.util.Optional;

/** Static utility methods pertaining to {@link NetworkService} proto buffer. */
public final class NetworkServiceUtils {
  // Service names are those described in [RFC6335].
  private static final ImmutableMap<String, Boolean> IS_PLAIN_HTTP_BY_KNOWN_WEB_SERVICE_NAME =
      ImmutableMap.<String, Boolean>builder()
          .put("http", true)
          .put("http-alt", true) // Some http server are identified as this rather than "http".
          .put("http-proxy", true)
          .put("https", false)
          .put("radan-http", true) // Port 8088, Hadoop Yarn web UI identified as this.
          .put("ssl/http", false)
          .put("ssl/https", false)
          .build();

  private NetworkServiceUtils() {}

  public static boolean isWebService(Optional<String> serviceName) {
    return serviceName.isPresent()
        && IS_PLAIN_HTTP_BY_KNOWN_WEB_SERVICE_NAME.containsKey(
            Ascii.toLowerCase(serviceName.get()));
  }

  public static boolean isWebService(NetworkService networkService) {
    checkNotNull(networkService);
    return isWebService(Optional.of(networkService.getServiceName()));
  }

  public static boolean isPlainHttp(NetworkService networkService) {
    checkNotNull(networkService);
    return isWebService(networkService)
        && IS_PLAIN_HTTP_BY_KNOWN_WEB_SERVICE_NAME.getOrDefault(
            Ascii.toLowerCase(networkService.getServiceName()), false);
  }

  public static String getServiceName(NetworkService networkService) {
    if (isWebService(networkService) && networkService.hasSoftware()) {
      return Ascii.toLowerCase(networkService.getSoftware().getName());
    }
    return Ascii.toLowerCase(networkService.getServiceName());
  }

  /**
   * Build the root url for a web application service.
   *
   * @param networkService a web (http/https) service
   * @return the root url for the web service, which always ends with a <code>"/"</code>.
   */
  public static String buildWebApplicationRootUrl(NetworkService networkService) {
    checkNotNull(networkService);
    checkArgument(isWebService(networkService));

    String rootUrl =
        (isPlainHttp(networkService) ? "http://" : "https://")
            + buildWebUriAuthority(networkService)
            + buildWebAppRootPath(networkService);
    return rootUrl.endsWith("/") ? rootUrl : rootUrl + "/";
  }

  private static String buildWebAppRootPath(NetworkService networkService) {
    String rootPath =
        networkService.getServiceContext().hasWebServiceContext()
            ? networkService.getServiceContext().getWebServiceContext().getApplicationRoot()
            : "/";
    if (!rootPath.startsWith("/")) {
      rootPath = "/" + rootPath;
    }
    return rootPath;
  }

  private static String buildWebUriAuthority(NetworkService networkService) {
    String uriAuthority = NetworkEndpointUtils.toUriAuthority(networkService.getNetworkEndpoint());

    // Remove default ports of the protocol.
    boolean isPlainHttp = isPlainHttp(networkService);
    if (isPlainHttp && uriAuthority.endsWith(":80")) {
      uriAuthority = uriAuthority.substring(0, uriAuthority.lastIndexOf(":80"));
    }
    if (!isPlainHttp && uriAuthority.endsWith(":443")) {
      uriAuthority = uriAuthority.substring(0, uriAuthority.lastIndexOf(":443"));
    }

    return uriAuthority;
  }
}
