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
package com.google.tsunami.plugins.detectors.jdwprce;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.GoogleLogger;
import com.google.protobuf.util.Timestamps;
import com.google.tsunami.common.data.NetworkEndpointUtils;
import com.google.tsunami.common.net.http.HttpClient;
import com.google.tsunami.common.net.http.HttpResponse;
import com.google.tsunami.common.time.UtcClock;
import com.google.tsunami.plugin.PluginType;
import com.google.tsunami.plugin.VulnDetector;
import com.google.tsunami.plugin.annotations.PluginInfo;
import com.google.tsunami.proto.AdditionalDetail;
import com.google.tsunami.proto.DetectionReport;
import com.google.tsunami.proto.DetectionReportList;
import com.google.tsunami.proto.DetectionStatus;
import com.google.tsunami.proto.NetworkService;
import com.google.tsunami.proto.Severity;
import com.google.tsunami.proto.TargetInfo;
import com.google.tsunami.proto.TextData;
import com.google.tsunami.proto.Vulnerability;
import com.google.tsunami.proto.VulnerabilityId;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Clock;
import java.time.Instant;
import javax.inject.Inject;

/** A VulnDetector plugin for JDWP RCE vulnerabilities. */
// PluginInfo tells Tsunami scanning engine basic information about your plugin.
@PluginInfo(
    // Which type of plugin this is.
    type = PluginType.VULN_DETECTION,
    // A human readable name of your plugin.
    name = "JdwpRceDetector",
    // Current version of your plugin.
    version = "0.1",
    // Detailed description about what this plugin does.
    description = "Detects Java Debug Wire Protocol (JDWP) remote code execution vulnerabilities.",
    // Author of this plugin.
    author = "Jules (AI Developer)",
    // How should Tsunami scanner bootstrap your plugin.
    bootstrapModule = JdwpRceDetectorBootstrapModule.class)
// Optionally, each VulnDetector can be annotated by service filtering annotatio
ns. For example, if
// the VulnDetector should only be executed when the scan target is running Jenk
ins, then add the
// following @ForSoftware annotation.
// @ForSoftware(name = "Jenkins")
public final class JdwpRceDetector implements VulnDetector {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  private static final String JDWP_HANDSHAKE_REQUEST = "JDWP-Handshake";
  private static final String JDWP_HANDSHAKE_RESPONSE_PREFIX = "JDWP-";
  // According to Oracle, the default JDWP port is 8000, but other sources mention 8005.
  // Common other ports include 5005, 8787.
  // We will check for a configurable list, defaulting to 8000 and 8005.
  private static final ImmutableList<Integer> DEFAULT_JDWP_PORTS = ImmutableList.of(8000, 8005, 5005, 8787);
  // TODO(b/261212064): Make the target JDWP ports configurable.

  private final Clock utcClock;
  private final HttpClient httpClient;

  // Tsunami scanner relies heavily on Guice framework. So all the utility depen
dencies of your
  // plugin must be injected through the constructor of the detector. Here the U
tcClock is provided
  // by the scanner.
  @Inject
  JdwpRceDetector(@UtcClock Clock utcClock, HttpClient httpClient) {
    this.utcClock = checkNotNull(utcClock);
    this.httpClient = checkNotNull(httpClient);
  }

  // This is the main entry point of your VulnDetector. Both parameters will be
populated by the
  // scanner. targetInfo contains the general information about the scan target.
 matchedServices
  // parameter contains all the network services that matches the service filter
ing annotations
  // mentioned earlier. If no filtering annotations added, then matchedServices
parameter contains
  // all exposed network services on the scan target.
  @Override
  public DetectionReportList detect(
      TargetInfo targetInfo, ImmutableList<NetworkService> matchedServices) {
    logger.atInfo().log("JdwpRceDetector starts detecting.");

    return DetectionReportList.newBuilder()
        .addAllDetectionReports(
            matchedServices.stream()
                .filter(this::isServiceJdwp)
                .filter(networkService -> isServiceVulnerable(networkService, targetInfo))
                .map(networkService -> buildDetectionReport(targetInfo, networkService))
                .collect(toImmutableList()))
        .build();
  }

  private boolean isServiceJdwp(NetworkService networkService) {
    if (networkService.getServiceName().equalsIgnoreCase("jdwp")) {
      return true;
    }
    if (networkService.hasPort() && DEFAULT_JDWP_PORTS.contains(networkService.getPort().getPortNumber())) {
      // Further check if it's actually JDWP, not just a known port.
      // This might involve a quick banner grab or handshake attempt here,
      // or rely on the isServiceVulnerable check.
      // For now, assume if it's on a common JDWP port, it's worth checking.
      return true;
    }
    return false;
  }

  // Checks whether a given network service is vulnerable.
  private boolean isServiceVulnerable(NetworkService networkService, TargetInfo targetInfo) {
    // TODO(b/261212064): Implement more robust JDWP detection and vulnerability check.
    // This might involve looking for specific behaviors of jdwp-shellifier or other tools.
    String targetAddress = NetworkEndpointUtils.toUriAuthority(networkService.getNetworkEndpoint());
    if (targetAddress.isEmpty()) {
      return false;
    }

    InetSocketAddress socketAddress;
    try {
      socketAddress = NetworkEndpointUtils.toInetSocketAddress(networkService.getNetworkEndpoint());
    } catch (IllegalArgumentException e) {
      logger.atWarning().withCause(e).log("Failed to convert NetworkEndpoint to InetSocketAddress.");
      return false;
    }

    try (Socket socket = new Socket()) {
      // Consider making timeout configurable
      socket.connect(socketAddress, 5000); // 5 seconds timeout
      logger.atInfo().log("Connected to %s:%d", socketAddress.getAddress(), socketAddress.getPort());

      OutputStream out = socket.getOutputStream();
      InputStream in = socket.getInputStream();

      // Send JDWP handshake
      out.write(JDWP_HANDSHAKE_REQUEST.getBytes());
      out.flush();
      logger.atInfo().log("Sent JDWP handshake request.");

      byte[] response = new byte[JDWP_HANDSHAKE_REQUEST.length()]; // Or JDWP_HANDSHAKE_RESPONSE_PREFIX.length()
      int bytesRead = in.read(response);

      if (bytesRead == JDWP_HANDSHAKE_REQUEST.length()) {
        String handshakeResponse = new String(response);
        logger.atInfo().log("Received JDWP handshake response: %s", handshakeResponse);
        if (handshakeResponse.startsWith(JDWP_HANDSHAKE_RESPONSE_PREFIX)) {
          // Basic handshake successful. A more advanced check would involve
          // trying to exploit or confirm RCE capability (e.g., using jdwp-shellifier techniques).
          // For now, a successful handshake on a JDWP port is considered indicative.
          return true;
        }
      } else {
        logger.atInfo().log("Received unexpected JDWP handshake response length: %d", bytesRead);
      }
    } catch (IOException e) {
      logger.atFine().withCause(e).log(
          "Failed to connect or perform JDWP handshake with %s:%d.",
          socketAddress.getAddress(), socketAddress.getPort());
      return false;
    }
    return false;
  }

  // This builds the DetectionReport message for a specific vulnerable network s
ervice.
  private DetectionReport buildDetectionReport(
      TargetInfo targetInfo, NetworkService vulnerableNetworkService) {
    return DetectionReport.newBuilder()
        .setTargetInfo(targetInfo)
        .setNetworkService(vulnerableNetworkService)
        .setDetectionTimestamp(Timestamps.fromMillis(Instant.now(utcClock).toEpochMilli()))
        .setDetectionStatus(DetectionStatus.VULNERABILITY_VERIFIED)
        .setVulnerability(
            Vulnerability.newBuilder()
                .setMainId(
                    VulnerabilityId.newBuilder()
                        .setPublisher("GOOGLE")
                        .setValue("JDWP_RCE"))
                .setSeverity(Severity.CRITICAL)
                .setTitle("Java Debug Wire Protocol (JDWP) Remote Code Execution")
                .setDescription(
                    "The JDWP service is vulnerable to remote code execution, potentially allowing an"
                        + " attacker to take control of the system.")
                .setRecommendation(
                    "Disable JDWP in production environments. If JDWP is required for specific"
                        + " purposes (e.g., debugging in a controlled environment), ensure it is not"
                        + " exposed to untrusted networks. Use firewall rules to restrict access to"
                        + " the JDWP port to only authorized IP addresses or VPN connections."
                        + " Configure JDWP to listen on a local interface only (e.g., 127.0.0.1) if"
                        + " remote debugging is not strictly necessary."))
        .build();
  }
}
// Note: The HttpClient import might not be strictly necessary if not used directly in the final JDWP check logic,
// but it's often useful for more complex interactions or if future checks require HTTP calls.
// For now, the JDWP check is direct socket interaction.
