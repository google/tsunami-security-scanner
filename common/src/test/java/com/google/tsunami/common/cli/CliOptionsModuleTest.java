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
package com.google.tsunami.common.cli;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.common.base.Strings;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link CliOptionsModule}. */
@RunWith(JUnit4.class)
public class CliOptionsModuleTest {
  @Test
  public void configure_whenValidArgs_parsesSuccessfully() {
    try (ScanResult scanResult =
        new ClassGraph()
            .enableAllInfo()
            .whitelistClasses(
                TestOption.class.getTypeName(), TestOptionWithRequiredParam.class.getTypeName())
            .scan()) {
      Injector injector =
          Guice.createInjector(
              new CliOptionsModule(
                  scanResult, "test", new String[] {"--test=testoption", "--test_required=abc"}));

      TestOption testOption = injector.getInstance(TestOption.class);
      TestOptionWithRequiredParam testOptionWithRequiredParam =
          injector.getInstance(TestOptionWithRequiredParam.class);

      assertThat(testOption.test).isEqualTo("testoption");
      assertThat(testOptionWithRequiredParam.testRequired).isEqualTo("abc");
    }
  }

  @Test
  public void configure_whenMissingRequiredArgs_throwsException() {
    try (ScanResult scanResult =
        new ClassGraph()
            .enableAllInfo()
            .whitelistClasses(
                TestOption.class.getTypeName(), TestOptionWithRequiredParam.class.getTypeName())
            .scan()) {
      CreationException ex =
          assertThrows(
              CreationException.class,
              () ->
                  Guice.createInjector(
                      new CliOptionsModule(scanResult, "test", new String[] {"--test=test"})));
      assertThat(ex).hasCauseThat().isInstanceOf(ParameterException.class);
    }
  }

  @Test
  public void configure_whenInvalidArgs_throwsException() {
    try (ScanResult scanResult =
        new ClassGraph().enableAllInfo().whitelistClasses(TestOption.class.getTypeName()).scan()) {
      CreationException ex =
          assertThrows(
              CreationException.class,
              () ->
                  Guice.createInjector(
                      new CliOptionsModule(scanResult, "test", new String[] {"--test=invalid"})));
      assertThat(ex).hasCauseThat().isInstanceOf(ParameterException.class);
    }
  }

  @Test
  public void configure_whenUnknownArgs_throwsException() {
    try (ScanResult scanResult =
        new ClassGraph().enableAllInfo().whitelistClasses(TestOption.class.getTypeName()).scan()) {
      CreationException ex =
          assertThrows(
              CreationException.class,
              () ->
                  Guice.createInjector(
                      new CliOptionsModule(
                          scanResult, "test", new String[] {"--test=test", "--unknown=unknown"})));
      assertThat(ex).hasCauseThat().isInstanceOf(ParameterException.class);
    }
  }

  @Test
  public void configure_whenCliOptionNoCorrectCtor_throwsException() {
    try (ScanResult scanResult =
        new ClassGraph()
            .enableAllInfo()
            .whitelistClasses(TestOptionWithoutNoArgumentCtor.class.getTypeName())
            .scan()) {
      assertThrows(
          AssertionError.class,
          () ->
              Guice.createInjector(
                  new CliOptionsModule(scanResult, "test", new String[] {})));
    }
  }

  @Parameters(separators = "=")
  private static final class TestOption implements CliOption {
    @Parameter(names = "--test", description = "A test option")
    String test;

    @Override
    public void validate() {
      if (Strings.isNullOrEmpty(test)) {
        throw new ParameterException("Empty test param");
      }

      if (!test.startsWith("test")) {
        throw new ParameterException("test param value must start with 'test'");
      }
    }
  }

  @Parameters(separators = "=")
  private static final class TestOptionWithRequiredParam implements CliOption {
    @Parameter(names = "--test_required", description = "A required option", required = true)
    String testRequired;

    @Override
    public void validate() {}
  }

  @Parameters(separators = "=")
  private static final class TestOptionWithoutNoArgumentCtor implements CliOption {
    String testOption;

    TestOptionWithoutNoArgumentCtor(String testOption) {
      this.testOption = testOption;
    }

    @Override
    public void validate() {}
  }
}
