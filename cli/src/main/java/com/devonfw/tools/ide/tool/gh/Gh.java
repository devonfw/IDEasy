package com.devonfw.tools.ide.tool.gh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;

/**
 * {@link AbstractToolCommandlet} for github CLI (gh).
 */
public class Gh extends AbstractLocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Gh.class);

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Gh(IdeContext context) {

    super(context, "gh", Set.of(Tag.CLOUD));
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }

  /**
   * @return the {@link Path} to the {@code gh} binary if it is available, otherwise {@code null}.
   */
  public Path getGhBinary() {

    return getBinaryExecutable();
  }

  /**
   * @return {@code true} if the user is logged in with the GitHub CLI (verified via {@code gh auth status}).
   */
  public boolean isGhAuthenticated() {

    Path binary = getGhBinary();
    if (binary == null) {
      return false;
    }
    ProcessResult result = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE).executable(binary)
        .addArgs("auth", "status").run(ProcessMode.DEFAULT_CAPTURE);
    return result.isSuccessful();
  }

  /**
   * Makes sure the user is logged in with the GitHub CLI. If not and {@code interactive} is {@code true} the user is asked whether to run
   * {@code gh auth login} now.
   *
   * @param interactive whether the user may be prompted to authenticate.
   * @return {@code true} if the user is authenticated after this call, otherwise {@code false}.
   */
  public boolean ensureGhAuthenticated(boolean interactive) {

    if (isGhAuthenticated()) {
      return true;
    }
    Path binary = getGhBinary();
    if (binary == null || !interactive) {
      return false;
    }
    if (!this.context.question("GitHub CLI is installed but you are not logged in. Do you want to run 'gh auth login' now?")) {
      return false;
    }
    this.context.newProcess().errorHandling(ProcessErrorHandling.NONE).executable(binary).addArgs("auth", "login").run(ProcessMode.DEFAULT);
    return isGhAuthenticated();
  }

  /**
   * Offers installing the GitHub CLI so it can be used to report bugs in the future. No-op if the tool is already installed or there is no
   * {@link IdeContext#getIdeHome() IDE_HOME} to install into.
   */
  public void offerGhInstall() {

    if (getGhBinary() != null) {
      return;
    }
    if (this.context.getIdeHome() == null) {
      LOG.info("GitHub CLI (gh) is not available. Install it later with 'ide install gh' inside an IDEasy project to report bugs via gh.");
      return;
    }
    if (this.context.question("GitHub CLI (gh) is not available. Do you want to install it with IDEasy so you can report bugs via gh in the future?")) {
      install(false);
    }
  }

  /**
   * Creates a GitHub issue via the GitHub CLI using {@code --body-file} so that long issue bodies (e.g. a full stacktrace) are not limited by the
   * operating system command-line length.
   *
   * @param repo the {@code «organization»/«repository»} to file the issue in.
   * @param title the issue title.
   * @param body the (potentially long) issue body.
   * @param type the issue type (e.g. {@code Bug}); may be {@code null} to omit.
   * @return the URL of the created issue as printed by {@code gh issue create}, or {@code null} if the issue could not be created.
   */
  public String createIssue(String repo, String title, String body, String type) {

    Path binary = getGhBinary();
    if (binary == null) {
      return null;
    }
    Path bodyFile = null;
    try {
      bodyFile = Files.createTempFile("ide-bug-report-", ".md");
      Files.writeString(bodyFile, body, StandardOpenOption.TRUNCATE_EXISTING);
      List<String> args = new ArrayList<>();
      args.add("issue");
      args.add("create");
      args.add("--repo");
      args.add(repo);
      args.add("--title");
      args.add(title);
      args.add("--body-file");
      args.add(bodyFile.toString());
      if (type != null) {
        args.add("--type");
        args.add(type);
      }
      ProcessResult result = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE).executable(binary).addArgs(args)
          .run(ProcessMode.DEFAULT_CAPTURE);
      if (!result.isSuccessful()) {
        return null;
      }
      return result.getOut().stream().filter(line -> line.startsWith("http")).findFirst().orElse(null);
    } catch (RuntimeException | IOException e) {
      LOG.warn("Creating the issue via GitHub CLI failed.", e);
      return null;
    } finally {
      if (bodyFile != null) {
        try {
          Files.deleteIfExists(bodyFile);
        } catch (IOException e) {
          LOG.debug("Failed to delete temporary issue body file {}.", bodyFile, e);
        }
      }
    }
  }
}
