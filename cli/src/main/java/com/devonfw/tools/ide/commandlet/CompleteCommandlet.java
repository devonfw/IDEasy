package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.fusesource.jansi.AnsiConsole;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

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

    try (Terminal terminal = TerminalBuilder.terminal()) {
      Collection<Commandlet> commandletCollection = context.getCommandletManager().getCommandlets();
      Iterator<Commandlet> iterator = commandletCollection.iterator();

      List<String> commandList = new ArrayList<>();

      while (iterator.hasNext()) {
        commandList.add(iterator.next().getName());
      }

      Completer commandletsCompleter = new StringsCompleter(commandList);
      Parser parser = new DefaultParser();
      LineReader reader = LineReaderBuilder.builder().terminal(terminal).completer(commandletsCompleter).parser(parser)
          .build();

      String prompt = "prompt> ";
      String rightPrompt = null;
      String line;

      while (true) {
        line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);

        if (line == null || line.equalsIgnoreCase("exit")) {
          break;
        }

        reader.getHistory().add(line);

        if (line.equalsIgnoreCase("help")) {
          context.info("help");
          context.getCommandletManager().getCommandlet("help").run();
        } else if (line.equalsIgnoreCase("install")) {
          context.info("install");
        } else {
          context.warning("Unknown command: {}", line);
        }
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      AnsiConsole.systemUninstall();
    }

  }
}
