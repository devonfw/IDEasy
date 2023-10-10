package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

/**
 * Integration test of {@link HelpCommandlet}.
 */
public class HelpCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link HelpCommandlet} does not require home.
   */
  @Test
  public void testThatHomeIsNotReqired() {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    // act
    HelpCommandlet help = new HelpCommandlet(context);
    // assert
    assertThat(help.isIdeHomeRequired()).isEqualTo(false);
  }

  /**
   * Test of {@link HelpCommandlet} run.
   */
  @Test
  public void testRun() {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    HelpCommandlet help = new HelpCommandlet(context);
    // act
    help.run();
    // assert
    assertLogoMessage(context);
    assertLogMessage(context, IdeLogLevel.INFO, "Usage: ide [option]* [[commandlet] [arg]*]");
    assertOptionLogMessages(context);
  }

  /**
   * Test of {@link HelpCommandlet} run with a Commandlet.
   */
  @Test
  public void testRunWithCommandlet() {

    // arrange
    String path = "workspaces/foo-test/my-git-repo";
    IdeContext context = newContext("basic", path, true);
    HelpCommandlet help = context.getCommandletManager().getCommandlet(HelpCommandlet.class);
    help.commandlet.setValueAsString("mvn");
    // act
    help.run();
    // assert
    assertLogoMessage(context);
    assertLogMessage(context, IdeLogLevel.INFO, "Usage: ide [option]* mvn [<args>*]");
    assertLogMessage(context, IdeLogLevel.INFO, "Tool commandlet for Maven (Build-Tool)");
    assertOptionLogMessages(context);
  }

  /**
   * Assertion for the options that should be displayed.
   */
  public void assertOptionLogMessages(IdeContext context) {

    assertLogMessage(context, IdeLogLevel.INFO, "--locale        the locale (e.g. 'de' for German language)");
    assertLogMessage(context, IdeLogLevel.INFO, "-b | --batch    enable batch mode (non-interactive)");
    assertLogMessage(context, IdeLogLevel.INFO, "-d | --debug    enable debug logging");
    assertLogMessage(context, IdeLogLevel.INFO, "-f | --force    enable force mode");
    assertLogMessage(context, IdeLogLevel.INFO,
        "-o | --offline  enable offline mode (skip updates or git pull, fail downloads or git clone)");
    assertLogMessage(context, IdeLogLevel.INFO,
        "-q | --quiet    disable info logging (only log success, warning or error)");
    assertLogMessage(context, IdeLogLevel.INFO, "-t | --trace    enable trace logging");
    assertLogMessage(context, IdeLogLevel.INFO, "-v | --version  Print the IDE version and exit.");
  }

  /**
   * Assertion for the IDE-Logo that should be displayed.
   */
  public void assertLogoMessage(IdeContext context) {

    assertLogMessage(context, IdeLogLevel.INFO, HelpCommandlet.LOGO);
  }
}
