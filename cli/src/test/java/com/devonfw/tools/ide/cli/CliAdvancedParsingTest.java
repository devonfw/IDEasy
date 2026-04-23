package com.devonfw.tools.ide.cli;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

import java.util.stream.Collectors;

/**
 * Integration test of {@link com.devonfw.tools.ide.context.AbstractIdeContext#run(CliArguments) CLI parsing}.
 */
class CliAdvancedParsingTest extends AbstractIdeContextTest {

  private static final String PROJECT_MVN = "mvn";

  /**
   * Test that implicit end-options is triggered for multi-valued arguments to prevent splitting odd-formatted short-options like "-version".
   */
  @Test
  void testPreventShortOptionsForMultivaluedArguments() {

    // arrange
    IdeTestContext context = newContext(PROJECT_MVN);
    CliArguments args = new CliArguments("java", "-version");
    args.next();
    // act
    context.run(args);
    // assert
    assertThat(context).logAtInfo().hasNoMessage("java -v -e -r -s -i -o -n").hasMessage("java -version");
  }

  /**
   * Tests if a 'repository setup' with a missing repository argument will succeed.
   * <p>
   * See: <a href="https://github.com/devonfw/IDEasy/issues/537">#537</a>
   */
  @Test
  void testRunRepositorySetupWithoutArgumentWillSucceed() {

    // arrange
    IdeTestContext context = newContext(PROJECT_MVN);
    CliArguments args = new CliArguments("repository", "setup");
    args.next();
    // act
    int success = context.run(args);
    // assert
    assertThat(success).isEqualTo(0);
  }

  @Test
  void testRunCommandletWithUnknownOption() {
    IdeTestContext context = newContext(PROJECT_MVN);
    CliArguments args = new CliArguments("install", "unknownOption");
    args.next();
    final String possibleCommands = context.getCommandletManager().getCommandlets().stream()
        .filter(c -> c instanceof ToolCommandlet)
        .map(Commandlet::getName)
        .map(n -> "'" + n + "'")
        .collect(Collectors.joining(", "));

    int resultStatus = context.run(args);
    assertThat(resultStatus).isEqualTo(1);
    assertThat(context).logAtError().hasMessage("Option unknownOption not found for commandlet install.");
    assertThat(context).logAtError().hasMessage("Did you mean one of [" + possibleCommands + "]?");
    assertThat(context).logAtInteraction().hasMessage("To see the available options and arguments call the following command:\n"
        + "ide install help");
  }

  @Test
  void testRunCommandletWithUnknownOptionParamValue() {
    IdeTestContext context = newContext(PROJECT_MVN);
    CliArguments args = new CliArguments("set-version", "intellij", "--cfg=config");
    args.next();

    int resultStatus = context.run(args);
    assertThat(resultStatus).isEqualTo(1);
    assertThat(context).logAtError().hasMessage("Invalid CLI argument 'config' for property '--cfg' of commandlet 'set-version'");
    assertThat(context).logAtError().hasMessage("Did you mean one of ['user', 'settings', 'workspace', 'conf']?");
    assertThat(context).logAtInteraction().hasMessage("To see the available options and arguments call the following command:\n"
        + "ide set-version help");
  }
}
