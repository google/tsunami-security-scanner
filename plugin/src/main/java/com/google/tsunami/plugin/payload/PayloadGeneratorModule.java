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
package com.google.tsunami.plugin.payload;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.protobuf.util.JsonFormat;
import com.google.tsunami.common.net.http.HttpClient;
import com.google.tsunami.plugin.TcsClient;
import com.google.tsunami.plugin.TcsConfigProperties;
import com.google.tsunami.proto.PayloadDefinition;
import com.google.tsunami.proto.PayloadGeneratorConfig;
import com.google.tsunami.proto.PayloadLibrary;
import com.google.tsunami.proto.PayloadValidationType;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Guice module for installing {@link PayloadGenerator}. */
public final class PayloadGeneratorModule extends AbstractModule {
  private final SecureRandom secureRng;

  public PayloadGeneratorModule(SecureRandom secureRng) {
    this.secureRng = secureRng;
  }

  @Provides
  TcsClient providesTcsClient(TcsConfigProperties config, HttpClient httpClient) {
    // when all tcs config are not set, we provide an invalid {@link TcsClient}
    // so that {@link TcsClient#isCallbackServerEnabled} returns false.
    if (config.callbackAddress == null
        && config.callbackPort == null
        && config.pollingUri == null) {
      return new TcsClient("", 0, "", checkNotNull(httpClient));
    }

    checkNotNull(config.callbackAddress);
    checkNotNull(config.callbackPort);
    checkNotNull(config.pollingUri);
    checkArgument(
        InetAddresses.isInetAddress(config.callbackAddress)
            || InternetDomainName.isValid(config.callbackAddress),
        "Invalid callback address specified");
    checkArgument(
        config.callbackPort > 0 && config.callbackPort < 65536, "Invalid port number specified");

    return new TcsClient(
        config.callbackAddress, config.callbackPort, config.pollingUri, checkNotNull(httpClient));
  }

  @Provides
  PayloadSecretGenerator providesPayloadSecretGenerator() {
    return new PayloadSecretGenerator(this.secureRng);
  }

  @Provides
  @PayloadGenerator.Payloads
  @Singleton
  ImmutableList<PayloadDefinition> provideParsedPayloads() throws IOException {
    // It is only safe to use SnakeYaml with SafeConstructor.
    // Parse the YAML by converting it into JSON and then into the proto message
    Yaml yaml = new Yaml(new SafeConstructor());
    Map<String, Object> rawYamlData =
        yaml.load(
            Resources.toString(
                Resources.getResource(this.getClass(), "payload_definitions.yaml"),
                UTF_8));

    Gson gson = new Gson();
    String json = gson.toJson(rawYamlData);

    PayloadLibrary.Builder builder = PayloadLibrary.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
    PayloadLibrary parsed = builder.build();

    return validatePayloads(parsed.getPayloadsList());
  }

  /** Validates that the parsed payloads adhere to the defined schema. */
  ImmutableList<PayloadDefinition> validatePayloads(List<PayloadDefinition> payloads) {
    for (PayloadDefinition p : payloads) {
      checkArgument(p.hasName(), "Parsed payload does not have a name.");
      checkArgument(
          p.getInterpretationEnvironment()
              != PayloadGeneratorConfig.InterpretationEnvironment
                  .INTERPRETATION_ENVIRONMENT_UNSPECIFIED,
          "Parsed payload does not have an interpretation_environment.");
      checkArgument(
          p.getExecutionEnvironment()
              != PayloadGeneratorConfig.ExecutionEnvironment.EXECUTION_ENVIRONMENT_UNSPECIFIED,
          "Parsed payload does not have an exeuction_environment.");
      checkArgument(
          !p.getVulnerabilityTypeList().isEmpty(),
          "Parsed payload has no entries for vulnerability_type.");
      checkArgument(p.hasPayloadString(), "Parsed payload does not have a payload_string.");

      if (p.getUsesCallbackServer().getValue()) {
        checkArgument(
            p.getPayloadString().getValue().contains("$TSUNAMI_PAYLOAD_TOKEN_URL"),
            "Parsed payload uses callback server but $TSUNAMI_PAYLOAD_TOKEN_URL not found in"
                + " payload_string.");
      } else {
        checkArgument(
            p.getValidationType() != PayloadValidationType.VALIDATION_TYPE_UNSPECIFIED,
            "Parsed payload has no validation_type and does not use the callback server.");

        if (p.getValidationType() == PayloadValidationType.VALIDATION_REGEX) {
          checkArgument(
              p.hasValidationRegex(),
              "Parsed payload has no validation_regex but uses PayloadValidationType.REGEX");
        }
      }
    }

    return ImmutableList.copyOf(payloads);
  }
}
