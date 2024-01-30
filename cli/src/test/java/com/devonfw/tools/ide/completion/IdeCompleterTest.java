package com.devonfw.tools.ide.completion;

import java.io.IOException;
import java.nio.file.Paths;

import com.devonfw.tools.ide.cli.AutocompletionReaderTestSupport;
import com.devonfw.tools.ide.completion.IdeCompleter;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Integration test of {@link IdeCompleter}.
 */
public class IdeCompleterTest extends AutocompletionReaderTestSupport {

  @Test
  public void testIdeCompleterHelp() throws IOException {

    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    this.reader.setCompleter(new IdeCompleter(ideContext));
    assertBuffer("helm", new TestBuffer("he").tab().tab());
  }

  @Test
  public void testIdeCompleterVersion() throws IOException {

    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    this.reader.setCompleter(new IdeCompleter(ideContext));
    assertBuffer("--version ", new TestBuffer("--vers").tab());
  }

  @Test
  public void testIdeCompleterInstall() throws IOException {

    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    this.reader.setCompleter(new IdeCompleter(ideContext));
    assertBuffer("install mvn ", new TestBuffer("install m").tab());
  }

  @Test
  public void testIdeCompleterHelpWithToolCompletion() throws IOException {

    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    this.reader.setCompleter(new IdeCompleter(ideContext));
    assertBuffer("help mvn ", new TestBuffer("help m").tab().tab());
  }

  @Test
  public void testIdeCompleterOptionsRemovesUsedOption() throws IOException {

    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    this.reader.setCompleter(new IdeCompleter(ideContext));
    assertBuffer("-t --t", new TestBuffer("-t --t").tab());

  }

  @Test
  public void testIdeCompleterThirdLayerVersions() throws IOException {

    String path = "workspaces/foo-test/my-git-repo";
    IdeTestContext ideContext = newContext("basic", path, false);
    this.reader.setCompleter(new IdeCompleter(ideContext));
    assertBuffer("install mvn 3.2.1", new TestBuffer("install mvn ").tab().tab());
  }

  @Test
  public void testIdeCompleterNonExistentCommand() throws IOException {

    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    this.reader.setCompleter(new IdeCompleter(ideContext));
    assertBuffer("cd ", new TestBuffer("cd ").tab().tab().tab());

  }

  @Test
  public void testIdeCompleterPreventsOptionsAfterCommandWithMinus() throws IOException {

    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    this.reader.setCompleter(new IdeCompleter(ideContext));
    assertBuffer("get-version -", new TestBuffer("get-version -").tab().tab());
    assertBuffer("get-version - ", new TestBuffer("get-version - ").tab().tab());

  }

  @Test
  public void testIdeCompleterWithInvalidInputDoesNothing() throws IOException {

    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    this.reader.setCompleter(new IdeCompleter(ideContext));
    assertBuffer("get-version -t ", new TestBuffer("get-version -t ").tab().tab());
    assertBuffer("- get-version ", new TestBuffer("- get-version ").tab().tab());
    assertBuffer(" - get-version", new TestBuffer(" - get-version").tab().tab());
  }

  @Test
  public void testIdeCompleterHandlesOptionsBeforeCommand() throws IOException {

    IdeTestContext ideContext = new IdeTestContext(Paths.get(""), "");
    this.reader.setCompleter(new IdeCompleter(ideContext));
    assertBuffer("get-version mvn ", new TestBuffer("get-version mv").tab().tab());
  }
}
