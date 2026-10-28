package com.devonfw.tools.ide.cli;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.tool.gh.Gh;
import com.devonfw.tools.ide.util.PrivacyUtil;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Helper to prepare GitHub bug reports for IDEasy (issue URL, title, and body).
 */
public final class BugReportHelper {

  private static final Logger LOG = LoggerFactory.getLogger(BugReportHelper.class);

  /** The GitHub repository where IDEasy issues are filed. */
  public static final String REPOSITORY = "devonfw/IDEasy";

  /** The issue form template used for bug reports. */
  public static final String TEMPLATE = "bug_report.yml";

  private static final String ISSUE_NEW_BASE = "https://github.com/" + REPOSITORY + "/issues/new?template=" + TEMPLATE
      + "&title=";

  /** The prefilled "Expected behaviour" value for bug reports created after an unexpected error. */
  private static final String EXPECTED_BEHAVIOUR = "No such unexpected exception should happen.";

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
   * @param reproduce steps to reproduce the bug.
   * @param expectedBehavior description of the expected behavior.
   * @param related optionally related issues or documents; may be {@code null} or blank.
   * @param comments optional additional comments and hints; may be {@code null} or blank.
   * @return the issue body.
   */
  public static String createIssueBody(IdeContext context, String actualBehavior, String reproduce, String expectedBehavior, String related,
      String comments) {

    StringBuilder body = new StringBuilder();
    body.append("### Actual behavior\n");
    body.append(placeholderOr(actualBehavior, "_Please describe the bug._"));
    body.append("\n\n### Reproduce\n");
    body.append(placeholderOr(reproduce, "_Please add steps to reproduce._"));
    body.append("\n\n### Expected behavior\n");
    body.append(placeholderOr(expectedBehavior, "_Please describe the expected behavior._"));
    body.append("\n\n### IDEasy status\n");
    body.append("```\n");
    body.append(createStatusSnippet(context));
    body.append("```\n");
    boolean hasRelated = (related != null) && !related.isBlank();
    boolean hasComments = (comments != null) && !comments.isBlank();
    if (hasRelated || hasComments) {
      body.append("\n### Comments/Hints\n");
      if (hasRelated) {
        body.append("Related: ").append(related.trim()).append('\n');
      }
      if (hasComments) {
        if (hasRelated) {
          body.append('\n');
        }
        body.append(comments).append('\n');
      }
    }
    return body.toString();
  }

  private static String placeholderOr(String value, String placeholder) {

    return value == null || value.isBlank() ? placeholder : value;
  }

  /**
   * @param context the {@link IdeContext}.
   * @return a short status snippet for the bug report (version, OS, IDE paths, online and settings status). Paths are masked with
   *     {@link PrivacyUtil} so that the generated report is safe to share publicly, mirroring the output of {@code ide status}.
   */
  public static String createStatusSnippet(IdeContext context) {

    StringBuilder status = new StringBuilder();
    if (context.getIdeRoot() != null) {
      status.append("IDE_ROOT is set to ").append(mask(context.getIdeRoot().toString())).append('\n');
    }
    if (context.getIdeHome() != null) {
      status.append("IDE_HOME is set to ").append(mask(context.getIdeHome().toString())).append('\n');
    }
    if (context.getNetworkStatus().isOnline()) {
      status.append("You are online.\n");
    } else {
      status.append("You are offline.\n");
    }
    String settingsStatus = context.getSettingsStatusLine();
    if (!settingsStatus.isEmpty()) {
      status.append(settingsStatus).append('\n');
    }
    status.append("Your version of IDEasy is ").append(IdeVersion.getVersionString()).append('\n');
    SystemInfo systemInfo = context.getSystemInfo();
    status.append("Your operating system is ").append(systemInfo.getOs()).append('(').append(systemInfo.getOsVersion())
        .append(")@").append(systemInfo.getArchitecture()).append(" [").append(systemInfo.getOsName()).append('@')
        .append(systemInfo.getArchitectureName()).append(']').append('\n');
    return status.toString();
  }

  /**
   * @param path the path to mask.
   * @return the path with sensitive path information (e.g. user names and project names) removed, matching the
   *     behavior of {@code ide -p status}.
   */
  private static String mask(String path) {

    return PrivacyUtil.removeSensitivePathInformation(path);
  }

  /**
   * Sanitizes a value that is going to be published in the bug report (e.g. the issue title or the full stacktrace). Registered secrets are always
   * replaced (regardless of the privacy mode) via {@link AbstractIdeContext#formatArgument(Object)}, and, on top of that, any path is masked
   * unconditionally (a public issue must not leak paths even when the privacy mode is off).
   *
   * @param context the {@link IdeContext}; may be {@code null} (in which case only the path masking is applied).
   * @param value the value to sanitize; may be {@code null}.
   * @return the sanitized value, or {@code null} if {@code value} is {@code null}.
   */
  private static String sanitize(IdeContext context, String value) {

    if (value == null) {
      return null;
    }
    // first let the context replace registered secrets (always) and, when the privacy mode is on, the privacy map, then mask the paths unconditionally
    String result = (context instanceof AbstractIdeContext ideContext) ? ideContext.formatArgument(value) : value;
    return mask(result);
  }

  /**
   * Offers to automatically create a GitHub bug report after an unexpected error. All details are derived from the error and the invoked
   * command (sensitive arguments are masked); the user only has to decide whether to report. Failures in this flow are logged and never
   * rethrown so the original error remains the primary outcome.
   *
   * @param context the {@link IdeContext}; may be {@code null}.
   * @param error the unexpected error that triggered this offer.
   * @param commandArgs the command-line arguments the user ran, used (masked) for the "Reproduce" section.
   */
  public static void offerAfterError(IdeContext context, Throwable error, String... commandArgs) {

    if ((context == null) || context.isBatchMode()) {
      return;
    }
    try {
      // the title and the full stacktrace are going to be published in the issue, so sanitize them (secrets and paths) before anything else
      String title = sanitize(context, createTitle(error));
      String actualBehavior = sanitize(context, stackTraceToString(error));
      String reproduce = maskedCommandDescription(joinCommandArgs(commandArgs));
      String related = null;
      String comments = "";
      // print the simple summary of the derived bug report so the user can review it before deciding
      IdeLogLevel.INTERACTION.log(LOG,
          "You can create a bug report for this error. It would look like this (sensitive arguments are masked):\n\n"
              + "Title: {}\nActual behaviour: {}\nReproduce: {}\nExpected behaviour: {}\nRelated: {}\nComments: {}",
          title, actualBehavior, reproduce, EXPECTED_BEHAVIOUR, placeholderOr(related, ""), comments);
      String choice = context.question(new String[] { "Do not report", "Report as is", "Edit and then report" },
          "Do you want to create a bug report for this error?");
      switch (choice) {
        case "Do not report" -> LOG.info("You can also open the prefilled issue URL later:\n{}", createIssueUrl(title));
        case "Report as is" -> report(context, title, createIssueBody(context, actualBehavior, reproduce, EXPECTED_BEHAVIOUR, related, comments));
        case "Edit and then report" -> editAndReport(context, title, actualBehavior, reproduce, related, comments);
        default -> {
        }
      }
    } catch (Throwable t) {
      LOG.debug("Bug report offer failed.", t);
    }
  }

  /**
   * Edits the prefilled bug report fields with the user and then reports it
   *
   * @param context the {@link IdeContext}.
   * @param title the prefilled issue title.
   * @param actualBehavior the prefilled "Actual behaviour" value (the full stacktrace).
   * @param reproduce the prefilled (masked) "Reproduce" value.
   * @param related the prefilled "Related" value; may be {@code null}.
   * @param comments the prefilled "Comments" value; may be {@code null} or blank.
   */
  private static void editAndReport(IdeContext context, String title, String actualBehavior, String reproduce, String related, String comments) {

    String newTitle = context.askForInput("Issue title:", title);
    String newActualBehavior = context.askForInput("Actual behaviour (the exception that occurred):", actualBehavior);
    String newReproduce = context.askForInput("Steps to reproduce:", reproduce);
    String newExpectedBehavior = context.askForInput("Expected behaviour:", EXPECTED_BEHAVIOUR);
    String newRelated = context.askForInput("Related issues/documents (optional):", (related == null) ? "" : related);
    String newComments = context.askForInput("Comments/hints (optional):", comments);
    String body = createIssueBody(context, newActualBehavior, newReproduce, newExpectedBehavior, newRelated, newComments);
    report(context, newTitle, body);
  }

  /**
   * Reports the given bug report, preferring the GitHub CLI and falling back to a prefilled browser URL.
   *
   * @param context the {@link IdeContext}.
   * @param title the issue title.
   * @param body the issue body.
   */
  private static void report(IdeContext context, String title, String body) {

    Gh gh = context.getCommandletManager().getCommandlet(Gh.class);
    if (gh.getGhBinary() == null) {
      gh.offerGhInstall();
    }
    Path ghBinary = gh.getGhBinary();
    if ((ghBinary != null) && gh.ensureGhAuthenticated(true)) {
      String issueUrl = gh.createIssue(REPOSITORY, title, body, "Bug");
      if (issueUrl != null) {
        IdeLogLevel.SUCCESS.log(LOG, "Created bug report: {}", issueUrl);
        return;
      }
      LOG.warn("Creating the issue via GitHub CLI failed. Falling back to the browser URL.");
    }
    openViaUrl(title, body);
  }

  /**
   * Prints the prefilled browser issue URL so the user can file the bug report manually (e.g. when the GitHub CLI is unavailable).
   *
   * @param title the issue title.
   * @param body the prepared issue body to copy into the form.
   */
  private static void openViaUrl(String title, String body) {

    String url = createIssueUrl(title);
    IdeLogLevel.INTERACTION.log(LOG,
        "Please open the following URL to file the bug report and paste the prepared details into the form:\n{}", url);
    LOG.info("Prepared issue body:\n{}", body);
  }

  /**
   * @param error the unexpected error; may be {@code null}.
   * @return the full stacktrace of the given error, or {@code null} if the error is {@code null}.
   */
  private static String stackTraceToString(Throwable error) {

    if (error == null) {
      return null;
    }
    StringWriter stringWriter = new StringWriter();
    try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
      error.printStackTrace(printWriter); // checkstyle:ignore PrintStackTrace - formats into a StringWriter for the bug report, no console output
    }
    return stringWriter.toString();
  }

  /**
   * @param commandArgs the command-line arguments to join.
   * @return the arguments joined with a single space, or an empty string if there are none.
   */
  private static String joinCommandArgs(String... commandArgs) {

    return (commandArgs == null) ? "" : String.join(" ", commandArgs);
  }

  /**
   * @param commandLine the masked, human-readable command the user ran; may be {@code null} or blank.
   * @return the "Reproduce" section value. Sensitive arguments are already removed via {@link PrivacyUtil}.
   */
  private static String maskedCommandDescription(String commandLine) {

    if ((commandLine == null) || commandLine.isBlank()) {
      return "an IDEasy command was called";
    }
    return "Happened when \"ide «" + PrivacyUtil.removeSensitivePathInformation(commandLine) + "»\" was called.";
  }

  /**
   * @param title the issue title.
   * @return the user-facing unexpected-error message including the improved bug-report URL and a hint that a bug report may be created
   *     automatically right after the error.
   */
  public static String createUnexpectedErrorMessage(String title) {

    return "An unexpected error occurred!\n" //
        + "We are sorry for the inconvenience.\n" //
        + "Please check the error below, resolve it and try again.\n" //
        + "If the error is not on your end (network connectivity, lack of permissions, etc.) you may be prompted to create a bug report,\n" //
        + "or you can file one manually at:\n" //
        + createIssueUrl(title);
  }
}
