package com.devonfw.tools.ide.cli;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.JulLogLevel;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.Property;

/**
 * The main program of the CLI (command-line-interface).
 */
public final class Ideasy {

  private static final Logger LOG = LoggerFactory.getLogger(Ideasy.class);

  private AbstractIdeContext context;

  /**
   * The default constructor.
   */
  public Ideasy() {
    super();
  }

  /**
   * The constructor.
   *
   * @param context the predefined {@link IdeContext} for testing.
   */
  Ideasy(AbstractIdeContext context) {

    super();
    this.context = context;
  }

  /**
   * Non-static variant of {@link #main(String...) main method} without invoking {@link System#exit(int)} so it can be tested.
   *
   * @param args the command-line arguments.
   * @return the exit code.
   */
  public int run(String... args) {

    int exitStatus;
    try {
      exitStatus = runOrThrow(args);
    } catch (CliException error) {
      exitStatus = error.getExitCode();
      String errorMessage = error.getMessage();
      if ((errorMessage != null) && !errorMessage.isBlank()) {
        LOG.error(errorMessage);
      }
    } catch (Throwable error) {
      exitStatus = 255;
      String title = error.getMessage();
      if (title == null) {
        title = error.getClass().getName();
      } else {
        title = error.getClass().getSimpleName() + ": " + title;
      }
      String message = "An unexpected error occurred!\n" //
          + "We are sorry for the inconvenience.\n" //
          + "Please check the error below, resolve it and try again.\n" //
          + "If the error is not on your end (network connectivity, lack of permissions, etc.) please file a bug:\n" //
          + "https://github.com/devonfw/IDEasy/issues/new?template=bug_report.yml&title="
          + URLEncoder.encode(title, StandardCharsets.UTF_8);
      LOG.error(message, error);
    }
    return exitStatus;
  }

  /**
   * Like {@link #run(String...)} but does not catch {@link Throwable}s so you can handle them yourself.
   *
   * @param args the command-line arguments.
   * @return the exit code.
   */
  public int runOrThrow(String... args) {

    CliArguments arguments = new CliArguments(args);
    initContext(arguments);
    return this.context.run(arguments);
  }

  private void initContext(CliArguments arguments) {

    IdeStartContextImpl startContext = null;
    if (this.context != null) {
      startContext = this.context.getStartContext();
    }
    ContextCommandlet contextCommandlet = new ContextCommandlet(startContext);
    while (arguments.hasNext()) {
      CliArgument current = arguments.next();
      String key = current.getKey();
      Property<?> property = contextCommandlet.getOption(key);
      if (property == null) {
        break;
      }
      String value = current.getValue();
      if (value == null) {
        if (property instanceof FlagProperty) {
          ((FlagProperty) property).setValue(Boolean.TRUE);
        } else {
          System.err.println("Missing value for option " + key);
        }
      } else {
        property.setValueAsString(value, this.context);
      }
    }
    contextCommandlet.run();
    if (this.context == null) {
      startContext = contextCommandlet.getStartContext();
      this.context = new IdeContextConsole(startContext);
    }
  }

  /**
   * The actual main method of the CLI program.
   *
   * @param args the command-line arguments.
   */
  public static void main(String... args) {

    JulLogLevel.init();
    int exitStatus = new Ideasy().run(args);
    System.exit(exitStatus);
  }

}
