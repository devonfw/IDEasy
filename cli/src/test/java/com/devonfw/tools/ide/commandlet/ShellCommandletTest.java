package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link ShellCommandlet}.
 */
class ShellCommandletTest extends AbstractIdeContextTest {
  @Test
  public void testExitIsOfferedAsCompletion() {
    IdeTestContext context = new IdeTestContext();
    CliArguments args = CliArguments.of(0, "");

    List<String> lines = context.complete(args, false).stream()
                         .map(CompletionCandidate::text).toList();
    assertThat(lines).contains("exit");
  }

  @Test
  public void testExitIsOfferedOnPartialInput() {
    IdeTestContext context = new IdeTestContext();
    CliArguments args = CliArguments.of(0, "ex");

    List<String> lines = context.complete(args, false).stream()
                         .map(CompletionCandidate::text).toList();
    assertThat(lines).containsExactly("exit");
  }
}
