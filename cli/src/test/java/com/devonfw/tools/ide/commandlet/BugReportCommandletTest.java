package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.BugReportHelper;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.CapturingProcessContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.process.OutputMessage;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;

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
    context.getNetworkStatus().simulateOnline();
    context.setAnswers("yes", "title from test", "actual behavior from test", "step 1 from test", "expected from test", "extra comments from test");
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
    assertThat(context).logAtInfo().hasMessageContaining("### Reproduce");
    assertThat(context).logAtInfo().hasMessageContaining("step 1 from test");
    assertThat(context).logAtInfo().hasMessageContaining("### Expected behavior");
    assertThat(context).logAtInfo().hasMessageContaining("expected from test");
    assertThat(context).logAtInfo().hasMessageContaining("### Comments/Hints");
    assertThat(context).logAtInfo().hasMessageContaining("extra comments from test");
  }

  /** Test that the optional comments and hints input can be left empty. */
  @Test
  void testCommentsAndHintsCanBeSkipped() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    context.getNetworkStatus().simulateOnline();
    context.setAnswers("yes", "title from test", "actual behavior from test", "step 1 from test", "expected from test", "");
    BugReportCommandlet bugreport = new BugReportCommandlet(context) {
      @Override
      protected Path resolveGhBinary() {

        return null;
      }
    };

    // act
    bugreport.run();

    // assert
    assertThat(context).logAtInfo().hasNoMessageContaining("### Comments/Hints");
  }

  /**
   * Test that the {@code gh issue create} command is issued with {@code --type Bug} so the created issue is marked as a bug.
   */
  @Test
  void testGhIssueCreateUsesBugType() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    context.getNetworkStatus().simulateOnline();
    context.setAnswers("yes", "title from test", "actual behavior from test", "step 1 from test", "expected from test", "extra comments from test");
    Path ghBinary = Path.of("gh");
    CapturingProcessContextTest processContext = new CapturingProcessContextTest(context) {
      @Override
      public ProcessResult run(ProcessMode processMode) {

        return new ProcessResultImpl("gh", "gh", ProcessResult.SUCCESS, List.of(new OutputMessage(false, "https://github.com/devonfw/IDEasy/issues/123")));
      }
    };
    context.setProcessContext(processContext);
    BugReportCommandlet bugreport = new BugReportCommandlet(context) {
      @Override
      protected Path resolveGhBinary() {

        return ghBinary;
      }
    };

    // act
    bugreport.run();

    // assert
    List<String> args = processContext.getArgs();
    assertThat(args).contains("issue", "create", "--repo", BugReportHelper.REPOSITORY);
    assertThat(args).endsWith("--type", "Bug");
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
