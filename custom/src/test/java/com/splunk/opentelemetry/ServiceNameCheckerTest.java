/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceNameCheckerTest {
  @Mock Consumer<String> logWarn;
  @Mock AutoConfiguredOpenTelemetrySdk autoConfiguredSdk;

  @Test
  void shouldLogWarnWhenNeitherServiceNameNorResourceAttributeIsConfigured() {
    // given
    var config = Config.builder().build();

    var underTest = new ServiceNameChecker(logWarn);

    // when
    underTest.beforeAgent(config, autoConfiguredSdk);

    // then
    verify(logWarn).accept(anyString());
  }

  @Test
  void shouldNotLogWarnWhenServiceNameIsConfigured() {
    // given
    var config = Config.builder().addProperty("otel.service.name", "test").build();

    var underTest = new ServiceNameChecker(logWarn);

    // when
    underTest.beforeAgent(config, autoConfiguredSdk);

    // then
    verifyNoInteractions(logWarn);
  }

  @Test
  void shouldNotLogWarnWhenResourceAttributeIsConfigured() {
    // given
    var config =
        Config.builder().addProperty("otel.resource.attributes", "service.name=test").build();

    var underTest = new ServiceNameChecker(logWarn);

    // when
    underTest.beforeAgent(config, autoConfiguredSdk);

    // then
    verifyNoInteractions(logWarn);
  }
}
