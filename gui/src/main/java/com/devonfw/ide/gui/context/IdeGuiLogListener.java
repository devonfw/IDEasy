package com.devonfw.ide.gui.context;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;

import com.devonfw.ide.gui.console.ConsoleController;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListener;

public class IdeGuiLogListener implements IdeLogListener {

  private final ConsoleController consoleController;

  public IdeGuiLogListener(ConsoleController consoleController) {

    this.consoleController = consoleController;
  }

  @Override
  public boolean onLog(IdeLogLevel level, String message, String rawMessage, Object[] args, Throwable error) {
    if (this.consoleController != null && message != null) {
      String prefix = getPrefix(level);
      String timeStamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
      String formattedMessage = String.format("%s | %s %s", timeStamp, prefix, message);

      Platform.runLater(() -> {
        this.consoleController.appendOutput(formattedMessage);
        if (error != null) {
          this.consoleController.appendOutput("  Error: " + error.getMessage());
        }
      });
    }
    return true; // continue processing (also log to standard output if needed)
  }

  private String getPrefix(IdeLogLevel level) {
    return switch (level) {
      case ERROR -> "[ERROR] ";
      case WARNING -> "[WARN]  ";
      case INFO -> "[INFO]  ";
      case DEBUG -> "[DEBUG] ";
      case TRACE -> "[TRACE] ";
      default -> "";
    };
  }

}
