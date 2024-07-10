/*
 * Copyright 2022 Google LLC
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
package com.google.tsunami.main.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableList;
import com.google.tsunami.common.cli.CliOption;
import com.google.tsunami.common.data.NetworkEndpointUtils;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/** Command line arguments for Tsunami language servers. */
@Parameters(separators = "=")
public final class LanguageServerOptions implements CliOption {

  @Parameter(
      names = "--plugin-server-paths",
      description = "The filename of the language server to run language-specific plugins.")
  public List<String> pluginServerFilenames = ImmutableList.of();

  @Parameter(
      names = "--plugin-server-ports",
      description =
          "The port of the plugin server to open connection with. If not enough ports were"
              + " specified for the number of language servers specified, an open port will be"
              + " chosen.")
  public List<String> pluginServerPorts = ImmutableList.of();

  @Parameter(
      names = "--plugin-server-rpc-deadline-seconds",
      description = "The RPC deadline in seconds for the plugin servers.")
  public List<Integer> pluginServerRpcDeadlineSeconds = ImmutableList.of();

  @Parameter(
      names = {"--remote-plugin-server-addresses", "--python-plugin-server-address"},
      description = "The address for remote language server (e.g. Python).")
  public List<String> remotePluginServerAddress = ImmutableList.of();

  @Parameter(
      names = {"--remote-plugin-server-ports", "--python-plugin-server-port"},
      description = "The port of the remote plugin server to open connection with.")
  public List<Integer> remotePluginServerPort = ImmutableList.of();

  @Parameter(
      names = "--remote-plugin-server-rpc-deadline-seconds",
      description = "The RPC deadline in seconds for this plugin server.")
  public List<Integer> remotePluginServerRpcDeadlineSeconds = ImmutableList.of();

  @Override
  public void validate() {
    if (!pluginServerFilenames.isEmpty() || !pluginServerPorts.isEmpty()) {
      if (pluginServerFilenames != null && !pluginServerFilenames.isEmpty()) {
        for (String pluginServerFilename : pluginServerFilenames) {
          if (!Files.exists(Paths.get(pluginServerFilename))) {
            throw new ParameterException(
                String.format("Language server path %s does not exist", pluginServerFilename));
          }
        }
      }

      if (pluginServerPorts != null && !pluginServerPorts.isEmpty()) {
        for (String pluginServerPort : pluginServerPorts) {
          try {
            int port = Integer.parseInt(pluginServerPort);
            if (!(port <= NetworkEndpointUtils.MAX_PORT_NUMBER && port > 0)) {
              throw new ParameterException(
                  String.format(
                      "Port out of range. Expected [0, %s], actual %s.",
                      NetworkEndpointUtils.MAX_PORT_NUMBER, pluginServerPort));
            }
          } catch (NumberFormatException e) {
            throw new ParameterException(
                String.format("Port number must be an integer. Got %s instead.", pluginServerPort),
                e);
          }
        }
      }

      var pathCounts = pluginServerFilenames == null ? 0 : pluginServerFilenames.size();
      var portCounts = pluginServerPorts == null ? 0 : pluginServerPorts.size();
      if (pathCounts != portCounts) {
        throw new ParameterException(
            String.format(
                "Number of plugin server paths must be equal to number of plugin server ports."
                    + " Paths: %s. Ports: %s.",
                pathCounts, portCounts));
      }

      if (!pluginServerRpcDeadlineSeconds.isEmpty()) {
        if (pluginServerRpcDeadlineSeconds.size() != pathCounts) {
          throw new ParameterException(
              String.format(
                  "Number of plugin server rpc deadlines must be equal to number of plugin server"
                      + " ports. Paths: %s. Ports: %s. Deadlines: %s",
                  pathCounts, portCounts, pluginServerRpcDeadlineSeconds.size()));
        }
      }
    }

    if (!remotePluginServerAddress.isEmpty()) {
      var addrCounts = remotePluginServerAddress.size();
      var portCounts = remotePluginServerPort.size();
      if (addrCounts != portCounts) {
        throw new ParameterException(
            String.format(
                "Number of remote plugin server paths must be equal to number of plugin server "
                    + "ports. Addresses: %s. Ports: %s.",
                addrCounts, portCounts));
      }

      if (!remotePluginServerRpcDeadlineSeconds.isEmpty()) {
        if (remotePluginServerRpcDeadlineSeconds.size() != addrCounts) {
          throw new ParameterException(
              String.format(
                  "Number of plugin server rpc deadlines must be equal to number of plugin server"
                      + " ports. Paths: %s. Ports: %s. Deadlines: %s",
                  addrCounts, portCounts, pluginServerRpcDeadlineSeconds.size()));
        }
      }

      for (int port : remotePluginServerPort) {
        if (!(port <= NetworkEndpointUtils.MAX_PORT_NUMBER && port > 0)) {
          throw new ParameterException(
              String.format(
                  "Remote plugin server port out of range. Expected [0, %s], actual %s.",
                  NetworkEndpointUtils.MAX_PORT_NUMBER, port));
        }
      }
    }
  }
}
