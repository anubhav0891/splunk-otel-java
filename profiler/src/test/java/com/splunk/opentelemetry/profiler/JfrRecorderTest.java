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

package com.splunk.opentelemetry.profiler;

import static com.splunk.opentelemetry.profiler.JfrRecorder.RECORDING_NAME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JfrRecorderTest {

  @TempDir Path outDir;
  Duration maxAge = Duration.ofMinutes(13);
  Map<String, String> settings;
  @Mock Recording recording;
  @Mock Consumer<Path> onNewRecordingFile;

  @BeforeEach
  void setup() {
    settings = new HashMap<>();
    settings.put("foo", "bar");
  }

  @Test
  void testStart() {
    JfrRecorder jfrRecorder = buildJfrRecorder(mock(JFR.class));
    jfrRecorder.start();
    verify(recording).setSettings(settings);
    verify(recording).setToDisk(false);
    verify(recording).setName(RECORDING_NAME);
    verify(recording).setDuration(null);
    verify(recording).setMaxAge(maxAge);
    verify(recording).start();
  }

  @Test
  void testFlushSnapshot() throws Exception {
    JFR jfr = mock(JFR.class);
    Recording snap = mock(Recording.class);
    ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
    doNothing().when(onNewRecordingFile).accept(pathCaptor.capture());
    when(snap.getStream(any(), any())).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(jfr.takeSnapshot()).thenReturn(snap);
    JfrRecorder jfrRecorder = buildJfrRecorder(jfr);

    jfrRecorder.flushSnapshot();
    Path outputPath = pathCaptor.getValue();
    assertTrue(outputPath.startsWith(outDir));
  }

  @Test
  void testDumpThrows() throws Exception {
    JFR jfr = mock(JFR.class);
    JfrRecorder jfrRecorder = buildJfrRecorder(jfr);
    Recording snap = mock(Recording.class);
    when(jfr.takeSnapshot()).thenReturn(snap);
    doThrow(new IOException("KABOOM!!!!!!!!")).when(snap).getStream(any(), any());
    jfrRecorder.flushSnapshot();
    // No exception propagated
  }

  @Test
  void testIsStarted() {
    JFR jfr = mock(JFR.class);
    JfrRecorder jfrRecorder = buildJfrRecorder(jfr);
    assertFalse(jfrRecorder.isStarted());
    jfrRecorder.start();
    when(recording.getState()).thenReturn(RecordingState.RUNNING);
    assertTrue(jfrRecorder.isStarted());
  }

  @Test
  void testIsStop() {
    JFR jfr = mock(JFR.class);
    JfrRecorder jfrRecorder = buildJfrRecorder(jfr);
    assertFalse(jfrRecorder.isStarted());
    jfrRecorder.start();
    verify(recording, never()).stop();
    jfrRecorder.stop();
    verify(recording).stop();
    assertFalse(jfrRecorder.isStarted());
  }

  private JfrRecorder buildJfrRecorder(JFR jfr) {
    JfrRecorder.Builder builder =
        JfrRecorder.builder()
            .maxAgeDuration(maxAge)
            .settings(settings)
            .namingConvention(new RecordingFileNamingConvention(outDir))
            .onNewRecordingFile(onNewRecordingFile)
            .jfr(jfr);

    return new JfrRecorder(builder) {
      @Override
      Recording newRecording() {
        return recording;
      }
    };
  }
}
