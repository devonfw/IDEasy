package com.devonfw.tools.ide.cli;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.util.PrivacyUtil;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Test of {@link BugReportHelper}.
 */
class BugReportHelperTest extends AbstractIdeContextTest {

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

    // assert
    assertThat(message).contains("An unexpected error occurred!");
    assertThat(message).contains(BugReportHelper.createIssueUrl("test-title"));
    assertThat(message).contains("ide bugreport");
  }

  /** Test of {@link BugReportHelper#createIssueBody(com.devonfw.tools.ide.context.IdeContext, String, String, String, String)}. */
  @Test
  void testCreateIssueBody() {

    // arrange
    IdeTestContext context = IdeTestContext.of();

    // act
    String body = BugReportHelper.createIssueBody(context, "It crashed", "step 1", "It should not crash", "stacktrace-line");

    // assert
    assertThat(body).contains("### Actual behavior");
    assertThat(body).contains("It crashed");
    assertThat(body).contains("### Reproduce");
    assertThat(body).contains("step 1");
    assertThat(body).contains("### Expected behavior");
    assertThat(body).contains("It should not crash");
    assertThat(body).contains("### IDEasy status");
    assertThat(body).contains("Your version of IDEasy is " + IdeVersion.getVersionString());
    assertThat(body).contains("stacktrace-line");
  }

  /** Test that {@link BugReportHelper#createIssueBody} falls back to placeholders for missing fields. */
  @Test
  void testCreateIssueBodyWithEmptyFields() {

    // arrange
    IdeTestContext context = IdeTestContext.of();

    // act
    String body = BugReportHelper.createIssueBody(context, "It crashed", " ", "  ", null);

    // assert
    assertThat(body).contains("_Please add steps to reproduce._");
    assertThat(body).contains("_Please describe the expected behavior._");
    assertThat(body).doesNotContain("### Comments/Hints");
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

    // act
    String snippet = BugReportHelper.createStatusSnippet(context);

    // assert
    assertThat(snippet).contains("IDE_ROOT is set to " + PrivacyUtil.removeSensitivePathInformation(rawIdeRoot) + "\n");
    assertThat(snippet).contains("IDE_HOME is set to " + PrivacyUtil.removeSensitivePathInformation(rawIdeHome) + "\n");
    assertThat(snippet).doesNotContain(rawIdeRoot);
    assertThat(snippet).doesNotContain(rawIdeHome);
  }
}
