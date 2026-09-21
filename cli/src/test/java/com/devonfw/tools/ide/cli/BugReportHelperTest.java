package com.devonfw.tools.ide.cli;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
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

  /** Test of {@link BugReportHelper#createIssueBody(com.devonfw.tools.ide.context.IdeContext, String, String)}. */
  @Test
  void testCreateIssueBody() {

    // arrange
    IdeTestContext context = IdeTestContext.of();

    // act
    String body = BugReportHelper.createIssueBody(context, "It crashed", "stacktrace-line");

    // assert
    assertThat(body).contains("### Actual behavior");
    assertThat(body).contains("It crashed");
    assertThat(body).contains("### IDEasy status");
    assertThat(body).contains("Your version of IDEasy is " + IdeVersion.getVersionString());
    assertThat(body).contains("stacktrace-line");
  }
}
