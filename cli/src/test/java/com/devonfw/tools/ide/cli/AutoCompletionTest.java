package com.devonfw.tools.ide.cli;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.context.IdeTestContext;

public class AutoCompletionTest extends ReaderTestSupport {

  @Test
  public void testIdeCompleterHelp() throws IOException {

    ContextCommandlet contextCommandlet = new ContextCommandlet();
    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    reader.setCompleter(new IdeCompleter(contextCommandlet, ideContext));
    assertBuffer("help", new TestBuffer("he").tab().tab().tab());
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
    assertBuffer("-t --t", new TestBuffer("-t --t").tab());

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

  @Test
  public void testIdeCompleterPreventsOptionsAfterCommand() throws IOException {

    ContextCommandlet contextCommandlet = new ContextCommandlet();
    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    reader.setCompleter(new IdeCompleter(contextCommandlet, ideContext));
    assertBuffer("-t -f get-version - ", new TestBuffer("-t -f get-version - ").tab());

  }

  @Test
  public void testIdeCompleterHandlesOptionsBeforeCommand() throws IOException {

    ContextCommandlet contextCommandlet = new ContextCommandlet();
    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    reader.setCompleter(new IdeCompleter(contextCommandlet, ideContext));
    assertBuffer("-t -f get-version mvn ", new TestBuffer("-t -f get-version mv").tab().tab());

  }
}
