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

import static org.junit.Assert.assertThrows;

import com.beust.jcommander.ParameterException;
import com.google.common.collect.ImmutableList;
import com.google.tsunami.common.data.NetworkEndpointUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class LanguageServerOptionsTest {

  @Test
  public void validate_whenPluginServerFilenameDoesNotExist_throwsParameterException() {
    LanguageServerOptions options = new LanguageServerOptions();

    options.pluginServerFilenames = ImmutableList.of("nonexistingfile");

    assertThrows(ParameterException.class, options::validate);
  }

  @Test
  public void validate_whenPortNumberNotInteger_throwsParameterException() {
    LanguageServerOptions options = new LanguageServerOptions();

    options.pluginServerPorts = ImmutableList.of("test");

    assertThrows(ParameterException.class, options::validate);
  }

  @Test
  public void validate_whenPortNumberOutOfRange_throwsParameterException() {
    LanguageServerOptions options = new LanguageServerOptions();

    options.pluginServerPorts = ImmutableList.of("34567", "-1");

    assertThrows(
        "Port out of range. Expected [0, "
            + NetworkEndpointUtils.MAX_PORT_NUMBER
            + "]"
            + ", actual -1",
        ParameterException.class,
        options::validate);
  }
}
