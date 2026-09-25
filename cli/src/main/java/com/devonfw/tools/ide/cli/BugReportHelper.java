package com.devonfw.tools.ide.cli;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Helper to prepare GitHub bug reports for IDEasy (issue URL, title, and body).
 */
public final class BugReportHelper {

  /** The GitHub repository where IDEasy issues are filed. */
  public static final String REPOSITORY = "devonfw/IDEasy";

  /** The issue form template used for bug reports. */
  public static final String TEMPLATE = "bug_report.yml";

  private static final String ISSUE_NEW_BASE = "https://github.com/" + REPOSITORY + "/issues/new?template=" + TEMPLATE
      + "&title=";

  private BugReportHelper() {

  }

  /**
   * @param error the unexpected {@link Throwable}.
   * @return a short issue title derived from the error.
   */
  public static String createTitle(Throwable error) {

    String title = error.getMessage();
    if (title == null) {
      title = error.getClass().getName();
    } else {
      title = error.getClass().getSimpleName() + ": " + title;
    }
    return title;
  }

  /**
   * @param title the issue title.
   * @return the GitHub "new issue" URL with {@link #TEMPLATE} and the given title.
   */
  public static String createIssueUrl(String title) {

    return ISSUE_NEW_BASE + URLEncoder.encode(title, StandardCharsets.UTF_8);
  }

  /**
   * Builds a markdown issue body suitable for {@code gh issue create} and for copy/paste into the browser form.
   *
   * @param context the {@link IdeContext}.
   * @param actualBehavior description of the actual behavior / bug.
   * @param stackTrace optional exception summary to append as a hint; may be {@code null}. The full stacktrace belongs in the IDEasy log, not here.
   * @return the issue body.
   */
  public static String createIssueBody(IdeContext context, String actualBehavior, String stackTrace) {

    StringBuilder body = new StringBuilder();
    body.append("### Actual behavior\n");
    body.append(actualBehavior == null || actualBehavior.isBlank() ? "_Please describe the bug._" : actualBehavior);
    body.append("\n\n### Reproduce\n");
    body.append("_Please add steps to reproduce._");
    body.append("\n\n### Expected behavior\n");
    body.append("_Please describe the expected behavior._");
    body.append("\n\n### IDEasy status\n");
    body.append("```\n");
    body.append(createStatusSnippet(context));
    body.append("```\n");
    if (stackTrace != null && !stackTrace.isBlank()) {
      body.append("\n### Comments/Hints\n");
      body.append("<details>\n<summary>Exception</summary>\n\n```\n");
      body.append(stackTrace);
      body.append("\n```\n</details>\n");
    }
    return body.toString();
  }

  /**
   * @param context the {@link IdeContext}.
   * @return a short status snippet for the bug report (version, OS, IDE paths).
   */
  public static String createStatusSnippet(IdeContext context) {

    StringBuilder status = new StringBuilder();
    if (context.getIdeRoot() != null) {
      status.append("IDE_ROOT is set to ").append(context.getIdeRoot()).append('\n');
    }
    if (context.getIdeHome() != null) {
      status.append("IDE_HOME is set to ").append(context.getIdeHome()).append('\n');
    }
    status.append("Your version of IDEasy is ").append(IdeVersion.getVersionString()).append('\n');
    SystemInfo systemInfo = context.getSystemInfo();
    status.append("Your operating system is ").append(systemInfo.getOs()).append('(').append(systemInfo.getOsVersion())
        .append(")@").append(systemInfo.getArchitecture()).append(" [").append(systemInfo.getOsName()).append('@')
        .append(systemInfo.getArchitectureName()).append(']').append('\n');
    return status.toString();
  }

  /**
   * @param title the issue title.
   * @return the user-facing unexpected-error message including the improved bug-report URL and a hint for
   *     {@code ide bugreport}.
   */
  public static String createUnexpectedErrorMessage(String title) {

    return "An unexpected error occurred!\n" //
        + "We are sorry for the inconvenience.\n" //
        + "Please check the error below, resolve it and try again.\n" //
        + "If the error is not on your end (network connectivity, lack of permissions, etc.) please file a bug:\n" //
        + createIssueUrl(title) + "\n" //
        + "You can also run 'ide bugreport' to create the report interactively (optionally via GitHub CLI).";
  }
}
