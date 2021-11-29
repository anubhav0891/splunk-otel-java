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

package com.splunk.opentelemetry.instrumentation.micrometer.v1_5;

import static com.splunk.opentelemetry.instrumentation.micrometer.Bridging.toAgentTags;
import static com.splunk.opentelemetry.instrumentation.micrometer.Bridging.toDuration;
import static com.splunk.opentelemetry.instrumentation.micrometer.Bridging.toDurations;

import application.io.micrometer.core.instrument.LongTaskTimer;
import application.io.micrometer.core.instrument.Meter;
import application.io.micrometer.core.instrument.MeterRegistry;
import application.io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import com.splunk.opentelemetry.instrumentation.micrometer.AbstractApplicationMeterRegistry;
import io.micrometer.core.instrument.Metrics;

public final class ApplicationMeterRegistry extends AbstractApplicationMeterRegistry {

  public static MeterRegistry create() {
    io.micrometer.core.instrument.Clock agentClock = Metrics.globalRegistry.config().clock();
    return new ApplicationMeterRegistry(agentClock, Metrics.globalRegistry);
  }

  public ApplicationMeterRegistry(
      io.micrometer.core.instrument.Clock agentClock,
      io.micrometer.core.instrument.MeterRegistry agentRegistry) {
    super(agentClock, agentRegistry);
  }

  @Override
  protected LongTaskTimer newLongTaskTimer(
      Meter.Id id, DistributionStatisticConfig distributionStats) {
    io.micrometer.core.instrument.LongTaskTimer agentLongTaskTimer =
        io.micrometer.core.instrument.LongTaskTimer.builder(id.getName())
            .tags(toAgentTags(id.getTagsAsIterable()))
            .description(id.getDescription())
            .serviceLevelObjectives(
                toDurations(distributionStats.getServiceLevelObjectiveBoundaries()))
            .minimumExpectedValue(toDuration(distributionStats.getMinimumExpectedValueAsDouble()))
            .maximumExpectedValue(toDuration(distributionStats.getMaximumExpectedValueAsDouble()))
            .distributionStatisticExpiry(distributionStats.getExpiry())
            .distributionStatisticBufferLength(distributionStats.getBufferLength())
            .publishPercentiles(distributionStats.getPercentiles())
            .percentilePrecision(distributionStats.getPercentilePrecision())
            .publishPercentileHistogram(distributionStats.isPublishingHistogram())
            .register(agentRegistry);
    return new ApplicationLongTaskTimer(id, agentLongTaskTimer);
  }
}
