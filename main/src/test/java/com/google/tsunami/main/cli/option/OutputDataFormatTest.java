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
package com.google.tsunami.main.cli.option;

import static com.google.common.truth.Truth8.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link OutputDataFormat}. */
@RunWith(JUnit4.class)
public class OutputDataFormatTest {

  @Test
  public void parse_whenStringMatchesExactly_returnsParsedOutputDataFormat() {
    assertThat(OutputDataFormat.parse("BIN_PROTO")).hasValue(OutputDataFormat.BIN_PROTO);
    assertThat(OutputDataFormat.parse("JSON")).hasValue(OutputDataFormat.JSON);
  }

  @Test
  public void parse_whenStringMatchesIgnoringCases_returnsParsedOutputDataFormat() {
    assertThat(OutputDataFormat.parse("bin_proto")).hasValue(OutputDataFormat.BIN_PROTO);
    assertThat(OutputDataFormat.parse("Json")).hasValue(OutputDataFormat.JSON);
  }

  @Test
  public void parse_whenStringNotMatch_returnsEmpty() {
    assertThat(OutputDataFormat.parse("xml")).isEmpty();
  }
}
