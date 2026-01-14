package com.devonfw.tools.ide.completion;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.AutocompletionReaderTestSupport;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Integration test of {@link IdeCompleter}.
 */
class IdeCompleterTest extends AutocompletionReaderTestSupport {

  /**
   * Test of 1st level auto-completion (commandlet name). As suggestions are sorted alphabetically "helm" will be the first match.
   */
  @Test
  void testIdeCompleterHelp() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("helm", new TestBuffer("he").tab().tab());
  }

  /**
   * Test of 1st level auto-completion (commandlet name). Here we test the special case of the {@link com.devonfw.tools.ide.commandlet.VersionCommandlet} that
   * has a long-option style.
   */
  @Test
  void testIdeCompleterVersion() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("--version ", new TestBuffer("--vers").tab());
  }

  /**
   * Test of 1st level auto-completion (commandlet name). Here we test the special case of the {@link com.devonfw.tools.ide.commandlet.VersionCommandlet} that
   * has a long-option style.
   */
  @Test
  void testIdeCompleterBatch() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("--batch ", new TestBuffer("--b").tab());
  }

  /**
   * Test of 2nd level auto-completion with tool property of {@link com.devonfw.tools.ide.commandlet.InstallCommandlet}.
   */
  @Test
  void testIdeCompleterInstall() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("install mvn ", new TestBuffer("install m").tab());
  }

  /**
   * Test of 2nd level auto-completion with commandlet property of {@link com.devonfw.tools.ide.commandlet.HelpCommandlet}.
   */
  @Test
  void testIdeCompleterHelpWithToolCompletion() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("help mvn ", new TestBuffer("help m").tab().tab());
  }

  /**
   * Test of second option completion that is already present as short-option.
   */
  @Test
  void testIdeCompleterDuplicatedOptions() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("-t --t", new TestBuffer("-t --t").tab());
  }

  /**
   * Test of 3rd level completion using version property of {@link com.devonfw.tools.ide.commandlet.InstallCommandlet} contextual to the specified tool. The
   * version "3.2.1" is the latest one from the mocked "basic" project configured for the tool "mvn".
   */
  @Test
  void testIdeCompleterThirdLayerVersions() {

    IdeTestContext ideContext = newContext(PROJECT_BASIC, null, false);
    this.reader.setCompleter(new IdeCompleter(ideContext));
    assertBuffer("install mvn 3.2.1", new TestBuffer("install mvn ").tab().tab());
  }

  /**
   * Test that 2nd level completion of undefined commandlet has no effect.
   */
  @Test
  void testIdeCompleterNonExistentCommand() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("cd ", new TestBuffer("cd ").tab().tab().tab());

  }

  /**
   * Test that no options are completed on 2nd level for {@link com.devonfw.tools.ide.commandlet.VersionGetCommandlet} that has no options.
   */
  @Test
  void testIdeCompleterPreventsOptionsAfterCommandWithMinus() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("get-version --configured ", new TestBuffer("get-version -").tab().tab());
    assertBuffer("get-version - ", new TestBuffer("get-version - ").tab().tab());

  }

  /**
   * Test that completion with invalid options does not trigger suggestions.
   */
  @Test
  void testIdeCompleterWithInvalidInputDoesNothing() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("get-version -t ", new TestBuffer("get-version -t ").tab().tab());
    assertBuffer("- get-version ", new TestBuffer("- get-version ").tab().tab());
    assertBuffer(" - get-version", new TestBuffer(" - get-version").tab().tab());
  }

  /**
   * Test of 2nd level completion of tool property for {@link com.devonfw.tools.ide.commandlet.VersionGetCommandlet}.
   */
  @Test
  void testIdeCompleterHandlesOptionsBeforeCommand() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("get-version mvn ", new TestBuffer("get-version mv").tab().tab());
  }

  /**
   * Test of completion of options after commandlets.
   */
  @Test
  void testIdeCompleterWithOptionAfterCommandletWorks() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("env --bash ", new TestBuffer("env --ba").tab().tab());
  }

  /**
   * Test of completion of repository.
   */
  @Test
  void testIdeCompleterWithRepository() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("repository setup ", new TestBuffer("repository ").tab().tab());
  }

  /**
   * Test of completion of options and arguments after commandlets.
   */
  @Test
  void testIdeCompleterWithOptionAndArguments() {

    this.reader.setCompleter(newCompleter());
    assertBuffer("get-version --configured ", new TestBuffer("get-version --c").tab().tab());
  }

  private IdeCompleter newCompleter() {

    return new IdeCompleter(newTestContext());
  }

  private IdeTestContext newTestContext() {

    return newContext(PROJECT_BASIC, null, false);
  }
}
