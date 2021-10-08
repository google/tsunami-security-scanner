/*
 * Copyright 2021 Google LLC
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
package com.google.tsunami.common.net.http.javanet;

import java.io.IOException;
import java.net.HttpURLConnection;

/** Given an URL, produces an {@link HttpURLConnection}. */
public interface ConnectionFactory {

  /**
   * Creates a new {@link HttpURLConnection} from the given {@code url}.
   *
   * @param url the URL to which the connection will be made
   * @return the created connection object, which will still be in the pre-connected state
   * @throws IOException if there was a problem producing the connection
   */
  HttpURLConnection openConnection(String url) throws IOException;
}
