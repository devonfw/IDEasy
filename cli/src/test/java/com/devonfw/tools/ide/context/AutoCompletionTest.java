package com.devonfw.tools.ide.context;

import static com.devonfw.tools.ide.context.AbstractIdeContextTest.newContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.jline.builtins.Completers;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.IdeCompleter;
import com.devonfw.tools.ide.commandlet.ContextCommandlet;

public class AutoCompletionTest extends ReaderTestSupport{

  @Test
  public void testOptions() throws Exception {
    List<Completer> argsCompleters = new ArrayList<>();
    List<Completers.OptDesc> options = new ArrayList<>();
    argsCompleters.add(new StringsCompleter("bar", "rab"));
    argsCompleters.add(new StringsCompleter("foo", "oof"));
    argsCompleters.add(NullCompleter.INSTANCE);
    options.add(new Completers.OptDesc("-s", "--sopt", new StringsCompleter("val", "lav")));
    options.add(new Completers.OptDesc(null, "--option", NullCompleter.INSTANCE));

    reader.setCompleter(new ArgumentCompleter(
    new StringsCompleter("command"), new Completers.OptionCompleter(argsCompleters, options, 1)));

    assertBuffer("command ", new TestBuffer("c").tab());
    assertBuffer("command -s", new TestBuffer("command -").tab());
    assertBuffer("command -s val ", new TestBuffer("command -s v").tab());
    assertBuffer("command -sval ", new TestBuffer("command -sv").tab());
    assertBuffer("command --sopt val ", new TestBuffer("command --sopt v").tab());
    assertBuffer("command --sopt=", new TestBuffer("command --sop").tab());
    assertBuffer("command --sopt=val ", new TestBuffer("command --sopt=v").tab());
    assertBuffer("command -sval ", new TestBuffer("command -sv").tab());
    assertBuffer("command -s val bar ", new TestBuffer("command -s val b").tab());
    assertBuffer("command -s val bar --option ", new TestBuffer("command -s val bar --o").tab());
    assertBuffer("command -s val bar --option foo ", new TestBuffer("command -s val bar --option f").tab());
  }

  @Test
  public void testIdeCompleter() throws IOException {
    ContextCommandlet contextCommandlet = new ContextCommandlet();
    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    reader.setCompleter(new IdeCompleter(contextCommandlet, ideContext));
    assertBuffer("help ", new TestBuffer("he").tab());
  }

  @Test
  public void testIdeCompleterInstall() throws IOException {
    ContextCommandlet contextCommandlet = new ContextCommandlet();
    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    reader.setCompleter(new IdeCompleter(contextCommandlet, ideContext));
    assertBuffer("install mvn ", new TestBuffer("install m").tab());
  }

  @Test
  public void testIdeCompleterHelpWithToolCompletion() throws IOException {
    ContextCommandlet contextCommandlet = new ContextCommandlet();
    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    reader.setCompleter(new IdeCompleter(contextCommandlet, ideContext));
    assertBuffer("help mvn ", new TestBuffer("help m").tab().tab());
  }

  @Test
  public void testIdeCompleterOptions() throws IOException {
    ContextCommandlet contextCommandlet = new ContextCommandlet();
    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    reader.setCompleter(new IdeCompleter(contextCommandlet, ideContext));
    assertBuffer("--trace ", new TestBuffer("--t").tab());
    assertBuffer("--debug ", new TestBuffer("--d").tab());
  }

  @Test
  public void testIdeCompleterOptionsRemovesUsedOption() throws IOException {
    ContextCommandlet contextCommandlet = new ContextCommandlet();
    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    reader.setCompleter(new IdeCompleter(contextCommandlet, ideContext));
    assertBuffer(" ", new TestBuffer("-t -t").tab());

  }

  @Test
  public void testIdeCompleterThirdLayerVersions() throws IOException {
    ContextCommandlet contextCommandlet = new ContextCommandlet();
    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    reader.setCompleter(new IdeCompleter(contextCommandlet, ideContext));
    assertBuffer("install mvn 3.9.5", new TestBuffer("install mvn").tab().tab().tab());

  }

  @Test
  public void testIdeCompleterNonExistentCommand() throws IOException {
    ContextCommandlet contextCommandlet = new ContextCommandlet();
    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    reader.setCompleter(new IdeCompleter(contextCommandlet, ideContext));
    assertBuffer("cd ", new TestBuffer("cd ").tab().tab().tab());

  }
}
