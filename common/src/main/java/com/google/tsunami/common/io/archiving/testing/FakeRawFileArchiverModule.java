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
package com.google.tsunami.common.io.archiving.testing;

import com.google.inject.AbstractModule;
import com.google.tsunami.common.io.archiving.RawFileArchiver;
import javax.inject.Singleton;

/** Installs {@link FakeRawFileArchiver}. */
public final class FakeRawFileArchiverModule extends AbstractModule {

  @Override
  protected void configure() {
    // This is intentional to create 2 separate bindings. One is for FakeRawFileArchiver itself,
    // which always injects as a singleton. The other one links the binding for RawFileArchiver to
    // FakeRawFileArchiver so that the FakeRawFileArchiver singleton instance is injected to
    // RawFileArchiver. This way the classes on the inheritance chain always get the same instance.
    //
    // This is useful in unit test. Test cases now are able to get the same injected instance as the
    // code under test.
    bind(FakeRawFileArchiver.class).in(Singleton.class);
    bind(RawFileArchiver.class).to(FakeRawFileArchiver.class);
  }
}
