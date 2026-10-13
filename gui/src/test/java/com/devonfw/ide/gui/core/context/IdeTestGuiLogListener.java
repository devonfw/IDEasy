package com.devonfw.ide.gui.core.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.core.mainwindow.console.ConsoleController;
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
}
