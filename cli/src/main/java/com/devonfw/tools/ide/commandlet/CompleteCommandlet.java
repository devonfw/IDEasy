package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.fusesource.jansi.AnsiConsole;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Parser;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.AutosuggestionWidgets;
import org.jline.widget.TailTipWidgets;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link Commandlet} used to provide bash like autocomplete functionality.
 */
public class CompleteCommandlet extends Commandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CompleteCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }

  @Override
  public String getName() {

    return "complete";
  }

  @Override
  public void run() {

    AnsiConsole.systemInstall();

    try {
      Supplier<Path> workDir = context::getCwd;
      // set up JLine built-in commands
//      Builtins builtins = new Builtins(workDir, null, null);
//      builtins.rename(Builtins.Command.TTOP, "top");
//      builtins.alias("zle", "widget");
//      builtins.alias("bindkey", "keymap");

      CommandletRegistry commandletRegistry = new CommandletRegistry(context);

      Parser parser = new DefaultParser();
      try (Terminal terminal = TerminalBuilder.builder().build()) {

        SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
        systemRegistry.setCommandRegistries(commandletRegistry);
//        systemRegistry.setCommandRegistries(builtins, commandletRegistry);
//        systemRegistry.register("help", commandletRegistry);

        LineReader reader = LineReaderBuilder.builder()
        .terminal(terminal)
        .completer(systemRegistry.completer())
        .parser(parser)
        .variable(LineReader.LIST_MAX, 50)
        .build();

        // Create autosuggestion widgets
        AutosuggestionWidgets autosuggestionWidgets = new AutosuggestionWidgets(reader);
        // Enable autosuggestions
        autosuggestionWidgets.enable();

        // TODO: fix these on
//        TailTipWidgets widgets = new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMPLETER);
//        widgets.enable();
        KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
        keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));

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
}
