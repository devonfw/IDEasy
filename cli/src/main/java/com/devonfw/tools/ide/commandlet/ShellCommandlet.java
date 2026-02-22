package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.fusesource.jansi.AnsiConsole;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.AutosuggestionWidgets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.completion.IdeCompleter;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.BooleanProperty;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;

/**
 * {@link Commandlet} for internal interactive shell with build-in auto-completion and help.
 */
public final class ShellCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(ShellCommandlet.class);

  private static final int AUTOCOMPLETER_MAX_RESULTS = 50;

  private static final int RC_EXIT = 987654321;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public ShellCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {

    return "shell";
  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }

  @Override
  protected void doRun() {

    try {
      Parser parser = new DefaultParser();
      try (Terminal terminal = TerminalBuilder.builder().build()) {
        // initialize our own completer here and add exit as an autocompletion option
        Completer completer = new AggregateCompleter(
            new StringsCompleter("exit"), new IdeCompleter((AbstractIdeContext) this.context));

        LineReader reader = LineReaderBuilder.builder().terminal(terminal).completer(completer).parser(parser)
            .variable(LineReader.LIST_MAX, AUTOCOMPLETER_MAX_RESULTS).build();

        // Create autosuggestion widgets
        AutosuggestionWidgets autosuggestionWidgets = new AutosuggestionWidgets(reader);
        // Enable autosuggestions
        autosuggestionWidgets.enable();

        // TODO: implement TailTipWidgets, see: https://github.com/devonfw/IDEasy/issues/169

        String rightPrompt = null;
        String line;

        AnsiConsole.systemInstall();
        while (true) {
          try {
            String prompt = context.getCwd() + "$ ide ";
            line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
            line = line.trim();
            if (line.equals("exit")) {
              return;
            }
            reader.getHistory().add(line);
            int rc = runCommand(line);
            if (rc == RC_EXIT) {
              return;
            }
          } catch (UserInterruptException e) {
            // Ignore CTRL+C
            return;
          } catch (EndOfFileException e) {
            // CTRL+D
            return;
          } finally {
            AnsiConsole.systemUninstall();
          }
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } catch (CliException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Unexpected error during interactive auto-completion", e);
    }
  }

  /**
   * Converts String of arguments to array and runs the command
   *
   * @param args String of arguments
   * @return status code
   */
  private int runCommand(String args) {

    if ("exit".equals(args) || "quit".equals(args)) {
      return RC_EXIT;
    }
    String[] arguments = args.split(" ", 0);
    CliArguments cliArgs = new CliArguments(arguments);
    cliArgs.next();

    if ("cd".equals(arguments[0])) {
      return changeDirectory(cliArgs);
    }

    return ((AbstractIdeContext) this.context).run(cliArgs);
  }

  private int changeDirectory(CliArguments cliArgs) {
    if (!cliArgs.hasNext()) {
      Path homeDir = this.context.getUserHome();
      context.setCwd(homeDir, context.getWorkspaceName(), context.getIdeHome());
      return 0;
    }

    String targetDir = String.valueOf(cliArgs.next());
    Path path = Paths.get(targetDir);

    // If the given path is relative, resolve it relative to the current directory
    if (!path.isAbsolute()) {
      path = context.getCwd().resolve(targetDir).normalize();
    }

    if (context.getFileAccess().isExpectedFolder(path)) {
      context.setCwd(path, context.getWorkspaceName(), context.getIdeHome());
      return 0;
    } else {
      return 1;
    }
  }

  /**
   * @param argument the current {@link CliArgument} (position) to match.
   * @param commandlet the potential {@link Commandlet} to match.
   * @return {@code true} if the given {@link Commandlet} matches to the given {@link CliArgument}(s) and those have been applied (set in the {@link Commandlet}
   *     and {@link Commandlet#validate() validated}), {@code false} otherwise (the {@link Commandlet} did not match and we have to try a different candidate).
   */
  private boolean apply(CliArgument argument, Commandlet commandlet) {

    LOG.trace("Trying to match arguments to commandlet {}", commandlet.getName());
    CliArgument currentArgument = argument;
    Iterator<Property<?>> valueIterator = commandlet.getValues().iterator();
    Property<?> currentProperty = null;
    boolean endOpts = false;
    while (!currentArgument.isEnd()) {
      if (currentArgument.isEndOptions()) {
        endOpts = true;
      } else {
        String arg = currentArgument.get();
        LOG.trace("Trying to match argument '{}'", currentArgument);
        if ((currentProperty != null) && (currentProperty.isExpectValue())) {
          currentProperty.setValueAsString(arg, this.context);
          if (!currentProperty.isMultiValued()) {
            currentProperty = null;
          }
        } else {
          Property<?> property = null;
          if (!endOpts) {
            property = commandlet.getOption(currentArgument.getKey());
          }
          if (property == null) {
            if (!valueIterator.hasNext()) {
              LOG.trace("No option or next value found");
              return false;
            }
            currentProperty = valueIterator.next();
            LOG.trace("Next value candidate is {}", currentProperty);
            if (currentProperty instanceof KeywordProperty keyword) {
              if (keyword.matches(arg)) {
                keyword.setValue(Boolean.TRUE);
                LOG.trace("Keyword matched");
              } else {
                LOG.trace("Missing keyword");
                return false;
              }
            } else {
              boolean success = currentProperty.assignValueAsString(arg, this.context, commandlet);
              if (!success && currentProperty.isRequired()) {
                return false;
              }
            }
            if ((currentProperty != null) && !currentProperty.isMultiValued()) {
              currentProperty = null;
            }
          } else {
            LOG.trace("Found option by name");
            String value = currentArgument.getValue();
            if (value != null) {
              property.setValueAsString(value, this.context);
            } else if (property instanceof BooleanProperty) {
              ((BooleanProperty) property).setValue(Boolean.TRUE);
            } else {
              currentProperty = property;
              if (property.isEndOptions()) {
                endOpts = true;
              }
              throw new UnsupportedOperationException("not implemented");
            }
          }
        }
      }
      currentArgument = currentArgument.getNext(!endOpts);
    }
    return commandlet.validate().isValid();
  }
}
