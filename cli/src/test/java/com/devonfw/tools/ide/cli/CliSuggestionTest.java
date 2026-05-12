package com.devonfw.tools.ide.cli;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Tests for CLI suggestion behavior:
 * <ul>
 *   <li>Missing IDE project context</li>
 *   <li>Invalid option with suggestion</li>
 *   <li>Unknown commandlet with suggestion</li>
 *   <li>Invalid enum property value with suggestion</li>
 * </ul>
 */
class CliSuggestionTest extends AbstractIdeContextTest {

  private static final String PROJECT_BASIC = "basic";


  @Test
  void testMissingProjectContextSuggestsProblem() {

    IdeTestContext context = new IdeTestContext();
    context.getTestStartContext().getEntries().clear();

    CliArguments args = new CliArguments("update");
    args.next();
    int exit = context.run(args);

    assertThat(exit).isEqualTo(1);

    assertThat(context).log().hasEntries(
        new IdeLogEntry(IdeLogLevel.ERROR, "The update commandlet requires to be an IDEasy project to work.", true),
        new IdeLogEntry(IdeLogLevel.INTERACTION, "Please run \"icd <project-name>\" before calling \"ide update\".", true),
        new IdeLogEntry(IdeLogLevel.INTERACTION, "Call \"ide help\" for additional details.", true)
    );
  }

  @Test
  void testInvalidOptionSuggestsClosestMatch() {

    IdeTestContext context = newContext(PROJECT_BASIC);
    context.getTestStartContext().getEntries().clear();

    CliArguments args = new CliArguments("upgrade", "--mdoe");
    args.next();

    int exit = context.run(args);

    assertThat(exit).isEqualTo(1);

    assertThat(context).log().hasEntries(
        new IdeLogEntry(IdeLogLevel.ERROR, "Option \"--mdoe\" not found for commandlet \"upgrade\"", true),
        new IdeLogEntry(IdeLogLevel.INTERACTION, "Did you mean \"--mode\"?", true),
        new IdeLogEntry(IdeLogLevel.INTERACTION, "Available options are: --mode.", true),
        new IdeLogEntry(IdeLogLevel.INTERACTION, "Call \"ide help upgrade\" for additional details.", true)
    );

  }

  @Test
  void testUnknownCommandSuggestsClosestCommand() {

    IdeTestContext context = new IdeTestContext();
    context.getTestStartContext().getEntries().clear();

    CliArguments args = new CliArguments("updtae");
    args.next();

    int exit = context.run(args);

    assertThat(exit).isEqualTo(1);
    assertThat(context).log().hasEntries(
        new IdeLogEntry(IdeLogLevel.ERROR, "Unknown command \"updtae\".", true),
        new IdeLogEntry(IdeLogLevel.INTERACTION, "Did you mean \"update\"?", true),
        new IdeLogEntry(IdeLogLevel.INTERACTION, "Call \"ide help\" for additional details.", true)
    );
  }

  @Test
  void testInvalidEnumValueSuggestsCorrectValue() {

    IdeTestContext context = newContext(PROJECT_BASIC);
    context.getTestStartContext().getEntries().clear();

    // Test set-edition commandlet with invalid --cfg value 'config'
    // Valid values are: user, settings, workspace, conf (from EnvironmentVariablesFiles enum)
    CliArguments args = new CliArguments("set-edition", "java", "11", "--cfg", "config");
    args.next();

    int exit = context.run(args);

    assertThat(exit).isEqualTo(1);

    assertThat(context).log().hasEntries(
        new IdeLogEntry(IdeLogLevel.WARNING,
            "Invalid CLI argument 'config' for property '--cfg' of commandlet 'set-edition'", true),
        new IdeLogEntry(IdeLogLevel.INTERACTION, "Did you mean \"--cfg=conf\"?", true),
        new IdeLogEntry(IdeLogLevel.INTERACTION, "Valid values for '--cfg' are: user, settings, workspace, conf.", true),
        new IdeLogEntry(IdeLogLevel.INTERACTION, "Call \"ide help set-edition\" for additional details.", true)
    );
  }


  @Test
  void testInvalidEnumValueWithoutSuggestionStillShowsAllowedValues() {

    IdeTestContext context = newContext(PROJECT_BASIC);
    context.getTestStartContext().getEntries().clear();

    CliArguments args = new CliArguments("set-edition", "java", "11", "--cfg", "zzzzzz");
    args.next();

    int exit = context.run(args);

    assertThat(exit).isEqualTo(1);
    assertThat(context).log().hasEntries(
        new IdeLogEntry(IdeLogLevel.WARNING,
            "Invalid CLI argument 'zzzzzz' for property '--cfg' of commandlet 'set-edition'", true),
        new IdeLogEntry(IdeLogLevel.INTERACTION, "Valid values for '--cfg' are: user, settings, workspace, conf.", true),
        new IdeLogEntry(IdeLogLevel.INTERACTION, "Call \"ide help set-edition\" for additional details.", true)
    );
    assertThat(context).log().hasNoMessageContaining("Did you mean");
  }
}
