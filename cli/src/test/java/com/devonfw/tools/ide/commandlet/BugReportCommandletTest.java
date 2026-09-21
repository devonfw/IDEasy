package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.BugReportHelper;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link BugReportCommandlet}.
 */
class BugReportCommandletTest extends AbstractIdeContextTest {

  /** Test that the user can decline creating a bug report. */
  @Test
  void testSkipWhenUserDeclines() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    context.setAnswers("no");
    BugReportCommandlet bugreport = context.getCommandletManager().getCommandlet(BugReportCommandlet.class);

    // act
    bugreport.run();

    // assert
    assertThat(context).logAtInfo().hasMessage("Bug report skipped.");
  }

  /** Test URL fallback when GitHub CLI is not available. */
  @Test
  void testFallsBackToUrlWhenGhUnavailable() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    context.setAnswers("yes", "title from test", "actual behavior from test");
    BugReportCommandlet bugreport = new BugReportCommandlet(context) {
      @Override
      protected Path resolveGhBinary() {

        return null;
      }
    };

    // act
    bugreport.run();

    // assert
    assertThat(context).logAtInteraction()
        .hasMessageContaining(BugReportHelper.createIssueUrl("title from test"));
    assertThat(context).logAtInfo().hasMessageContaining("### Actual behavior");
    assertThat(context).logAtInfo().hasMessageContaining("actual behavior from test");
  }

  /** Test registration of the commandlet. */
  @Test
  void testCommandletRegistered() {

    // arrange
    IdeTestContext context = IdeTestContext.of();

    // act
    BugReportCommandlet bugreport = context.getCommandletManager().getCommandlet(BugReportCommandlet.class);

    // assert
    assertThat(bugreport.getName()).isEqualTo("bugreport");
    assertThat(bugreport.isIdeRootRequired()).isFalse();
  }
}
