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
package com.google.tsunami.common.reflection;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.AbstractModule;
import io.github.classgraph.ScanResult;

/** Guice module for providing ClassGraph bindings. */
public final class ClassGraphModule extends AbstractModule {
  private final ScanResult scanResult;

  public ClassGraphModule(ScanResult scanResult) {
    this.scanResult = checkNotNull(scanResult);
  }

  @Override
  protected void configure() {
    bind(ScanResult.class).annotatedWith(RuntimeClassGraphScanResult.class).toInstance(scanResult);
  }
}
