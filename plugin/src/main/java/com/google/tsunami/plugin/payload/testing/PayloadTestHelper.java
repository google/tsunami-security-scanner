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
package com.google.tsunami.plugin.payload.testing;

import com.google.protobuf.util.JsonFormat;
import com.google.tsunami.callbackserver.proto.PollingResult;
import com.google.tsunami.common.net.http.HttpStatus;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;

/** Exposes some helpers when writing tests against code that use the paylaod generator. */
public final class PayloadTestHelper {

  private PayloadTestHelper() {}

  public static MockResponse generateMockSuccessfulCallbackResponse() throws IOException {
    PollingResult log = PollingResult.newBuilder().setHasHttpInteraction(true).build();
    String body = JsonFormat.printer().preservingProtoFieldNames().print(log);
    return new MockResponse().setResponseCode(HttpStatus.OK.code()).setBody(body);
  }

  public static MockResponse generateMockUnsuccessfulCallbackResponse() {
    return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.code());
  }
}
