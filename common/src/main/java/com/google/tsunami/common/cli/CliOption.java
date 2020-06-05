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

/**
 * A marker interface for a subset of command line options used in Tsunami modules.
 *
 * <p>Client should ALWAYS mark its options with this interface so that they can be identified by
 * {@link io.github.classgraph.ClassGraph}. All implementations of {@link CliOption} should provide
 * a no argument constructor or omit constructors completely.
 */
public interface CliOption {

  /**
   * Performs additional validation logic across options defined in the same {@link CliOption}.
   *
   * <p>If validation failed, simply throw a {@link com.beust.jcommander.ParameterException}.
   */
  void validate();
}
