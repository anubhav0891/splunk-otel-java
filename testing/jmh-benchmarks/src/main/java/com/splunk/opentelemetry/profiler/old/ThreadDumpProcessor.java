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

package com.splunk.opentelemetry.profiler.old;

import com.splunk.opentelemetry.profiler.ThreadDumpRegion;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadDumpProcessor {
  public static final String EVENT_NAME = "jdk.ThreadDump";
  private static final Logger logger = LoggerFactory.getLogger(ThreadDumpProcessor.class);
  private final Pattern stackSeparator = Pattern.compile("\n\n");
  private final SpanContextualizer contextualizer;
  private final Consumer<StackToSpanLinkage> processor;
  private final Predicate<String> agentInternalsFilter;

  public ThreadDumpProcessor(
      SpanContextualizer contextualizer,
      Consumer<StackToSpanLinkage> processor,
      Predicate<String> agentInternalsFilter) {
    this.contextualizer = contextualizer;
    this.processor = processor;
    this.agentInternalsFilter = agentInternalsFilter;
  }

  public void accept(RecordedEvent event) {
    String eventName = event.getEventType().getName();
    logger.debug("Processing JFR event {}", eventName);
    String wallOfStacks = event.getString("result");
    String[] stacks = stackSeparator.split(wallOfStacks);
    // TODO: Filter out all the VM and GC entries without real stack traces?
    Stream.of(stacks)
        .filter(stack -> stack.charAt(0) == '"') // omit non-stack entries
        .filter(agentInternalsFilter)
        .map(
            stack ->
                new StackToSpanLinkage(
                    event.getStartTime(),
                    stack,
                    event.getEventType().getName(),
                    contextualizer.link(new ThreadDumpRegion(stack, 0, stack.length()))))
        .forEach(processor);
  }
}
