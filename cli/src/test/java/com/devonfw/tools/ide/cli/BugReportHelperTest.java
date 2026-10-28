package com.devonfw.tools.ide.cli;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.AbstractCommandlet;
import com.devonfw.tools.ide.commandlet.TestCommandletManager;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.git.GitContextMock;
import com.devonfw.tools.ide.tool.gh.Gh;
import com.devonfw.tools.ide.util.PrivacyUtil;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Test of {@link BugReportHelper}.
 */
class BugReportHelperTest extends AbstractIdeContextTest {

  /**
   * Installs a {@link TestCommandletManager} that returns the given {@link Gh} stub instead of the real one, so that no real GitHub CLI binary is ever
   * resolved or executed (the test must stay hermetic even when {@code gh} is present on the machine's PATH).
   *
   * @param context the {@link IdeTestContext}.
   * @param ghStub the {@link Gh} commandlet to return for {@link Gh#class}.
   */
  private void useGhStub(IdeTestContext context, Gh ghStub) {

    context.setCommandletManager(new TestCommandletManager(context) {
      @Override
      @SuppressWarnings("unchecked")
      public <C extends AbstractCommandlet> C getCommandlet(Class<C> commandletType) {

        if (commandletType == Gh.class) {
          return (C) ghStub;
        }
        return super.getCommandlet(commandletType);
      }
    });
  }

  /** Test of {@link BugReportHelper#createTitle(Throwable)}. */
  @Test
  void testCreateTitle() {

    // arrange
    RuntimeException error = new RuntimeException("something broke");

    // act
    String title = BugReportHelper.createTitle(error);

    // assert
    assertThat(title).isEqualTo("RuntimeException: something broke");
  }

  /** Test of {@link BugReportHelper#createTitle(Throwable)} without message. */
  @Test
  void testCreateTitleWithoutMessage() {

    // arrange
    NullPointerException error = new NullPointerException();

    // act
    String title = BugReportHelper.createTitle(error);

    // assert
    assertThat(title).isEqualTo(NullPointerException.class.getName());
  }

  /** Test of {@link BugReportHelper#createIssueUrl(String)}. */
  @Test
  void testCreateIssueUrl() {

    // act
    String url = BugReportHelper.createIssueUrl("RuntimeException: boom & more");

    // assert
    assertThat(url).startsWith("https://github.com/devonfw/IDEasy/issues/new?template=bug_report.yml&title=");
    assertThat(url).contains("RuntimeException");
    assertThat(url).contains("%26");
  }

  /** Test of {@link BugReportHelper#createUnexpectedErrorMessage(String)}. */
  @Test
  void testCreateUnexpectedErrorMessage() {

    // act
    String message = BugReportHelper.createUnexpectedErrorMessage("test-title");

    assertThat(message).contains("An unexpected error occurred!");
    assertThat(message).contains(BugReportHelper.createIssueUrl("test-title"));
    assertThat(message).doesNotContain("ide bugreport");
  }

  /** Test of {@link BugReportHelper#createIssueBody(com.devonfw.tools.ide.context.IdeContext, String, String, String, String, String)}. */
  @Test
  void testCreateIssueBody() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    context.getNetworkStatus().simulateOnline();

    // act
    String body = BugReportHelper.createIssueBody(context, "It crashed", "step 1", "It should not crash", "#123", "my hint");

    assertThat(body).contains("### Actual behavior");
    assertThat(body).contains("It crashed");
    assertThat(body).contains("### Reproduce");
    assertThat(body).contains("step 1");
    assertThat(body).contains("### Expected behavior");
    assertThat(body).contains("It should not crash");
    assertThat(body).contains("### IDEasy status");
    assertThat(body).contains("You are online.");
    assertThat(body).contains("Your version of IDEasy is " + IdeVersion.getVersionString());
    assertThat(body).contains("### Comments/Hints");
    assertThat(body).contains("Related: #123");
    assertThat(body).contains("my hint");
  }

  /** Test that {@link BugReportHelper#createIssueBody} falls back to placeholders for missing fields. */
  @Test
  void testCreateIssueBodyWithEmptyFields() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    context.getNetworkStatus().simulateOnline();

    // act
    String body = BugReportHelper.createIssueBody(context, "It crashed", " ", "  ", null, null);

    // assert
    assertThat(body).contains("_Please add steps to reproduce._");
    assertThat(body).contains("_Please describe the expected behavior._");
    assertThat(body).doesNotContain("### Comments/Hints");
    assertThat(body).doesNotContain("Related:");
  }

  /** Test that the {@code Comments/Hints} section is only rendered when there is content (comments and/or an exception hint). */
  @Test
  void testCreateIssueBodyWithCommentsOnly() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    context.getNetworkStatus().simulateOnline();

    // act
    String body = BugReportHelper.createIssueBody(context, "It crashed", "step 1", "It should not crash", null, "user hint about network");

    // assert
    assertThat(body).contains("### Comments/Hints");
    assertThat(body).contains("user hint about network");
    assertThat(body).doesNotContain("Related:");
  }

  /**
   * Test of {@link BugReportHelper#createStatusSnippet}. The IDE paths must be masked with {@link PrivacyUtil} so that the generated report is safe to
   * share publicly, mirroring the output of {@code ide -p status}.
   */
  @Test
  void testCreateStatusSnippetMasksPaths() {

    // arrange
    IdeTestContext context = newContext("basic", null, false);
    String rawIdeRoot = context.getIdeRoot().toString();
    String rawIdeHome = context.getIdeHome().toString();
    context.getNetworkStatus().simulateOnline();

    // act
    String snippet = BugReportHelper.createStatusSnippet(context);

    assertThat(snippet).contains("IDE_ROOT is set to " + PrivacyUtil.removeSensitivePathInformation(rawIdeRoot) + "\n");
    assertThat(snippet).contains("IDE_HOME is set to " + PrivacyUtil.removeSensitivePathInformation(rawIdeHome) + "\n");
    assertThat(snippet).doesNotContain(rawIdeRoot);
    assertThat(snippet).doesNotContain(rawIdeHome);
    // the snippet mirrors the online and settings status of `ide status`
    assertThat(snippet).contains("You are online.\n");
    assertThat(snippet).contains("Your settings are up-to-date.\n");
  }

  /**
   * Test that {@link BugReportHelper#createStatusSnippet} reflects an outdated settings repository, mirroring the output of {@code ide status}.
   */
  @Test
  void testCreateStatusSnippetWithOutdatedSettings() {

    // arrange
    IdeTestContext context = newContext("basic", null, false);
    context.getNetworkStatus().simulateOnline();
    context.setGitContext(new GitContextMock(context) {
      @Override
      public boolean isRepositoryUpdateAvailable(Path repository, Path trackedCommitIdPath) {

        return true;
      }
    });

    // act
    String snippet = BugReportHelper.createStatusSnippet(context);

    // assert
    assertThat(snippet).contains("You are online.\n");
    assertThat(snippet).contains("Your settings are not up-to-date, please run 'ide update'.\n");
  }

  /** Test that a combined code and settings repository reports up-to-date when no settings update is available, mirroring {@code ide status}. */
  @Test
  void testUpToDateSettingsStatusForCombinedCodeSettingsRepository() {

    // arrange, a settings repository that is a combined code and settings repository
    IdeTestContext context = new IdeTestContext(Path.of("/"), null) {
      @Override
      public Path getSettingsGitRepository() {

        return Path.of("settings");
      }

      @Override
      public boolean isCombinedCodeSettingsRepository() {

        return true;
      }
    };
    context.setGitContext(new GitContextMock(context) {
      @Override
      public boolean isRepositoryUpdateAvailable(Path repository, Path trackedCommitIdPath) {

        return false;
      }
    });
    context.getNetworkStatus().simulateOnline();

    // act
    IdeContext.SettingsUpdateState status = context.evaluateSettingsUpdateStatus();
    String snippet = BugReportHelper.createStatusSnippet(context);

    // assert
    assertThat(status).isEqualTo(IdeContext.SettingsUpdateState.UP_TO_DATE);
    assertThat(context.getSettingsStatusLine()).isEqualTo("Your settings are up-to-date.");
    assertThat(snippet).contains("Your settings are up-to-date.\n");
    assertThat(snippet).doesNotContain("ide update");
  }

  /** Test that a combined code and settings repository suppresses the outdated warning, mirroring {@code ide status}. */
  @Test
  void testNoOutdatedSettingsWarningForCombinedCodeSettingsRepository() {

    // arrange
    IdeTestContext context = new IdeTestContext(Path.of("/"), null) {
      @Override
      public Path getSettingsGitRepository() {

        return Path.of("settings");
      }

      @Override
      public boolean isCombinedCodeSettingsRepository() {

        return true;
      }
    };
    context.setGitContext(new GitContextMock(context) {
      @Override
      public boolean isRepositoryUpdateAvailable(Path repository, Path trackedCommitIdPath) {

        return true;
      }
    });
    context.getNetworkStatus().simulateOnline();

    // act
    IdeContext.SettingsUpdateState status = context.evaluateSettingsUpdateStatus();
    String snippet = BugReportHelper.createStatusSnippet(context);

    // assert
    assertThat(status).isEqualTo(IdeContext.SettingsUpdateState.UNKNOWN);
    assertThat(context.getSettingsStatusLine()).isEmpty();
    assertThat(snippet).doesNotContain("Your settings are not up-to-date");
  }

  /**
   * Test that {@link BugReportHelper#offerAfterError} derives the issue from the error and the invoked command (masked), prints a summary and asks
   * whether to report.
   */
  @Test
  void testOfferAfterErrorAsksAndDerivesPrefill() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    context.setAnswers("Do not report");

    // act
    BugReportHelper.offerAfterError(context, new IllegalStateException("settings are broken"), "status", "--project", "/tmp/project");

    // assert (the simple summary shows all six derived fields; the actual behaviour is the full stacktrace whose first line carries the
    // fully-qualified exception class name)
    assertThat(context).logAtInteraction().hasMessageContaining("Title: IllegalStateException: settings are broken");
    assertThat(context).logAtInteraction().hasMessageContaining("Actual behaviour: java.lang.IllegalStateException: settings are broken");
    assertThat(context).logAtInteraction().hasMessageContaining(PrivacyUtil.removeSensitivePathInformation("status --project /tmp/project"));
    assertThat(context).logAtInteraction().hasMessageContaining("Expected behaviour: No such unexpected exception should happen.");
    assertThat(context).logAtInteraction().hasMessageContaining("Related:");
    assertThat(context).logAtInteraction().hasMessageContaining("Comments:");
    assertThat(context).logAtInteraction().hasMessage("Option 1: Do not report");
    assertThat(context).logAtInteraction().hasMessage("Option 2: Report as is");
    assertThat(context).logAtInteraction().hasMessage("Option 3: Edit and then report");
  }

  /** Test that declining the bug report offers the prefilled issue URL to file it later. */
  @Test
  void testOfferAfterErrorDeclineOffersUrl() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    context.setAnswers("Do not report");

    // act
    BugReportHelper.offerAfterError(context, new IllegalStateException("boom"), "status");

    // assert
    assertThat(context).logAtInfo().hasMessageContaining(BugReportHelper.createIssueUrl("IllegalStateException: boom"));
  }

  /**
   * Test that reporting as is falls back to the prefilled browser URL when the GitHub CLI is not available and includes the auto-derived (masked)
   * details.
   */
  @Test
  void testOfferAfterErrorReportAsIsFallsBackToUrlWhenNoGh() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    context.getNetworkStatus().simulateOnline();
    context.setAnswers("Report as is");
    useGhStub(context, new Gh(context) {
      @Override
      public Path getGhBinary() {

        return null;
      }
    });

    // act
    BugReportHelper.offerAfterError(context, new IllegalStateException("boom"), "status", "--project", "/tmp/project");

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("GitHub CLI (gh) is not available");
    assertThat(context).logAtInteraction().hasMessageContaining(BugReportHelper.createIssueUrl("IllegalStateException: boom"));
    assertThat(context).logAtInfo().hasMessageContaining("### Actual behavior");
    assertThat(context).logAtInfo().hasMessageContaining("IllegalStateException: boom"); // the actual behaviour is the full stacktrace
    assertThat(context).logAtInfo().hasMessageContaining("No such unexpected exception should happen.");
    assertThat(context).logAtInfo().hasMessageContaining(PrivacyUtil.removeSensitivePathInformation("status --project /tmp/project"));
    assertThat(context).logAtInfo().hasNoMessageContaining("### Comments/Hints"); // empty related and comments are not rendered
    assertThat(context).logAtInfo().hasNoMessageContaining("<details>");
  }

  /**
   * Test that reporting as is creates the issue via the GitHub CLI when it is available and authenticated, and prints the created issue URL.
   */
  @Test
  void testOfferAfterErrorReportAsIsCreatesIssueViaGh() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    context.getNetworkStatus().simulateOnline();
    context.setAnswers("Report as is");
    useGhStub(context, new Gh(context) {
      @Override
      public Path getGhBinary() {

        return Path.of("gh");
      }

      @Override
      public boolean isGhAuthenticated() {

        return true;
      }

      @Override
      public String createIssue(String repo, String title, String body, String type) {

        return "https://github.com/devonfw/IDEasy/issues/123";
      }
    });

    // act
    BugReportHelper.offerAfterError(context, new IllegalStateException("boom"), "status");

    // assert
    assertThat(context).logAtSuccess().hasMessageContaining("https://github.com/devonfw/IDEasy/issues/123");
    assertThat(context).logAtSuccess().hasMessageContaining("Created bug report");
    assertThat(context).logAtInteraction().hasNoMessageContaining("Please open the following URL");
  }

  /**
   * Test that the "edit and then report" option lets the user adjust all prefilled values (title, actual behaviour, reproduce, expected behaviour,
   * related, comments) and reports the edited issue.
   */
  @Test
  void testOfferAfterErrorEditAndThenReport() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    context.getNetworkStatus().simulateOnline();
    context.setAnswers("Edit and then report", "edited title", "edited actual behaviour", "edited steps", "edited expected", "#99",
        "edited comments");
    final String[] createdTitle = {null};
    final String[] createdBody = {null};
    useGhStub(context, new Gh(context) {
      @Override
      public Path getGhBinary() {

        return Path.of("gh");
      }

      @Override
      public boolean isGhAuthenticated() {

        return true;
      }

      @Override
      public String createIssue(String repo, String title, String body, String type) {

        createdTitle[0] = title;
        createdBody[0] = body;
        return "https://github.com/devonfw/IDEasy/issues/123";
      }
    });

    // act
    BugReportHelper.offerAfterError(context, new IllegalStateException("boom"), "status");

    // assert (the edited values are what gets reported)
    assertThat(context).logAtSuccess().hasMessageContaining("https://github.com/devonfw/IDEasy/issues/123");
    assertThat(createdTitle[0]).isEqualTo("edited title");
    assertThat(createdBody[0]).contains("edited actual behaviour");
    assertThat(createdBody[0]).contains("edited steps");
    assertThat(createdBody[0]).contains("edited expected");
    assertThat(createdBody[0]).contains("Related: #99");
    assertThat(createdBody[0]).contains("edited comments");
  }

  /**
   * Test that the values published in the GitHub issue are sanitized: a registered secret (e.g. a token embedded in the error message) is always
   * replaced and any path the message contains is masked, so neither leaks into the public issue title or body.
   */
  @Test
  void testOfferAfterErrorSanitizesPublishedTitleAndBody() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    context.getNetworkStatus().simulateOnline();
    context.setAnswers("Report as is");
    String token = "secrettoken12345";
    context.addSecretVariable("API_TOKEN");
    context.addSecretValue("API_TOKEN", token);
    String message = "cannot reach git@github.com:hdawoud/myproject, token " + token + " is invalid";
    final String[] createdTitle = {null};
    final String[] createdBody = {null};
    useGhStub(context, new Gh(context) {
      @Override
      public Path getGhBinary() {

        return Path.of("gh");
      }

      @Override
      public boolean isGhAuthenticated() {

        return true;
      }

      @Override
      public String createIssue(String repo, String title, String body, String type) {

        createdTitle[0] = title;
        createdBody[0] = body;
        return "https://github.com/devonfw/IDEasy/issues/123";
      }
    });

    // act
    BugReportHelper.offerAfterError(context, new IllegalStateException(message), "status");

    // assert (neither the secret nor any path segment survives into the published title or body)
    assertThat(createdTitle[0]).doesNotContain(token);
    assertThat(createdTitle[0]).doesNotContain("hdawoud");
    assertThat(createdTitle[0]).doesNotContain("ide-project");
    assertThat(createdBody[0]).doesNotContain(token);
    assertThat(createdBody[0]).doesNotContain("hdawoud");
    assertThat(createdBody[0]).doesNotContain("ide-project");
  }
}
