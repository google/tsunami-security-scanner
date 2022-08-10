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
      description = "The filename of the language server to run language-spceific plugins.")
  public List<String> pluginServerFilenames;

  @Parameter(
      names = "--plugin-server-ports",
      description =
          "The port of the plugin server to open connection with. If not enough ports were"
              + " specified for the number of language servers specified, an open port will be"
              + " chosen.")
  public List<String> pluginServerPorts;

  @Override
  public void validate() {
    if (pluginServerFilenames != null || pluginServerPorts != null) {
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
            var port = Integer.parseInt(pluginServerPort, 10);
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
    }
  }
}
