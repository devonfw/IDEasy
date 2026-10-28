package com.devonfw.tools.ide.tool.gh;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.BugReportHelper;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.process.OutputMessage;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessContextImpl;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;

/**
 * Test of the reusable GitHub CLI (gh) operations in {@link Gh} (authentication and issue creation).
 */
class GhTest extends AbstractIdeContextTest {

  private static ProcessResult success() {

    return new ProcessResultImpl("gh", "gh", ProcessResult.SUCCESS, List.of());
  }

  /**
   * @param context the {@link IdeTestContext}.
   * @param result the {@link ProcessResult} to return for every invocation.
   * @return a capturing {@link ProcessContext} that records the executed commands and returns the given result.
   */
  private static ProcessContext capturingProcess(IdeTestContext context, ProcessResult result) {

    List<List<String>> runs = new ArrayList<>();
    ProcessContextImpl mock = new ProcessContextImpl(context) {
      @Override
      public ProcessResult run(ProcessMode processMode) {

        runs.add(List.copyOf(arguments));
        arguments.clear();
        executable = null;
        return result;
      }
    };
    context.setProcessContext(mock);
    return mock;
  }

  private static ProcessResult failure() {

    return new ProcessResultImpl("gh", "gh", 1, List.of());
  }

  /** Test that {@link Gh#getGhBinary()} returns the binary when it is available. */
  @Test
  void testGetGhBinaryWhenAvailable() {

    IdeTestContext context = IdeTestContext.of();
    Gh gh = new Gh(context) {
      @Override
      public Path getGhBinary() {

        return Path.of("gh");
      }
    };

    assertThat(gh.getGhBinary()).isEqualTo(Path.of("gh"));
  }

  /** Test that {@link Gh#isGhAuthenticated()} is {@code false} when the binary is not available. */
  @Test
  void testIsGhAuthenticatedFalseWhenNoBinary() {

    IdeTestContext context = IdeTestContext.of();
    capturingProcess(context, success());
    Gh gh = new Gh(context) {
      @Override
      public Path getGhBinary() {

        return null;
      }
    };

    assertThat(gh.isGhAuthenticated()).isFalse();
  }

  /** Test that {@link Gh#isGhAuthenticated()} is {@code true} when {@code gh auth status} succeeds. */
  @Test
  void testIsGhAuthenticatedTrueWhenAuthStatusSucceeds() {

    IdeTestContext context = IdeTestContext.of();
    capturingProcess(context, success());
    Gh gh = new Gh(context) {
      @Override
      public Path getGhBinary() {

        return Path.of("gh");
      }
    };

    assertThat(gh.isGhAuthenticated()).isTrue();
  }

  /** Test that {@link Gh#ensureGhAuthenticated(boolean)} returns {@code true} without prompting when already authenticated. */
  @Test
  void testEnsureGhAuthenticatedWhenAlreadyAuthenticated() {

    IdeTestContext context = IdeTestContext.of();
    capturingProcess(context, success());
    Gh gh = new Gh(context) {
      @Override
      public Path getGhBinary() {

        return Path.of("gh");
      }
    };

    assertThat(gh.ensureGhAuthenticated(true)).isTrue();
  }

  /** Test that {@link Gh#ensureGhAuthenticated(boolean)} offers {@code gh auth login} and returns {@code true} when the user accepts. */
  @Test
  void testEnsureGhAuthenticatedOffersLogin() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    final int[] statusCalls = {0};
    ProcessContextImpl mock = new ProcessContextImpl(context) {
      @Override
      public ProcessResult run(ProcessMode processMode) {

        List<String> args = List.copyOf(arguments);
        arguments.clear();
        executable = null;
        if (args.contains("login")) {
          return success();
        }
        if (args.contains("status")) {
          statusCalls[0]++;
          // the first 'auth status' reports not authenticated, subsequent ones succeed after login
          return (statusCalls[0] >= 2) ? success() : failure();
        }
        return success();
      }
    };
    context.setProcessContext(mock);
    context.setAnswers("yes");
    Gh gh = new Gh(context) {
      @Override
      public Path getGhBinary() {

        return Path.of("gh");
      }
    };

    // act
    boolean authenticated = gh.ensureGhAuthenticated(true);

    // assert
    assertThat(authenticated).isTrue();
  }

  /** Test that {@link Gh#ensureGhAuthenticated(boolean)} returns {@code false} when the user declines to log in. */
  @Test
  void testEnsureGhAuthenticatedDeclinesLogin() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    capturingProcess(context, failure());
    context.setAnswers("no");
    Gh gh = new Gh(context) {
      @Override
      public Path getGhBinary() {

        return Path.of("gh");
      }
    };

    // act
    boolean authenticated = gh.ensureGhAuthenticated(true);

    // assert
    assertThat(authenticated).isFalse();
  }

  /** Test that {@link Gh#createIssue(String, String, String, String)} returns the issue URL on success and issues the expected command. */
  @Test
  void testCreateIssueReturnsUrlOnSuccess() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    ProcessContext mock = capturingProcess(context,
        new ProcessResultImpl("gh", "gh issue create", ProcessResult.SUCCESS,
            List.of(new OutputMessage(false, "https://github.com/devonfw/IDEasy/issues/123"))));
    Gh gh = new Gh(context) {
      @Override
      public Path getGhBinary() {

        return Path.of("gh");
      }
    };

    // act
    String issueUrl = gh.createIssue(BugReportHelper.REPOSITORY, "boom", "the body", "Bug");

    // assert
    assertThat(issueUrl).isEqualTo("https://github.com/devonfw/IDEasy/issues/123");
  }

  /** Test that {@link Gh#createIssue(String, String, String, String)} issues {@code gh issue create ... --type Bug} with a body file. */
  @Test
  void testCreateIssueUsesBugTypeAndBodyFile() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    List<List<String>> runs = new ArrayList<>();
    ProcessContextImpl mock = new ProcessContextImpl(context) {
      @Override
      public ProcessResult run(ProcessMode processMode) {

        runs.add(List.copyOf(arguments));
        arguments.clear();
        executable = null;
        return new ProcessResultImpl("gh", "gh issue create", ProcessResult.SUCCESS,
            List.of(new OutputMessage(false, "https://github.com/devonfw/IDEasy/issues/123")));
      }
    };
    context.setProcessContext(mock);
    Gh gh = new Gh(context) {
      @Override
      public Path getGhBinary() {

        return Path.of("gh");
      }
    };

    // act
    gh.createIssue(BugReportHelper.REPOSITORY, "boom", "the body", "Bug");

    // assert
    List<String> args = runs.get(0);
    assertThat(args).contains("issue", "create", "--repo", BugReportHelper.REPOSITORY);
    assertThat(args).contains("--body-file");
    assertThat(args).endsWith("--type", "Bug");
  }

  /** Test that {@link Gh#createIssue(String, String, String, String)} returns {@code null} when the {@code gh issue create} call fails. */
  @Test
  void testCreateIssueReturnsNullOnFailure() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    capturingProcess(context, new ProcessResultImpl("gh", "gh issue create", 1, List.of()));
    Gh gh = new Gh(context) {
      @Override
      public Path getGhBinary() {

        return Path.of("gh");
      }
    };

    // act
    String issueUrl = gh.createIssue(BugReportHelper.REPOSITORY, "boom", "the body", "Bug");

    // assert
    assertThat(issueUrl).isNull();
  }

  /** Test that {@link Gh#createIssue(String, String, String, String)} returns {@code null} when the binary is not available. */
  @Test
  void testCreateIssueReturnsNullWhenNoBinary() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    capturingProcess(context, success());
    Gh gh = new Gh(context) {
      @Override
      public Path getGhBinary() {

        return null;
      }
    };

    // act
    String issueUrl = gh.createIssue(BugReportHelper.REPOSITORY, "boom", "the body", "Bug");

    // assert
    assertThat(issueUrl).isNull();
  }

  /** Test that {@link Gh#offerGhInstall()} does not prompt when the binary is already available. */
  @Test
  void testOfferGhInstallNoOpWhenAvailable() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    Gh gh = new Gh(context) {
      @Override
      public Path getGhBinary() {

        return Path.of("gh");
      }
    };

    // act (no answers set on purpose - no prompt must be issued)
    gh.offerGhInstall();

    // assert
    assertThat(context).logAtInfo().hasNoMessageContaining("install it with IDEasy");
  }
}
