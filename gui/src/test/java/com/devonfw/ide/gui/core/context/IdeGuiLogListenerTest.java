package com.devonfw.ide.gui.core.context;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.core.mainwindow.console.ConsoleController;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Tests for {@link IdeGuiLogListener}.
 */
public class IdeGuiLogListenerTest extends Assertions {

  @TempDir
  private Path mockIdeRoot;

  /**
   * A {@link ConsoleController} that records every entry forwarded to it instead of touching the JavaFX toolkit, so the number of times
   * {@link IdeGuiLogListener#onLog(IdeLogEntry)} sends a message can be counted.
   */
  private static class RecordingConsoleController extends ConsoleController {

    private final List<IdeLogEntry> appended = new ArrayList<>();

    RecordingConsoleController(GuiStateManager guiStateManager) {
      super(guiStateManager);
    }

    @Override
    public void appendOutput(IdeLogEntry entry) {

      this.appended.add(entry);
    }

    List<IdeLogEntry> getEntries() {
      return this.appended;
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

    GuiStateManager guiStateManager = new GuiStateManager(this.mockIdeRoot.toString());
    RecordingConsoleController console = new RecordingConsoleController(guiStateManager);
    IdeGuiLogListener listener = new IdeGuiLogListener(console);
    listener.flushAndEndBuffering();

    String message = "TestMessage";
    IdeLogEntry entry = new IdeLogEntry(IdeLogLevel.INFO, message);
    boolean accepted = listener.onLog(entry);

    assertThat(accepted).as("onLog() should accept the entry").isTrue();
    assertThat(console.getEntries()).as("the entry must be forwarded to the console exactly once").hasSize(1);
    assertThat(console.getEntries().get(0).message()).isEqualTo(message);
  }


  @Test
  void testBufferedMessageShownOnceAfterFlush() {

    Logger log = LoggerFactory.getLogger(IdeGuiLogListenerTest.class);
    GuiStateManager guiStateManager = new GuiStateManager(this.mockIdeRoot.toString());
    RecordingConsoleController console = new RecordingConsoleController(guiStateManager);
    IdeGuiLogListener listener = new IdeGuiLogListener(console);
    new IdeStartContextImpl(IdeLogLevel.INFO, listener);

    log.warn("TestMessage");
    assertThat(console.getEntries()).as("nothing is shown while buffering").isEmpty();

    listener.flushAndEndBuffering();
    assertThat(console.getEntries()).extracting(IdeLogEntry::message).containsExactly("TestMessage");
  }
}
