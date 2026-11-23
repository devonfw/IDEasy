package com.devonfw.ide.gui.core.context;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.core.event.GuiEventBus;
import com.devonfw.ide.gui.core.event.console.LogEvent;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListenerNone;

import io.github.mmm.event.EventListener;

/**
 * Tests for {@link IdeGuiLogListener}.
 */
public class IdeGuiLogListenerTest extends Assertions {

  /**
   * An {@link EventListener} for {@link LogEvent} that records every event published on the {@link GuiEventBus} instead of touching the JavaFX toolkit, so the
   * number of times {@link IdeGuiLogListener#onLog(IdeLogEntry)} sends a message can be counted.
   */
  private static final class RecordingLogListener implements EventListener<LogEvent> {

    private final List<IdeLogEntry> entries = new ArrayList<>();

    @Override
    public void onEvent(LogEvent event) {

      this.entries.add(event.logEntry());
    }

    List<IdeLogEntry> getEntries() {
      return this.entries;
    }
  }

  /**
   * {@link IdeStartContextImpl} holds its state in a process-wide static field that is (re)assigned by every construction and offers no reset hook. This test
   * installs a buffering {@link IdeGuiLogListener} into that static; without cleanup it would leak into every other gui test running in the same surefire JVM,
   * routing their SLF4J log events into the leftover buffering listener. Restore a neutral, non-buffering no-op context afterwards.
   */
  @AfterEach
  void resetStartContext() {

    new IdeStartContextImpl(IdeLogLevel.INFO, IdeLogListenerNone.INSTANCE);
  }

  @Test
  void testMessageOnlySendOnce() {

    GuiEventBus eventBus = new GuiEventBus();
    RecordingLogListener recorder = new RecordingLogListener();
    eventBus.addListener(LogEvent.class, recorder);
    IdeGuiLogListener listener = new IdeGuiLogListener(eventBus);
    listener.flushAndEndBuffering();

    String message = "TestMessage";
    IdeLogEntry entry = new IdeLogEntry(IdeLogLevel.INFO, message);
    boolean accepted = listener.onLog(entry);

    assertThat(accepted).as("onLog() should accept the entry").isTrue();
    assertThat(recorder.getEntries()).as("the entry must be forwarded to the console exactly once").hasSize(1);
    assertThat(recorder.getEntries().get(0).message()).isEqualTo(message);
  }


  @Test
  void testBufferedMessageShownOnceAfterFlush() {

    Logger log = LoggerFactory.getLogger(IdeGuiLogListenerTest.class);
    GuiEventBus eventBus = new GuiEventBus();
    RecordingLogListener recorder = new RecordingLogListener();
    eventBus.addListener(LogEvent.class, recorder);
    IdeGuiLogListener listener = new IdeGuiLogListener(eventBus);
    new IdeStartContextImpl(IdeLogLevel.INFO, listener);

    log.warn("TestMessage");
    assertThat(recorder.getEntries()).as("nothing is shown while buffering").isEmpty();

    listener.flushAndEndBuffering();
    assertThat(recorder.getEntries()).extracting(IdeLogEntry::message).containsExactly("TestMessage");
  }

  @Test
  void testNullMessage() {

    Logger log = LoggerFactory.getLogger(IdeGuiLogListenerTest.class);
    assertThatCode(() -> log.warn((String) null)).doesNotThrowAnyException();

  }

  @Test
  void testEntryWithErrorAndNullMessageIsForwardedUnchanged() {

    GuiEventBus eventBus = new GuiEventBus();
    RecordingLogListener recorder = new RecordingLogListener();
    eventBus.addListener(LogEvent.class, recorder);
    IdeGuiLogListener listener = new IdeGuiLogListener(eventBus);
    listener.flushAndEndBuffering();

    RuntimeException error = new NullPointerException();
    IdeLogEntry entry = new IdeLogEntry(IdeLogLevel.ERROR, null, null, null, error);
    listener.onLog(entry);

    assertThat(recorder.getEntries()).first().isSameAs(entry);
    assertThat(recorder.getEntries().get(0).error()).isSameAs(error);
  }

}
