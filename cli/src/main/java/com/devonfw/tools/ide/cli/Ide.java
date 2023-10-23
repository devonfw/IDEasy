package com.devonfw.tools.ide.cli;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Supplier;

import org.fusesource.jansi.AnsiConsole;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.AutosuggestionWidgets;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.commandlet.HelpCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.property.BooleanProperty;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;

/**
 * The main program of the CLI (command-line-interface).
 */
public final class Ide {

  private static final String INVALID_ARGUMENT = "Invalid CLI argument '{}' for property '{}' of commandlet '{}'";

  private static final String INVALID_ARGUMENT_WITH_CAUSE = INVALID_ARGUMENT + ":{}";

  private AbstractIdeContext context;

  /**
   * The actual main method of the CLI program.
   *
   * @param args the command-line arguments.
   */
  public static void main(String... args) {

    int exitStatus = new Ide().run(args);
    System.exit(exitStatus);
  }

  private IdeContext context() {

    if (this.context == null) {
      // fallback in case of exception before initialization
      this.context = new IdeContextConsole(IdeLogLevel.INFO, null, false);
    }
    return this.context;
  }

  /**
   * Non-static variant of {@link #main(String...) main method} without invoking {@link System#exit(int)} so it can be
   * tested.
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
      if (context().level(IdeLogLevel.DEBUG).isEnabled()) {
        context().error(error.getMessage(), error);
      } else {
        context().error(error.getMessage());
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
          + "https://github.com/devonfw/ide/issues/new?assignees=&labels=bug&projects=&template=bug.md&title="
          + URLEncoder.encode(title, StandardCharsets.UTF_8);
      context().error(error, message);
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

    if (args.length == 0) {
      initializeJlineCompletion();
      return 1;
    }

    CliArgument first = CliArgument.of(args);
    CliArgument current = initContext(first);
    if (current == null) {
      // exit with success after --version has been printed...
      return ProcessResult.SUCCESS;
    }
    String keyword = current.get();
    Commandlet firstCandidate = this.context.getCommandletManager().getCommandletByFirstKeyword(keyword);
    boolean matches;
    if (firstCandidate != null) {
      matches = applyAndRun(current, firstCandidate);
      if (matches) {
        return ProcessResult.SUCCESS;
      }
    }
    for (Commandlet commandlet : this.context.getCommandletManager().getCommandlets()) {
      if (commandlet != firstCandidate) {
        matches = applyAndRun(current, commandlet);
        if (matches) {
          return ProcessResult.SUCCESS;
        }
      }
    }
    if (!current.isEnd()) {
      context().error("Invalid arguments: {}", current.getArgs());
    }
    context().getCommandletManager().getCommandlet(HelpCommandlet.class).run();
    return 1;
  }

  /**
   * Initializes jline3 autocompletion.
   */
  private void initializeJlineCompletion() {

    AnsiConsole.systemInstall();

    try {
      ContextCommandlet init = new ContextCommandlet();
      init.run();
      this.context = init.getIdeContext();

      Supplier<Path> workDir = context::getCwd;
      // set up JLine built-in commands
      // TODO: fix BuiltIns or remove
      // Builtins builtins = new Builtins(workDir, null, null);
      // builtins.rename(Builtins.Command.TTOP, "top");
      // builtins.alias("zle", "widget");
      // builtins.alias("bindkey", "keymap");

      CommandletRegistry commandletRegistry = new CommandletRegistry(init, this, context);

      Parser parser = new DefaultParser();
      try (Terminal terminal = TerminalBuilder.builder().build()) {

        SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
        systemRegistry.setCommandRegistries(commandletRegistry);

        // systemRegistry.setCommandRegistries(builtins, commandletRegistry);
        // systemRegistry.register("help", commandletRegistry);

        LineReader reader = LineReaderBuilder.builder().terminal(terminal).completer(systemRegistry.completer())
            .parser(parser).variable(LineReader.LIST_MAX, 50).build();

        // Create autosuggestion widgets
        AutosuggestionWidgets autosuggestionWidgets = new AutosuggestionWidgets(reader);
        // Enable autosuggestions
        autosuggestionWidgets.enable();

        // TODO: implement TailTipWidgets
        // TailTipWidgets widgets = new TailTipWidgets(reader, systemRegistry::commandDescription, 5,
        // TailTipWidgets.TipType.COMPLETER);
        // widgets.enable();
        // TODO: add own KeyMap
        // KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
        // keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));

        String prompt = "prompt> ";
        String rightPrompt = null;
        String line;

        while (true) {
          try {
            systemRegistry.cleanUp();
            line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
            systemRegistry.execute(line);
          } catch (UserInterruptException e) {
            // Ignore
            context.warning("User canceled with CTRL+C", e);
          } catch (EndOfFileException e) {
            context.warning("User canceled with CTRL+D", e);
            return;
          } catch (Exception e) {
            systemRegistry.trace(e);
          }
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } catch (Throwable e) {
      throw new RuntimeException(e);
    } finally {
      AnsiConsole.systemUninstall();
    }
  }

  private CliArgument initContext(CliArgument first) {

    ContextCommandlet init = new ContextCommandlet();
    CliArgument current = first;
    while (!current.isEnd()) {
      String key = current.getKey();
      Property<?> property = init.getOption(key);
      if (property == null) {
        break;
      }
      String value = current.getValue();
      if (value == null) {
        if (property instanceof FlagProperty) {
          ((FlagProperty) property).setValue(Boolean.TRUE);
        } else {
          this.context.error("Missing value for option " + key);
        }
      } else {
        property.setValueAsString(value);
      }
      current = current.getNext(true);
    }
    init.run();
    this.context = init.getIdeContext();
    if (init.version.isTrue()) {
      return null;
    }
    return current;
  }

  /**
   * @param argument the current {@link CliArgument} (position) to match.
   * @param commandlet the potential {@link Commandlet} to {@link #apply(CliArgument, Commandlet) apply} and
   *        {@link Commandlet#run() run}.
   * @return {@code true} if the given {@link Commandlet} matched and did {@link Commandlet#run() run} successfully,
   *         {@code false} otherwise (the {@link Commandlet} did not match and we have to try a different candidate).
   */
  protected boolean applyAndRun(CliArgument current, Commandlet commandlet) {

    boolean matches = apply(current, commandlet);
    if (matches) {
      this.context.debug("Running commandlet {}", commandlet);
      if (commandlet.isIdeHomeRequired() && (this.context.getIdeHome() == null)) {
        throw new CliException(this.context.getMessageIdeHome());
      }
      commandlet.run();
    } else {
      this.context.trace("Commandlet did not match");
    }
    return matches;
  }

  /**
   * @param argument the current {@link CliArgument} (position) to match.
   * @param commandlet the potential {@link Commandlet} to match.
   * @return {@code true} if the given {@link Commandlet} matches to the given {@link CliArgument}(s) and those have
   *         been applied (set in the {@link Commandlet} and {@link Commandlet#validate() validated}), {@code false}
   *         otherwise (the {@link Commandlet} did not match and we have to try a different candidate).
   */
  private boolean apply(CliArgument argument, Commandlet commandlet) {

    this.context.trace("Trying to match arguments to commandlet {}", commandlet.getName());
    CliArgument currentArgument = argument;
    Iterator<Property<?>> valueIterator = commandlet.getValues().iterator();
    Property<?> currentProperty = null;
    boolean endOpts = false;
    while (!currentArgument.isEnd()) {
      if (currentArgument.isEndOptions()) {
        endOpts = true;
      } else {
        String arg = currentArgument.get();
        this.context.trace("Trying to match argument '{}'", currentArgument);
        if ((currentProperty != null) && (currentProperty.isExpectValue())) {
          currentProperty.setValueAsString(arg);
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
              this.context.trace("No option or next value found");
              return false;
            }
            currentProperty = valueIterator.next();
            this.context.trace("Next value candidate is {}", currentProperty);
            if (currentProperty instanceof KeywordProperty) {
              KeywordProperty keyword = (KeywordProperty) currentProperty;
              if (keyword.matches(arg)) {
                keyword.setValue(Boolean.TRUE);
                this.context.trace("Keyword matched");
              } else {
                this.context.trace("Missing keyword");
                return false;
              }
            } else {
              boolean success = false;
              try {
                currentProperty.setValueAsString(arg);
                success = true;
              } catch (RuntimeException e) {
                String message = INVALID_ARGUMENT_WITH_CAUSE;
                if (e instanceof IllegalArgumentException) {
                  message = INVALID_ARGUMENT;
                }
                this.context.warning(message, arg, currentProperty.getNameOrAlias(), commandlet.getName(),
                    e.getMessage());
              }
              if (!success && currentProperty.isRequired()) {
                return false;
              }
            }
            if ((currentProperty != null) && !currentProperty.isMultiValued()) {
              currentProperty = null;
            }
          } else {
            this.context.trace("Found option by name");
            String value = currentArgument.getValue();
            if (value != null) {
              property.setValueAsString(value);
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
    return commandlet.validate();
  }

}
