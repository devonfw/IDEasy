package com.devonfw.tools.ide.commandlet;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.BugReportHelper;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.gh.Gh;

/**
 * {@link Commandlet} to help users create a GitHub bug report for IDEasy.
 */
public class BugReportCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(BugReportCommandlet.class);

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public BugReportCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {

    return "bugreport";
  }

  @Override
  public boolean isIdeRootRequired() {

    return false;
  }

  @Override
  public boolean isWriteLogFile() {

    return false;
  }

  @Override
  protected void doRun() {

    if (!this.context.question("Do you want to create a bug report for IDEasy?")) {
      LOG.info("Bug report skipped.");
      return;
    }
    String title = this.context.askForInput("Please enter a short title for the bug report:");
    String description = this.context.askForInput("Please briefly describe the actual behavior / bug:");
    createBugReport(title, description, null);
  }

  /**
   * Offers creating a bug report after an unexpected error. Failures in this flow are logged and never rethrown so the original error remains the primary
   * outcome.
   *
   * @param context the {@link IdeContext}; may be {@code null}.
   * @param title the prefilled issue title.
   * @param error the unexpected error that triggered this offer.
   */
  public static void offerAfterError(IdeContext context, String title, Throwable error) {

    if (context == null || context.isBatchMode()) {
      return;
    }
    try {
      new BugReportCommandlet(context).offerAfterError(title, error);
    } catch (Throwable t) {
      LOG.debug("Bug report offer failed.", t);
    }
  }

  private void offerAfterError(String title, Throwable error) {

    if (!this.context.question("Do you want to create a bug report for this error?")) {
      LOG.info("You can later run 'ide bugreport' or open:\n{}", BugReportHelper.createIssueUrl(title));
      return;
    }
    String description = this.context.askForInput("Please briefly describe what you were doing when the error occurred:", title);
    createBugReport(title, description, error);
  }

  private void createBugReport(String title, String description, Throwable error) {

    String body = BugReportHelper.createIssueBody(this.context, description, toStackTrace(error));
    Path ghBinary = resolveGhBinary();
    if (ghBinary == null) {
      offerGhInstall();
      ghBinary = resolveGhBinary();
    }
    if (ghBinary != null && ensureGhAuthenticated(ghBinary)) {
      if (createIssueWithGh(ghBinary, title, body)) {
        return;
      }
      LOG.warn("Creating the issue via GitHub CLI failed. Falling back to the browser URL.");
    }
    openViaUrl(title, body);
  }

  /**
   * @return the {@link Path} to the {@code gh} binary if available on the {@link com.devonfw.tools.ide.common.SystemPath}, {@code null} otherwise.
   */
  protected Path resolveGhBinary() {

    Path binary = Path.of("gh");
    Path binaryPath = this.context.getPath().findBinary(binary);
    if ((binaryPath != binary) && Files.exists(binaryPath)) {
      return binaryPath;
    }
    return null;
  }

  private void offerGhInstall() {

    if (this.context.getIdeHome() == null) {
      LOG.info("GitHub CLI (gh) is not available. Install it later with 'ide install gh' inside an IDEasy project to report bugs via gh.");
      return;
    }
    if (this.context.question(
        "GitHub CLI (gh) is not available. Do you want to install it with IDEasy so you can report bugs via gh in the future?")) {
      getCommandlet(Gh.class).install(false);
    }
  }

  private boolean ensureGhAuthenticated(Path ghBinary) {

    if (isGhAuthenticated(ghBinary)) {
      return true;
    }
    if (!this.context.question("GitHub CLI is installed but you are not logged in. Do you want to run 'gh auth login' now?")) {
      return false;
    }
    this.context.newProcess().errorHandling(ProcessErrorHandling.NONE).executable(ghBinary).addArgs("auth", "login")
        .run(ProcessMode.DEFAULT);
    return isGhAuthenticated(ghBinary);
  }

  private boolean isGhAuthenticated(Path ghBinary) {

    ProcessResult result = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE).executable(ghBinary)
        .addArgs("auth", "status").run(ProcessMode.DEFAULT_CAPTURE);
    return result.isSuccessful();
  }

  private boolean createIssueWithGh(Path ghBinary, String title, String body) {

    ProcessResult result = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE).executable(ghBinary)
        .addArgs("issue", "create", "--repo", BugReportHelper.REPOSITORY, "--title", title, "--body", body)
        .run(ProcessMode.DEFAULT_CAPTURE);
    if (!result.isSuccessful()) {
      return false;
    }
    String issueUrl = result.getOut().stream().filter(line -> line.startsWith("http")).findFirst().orElse(null);
    if (issueUrl != null) {
      IdeLogLevel.SUCCESS.log(LOG, "Created bug report: {}", issueUrl);
    } else {
      IdeLogLevel.SUCCESS.log(LOG, "Created bug report via GitHub CLI.");
    }
    return true;
  }

  private void openViaUrl(String title, String body) {

    String url = BugReportHelper.createIssueUrl(title);
    IdeLogLevel.INTERACTION.log(LOG,
        "Please open the following URL to file the bug report and paste the prepared details into the form:\n{}", url);
    LOG.info("Prepared issue body:\n{}", body);
  }

  private static String toStackTrace(Throwable error) {

    if (error == null) {
      return null;
    }
    StringWriter writer = new StringWriter();
    error.printStackTrace(new PrintWriter(writer));
    return writer.toString();
  }
}
