package com.devonfw.tools.ide.completion;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.AutocompletionReaderTestSupport;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Integration test of {@link IdeCompleter}.
 */
public class IdeCompleterTest extends AutocompletionReaderTestSupport {

  /**
   * Test of 1st level auto-completion (commandlet name). As suggestions are sorted alphabetically "helm" will be the
   * first match.
   */
  @Test
  public void testIdeCompleterHelp() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("helm", new TestBuffer("he").tab().tab());
  }

  /**
   * Test of 1st level auto-completion (commandlet name). Here we test the special case of the
   * {@link com.devonfw.tools.ide.commandlet.VersionCommandlet} that has a long-option style.
   */
  @Test
  public void testIdeCompleterVersion() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("--version ", new TestBuffer("--vers").tab());
  }

  /**
   * Test of 2nd level auto-completion with tool property of {@link com.devonfw.tools.ide.commandlet.InstallCommandlet}.
   */
  @Test
  public void testIdeCompleterInstall() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("install mvn ", new TestBuffer("install m").tab());
  }

  /**
   * Test of 2nd level auto-completion with commandlet property of
   * {@link com.devonfw.tools.ide.commandlet.HelpCommandlet}.
   */
  @Test
  public void testIdeCompleterHelpWithToolCompletion() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("help mvn ", new TestBuffer("help m").tab().tab());
  }

  /**
   * Test of second option completion that is already present as short-option.
   */
  @Test
  public void testIdeCompleterDuplicatedOptions() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("-t --t", new TestBuffer("-t --t").tab());

  }

  /**
   * Test of 3rd level completion using version property of {@link com.devonfw.tools.ide.commandlet.InstallCommandlet}
   * contextual to the specified tool. The version "3.2.1" is the latest one from the mocked "basic" project configured
   * for the tool "mvn".
   */
  @Test
  public void testIdeCompleterThirdLayerVersions() {

    String path = "workspaces/foo-test/my-git-repo";
    IdeTestContext ideContext = newContext("basic", path, false);
    this.reader.setCompleter(new IdeCompleter(ideContext));
    assertBuffer("install mvn 3.2.1", new TestBuffer("install mvn ").tab().tab());
  }

  /**
   * Test that 2nd level completion of undefined commandlet has no effect.
   */
  @Test
  public void testIdeCompleterNonExistentCommand() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("cd ", new TestBuffer("cd ").tab().tab().tab());

  }

  /**
   * Test that no options are completed on 2nd level for {@link com.devonfw.tools.ide.commandlet.VersionGetCommandlet}
   * that has no options.
   */
  @Test
  public void testIdeCompleterPreventsOptionsAfterCommandWithMinus() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("get-version -", new TestBuffer("get-version -").tab().tab());
    assertBuffer("get-version - ", new TestBuffer("get-version - ").tab().tab());

  }

  /**
   * Test that completion with invalid options does not trigger suggestions.
   */
  @Test
  public void testIdeCompleterWithInvalidInputDoesNothing() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("get-version -t ", new TestBuffer("get-version -t ").tab().tab());
    assertBuffer("- get-version ", new TestBuffer("- get-version ").tab().tab());
    assertBuffer(" - get-version", new TestBuffer(" - get-version").tab().tab());
  }

  /**
   * Test of 2nd level completion of tool property for {@link com.devonfw.tools.ide.commandlet.VersionGetCommandlet}.
   */
  @Test
  public void testIdeCompleterHandlesOptionsBeforeCommand() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("get-version mvn ", new TestBuffer("get-version mv").tab().tab());
  }

  private IdeCompleter newCompleter() {

    return new IdeCompleter(newTestContext());
  }

  private IdeTestContext newTestContext() {

    return new IdeTestContext(Path.of(""), "");
  }
}
