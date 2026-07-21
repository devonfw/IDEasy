package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link Commandlet} to build and deploy a release of the current project.
 */
public class ReleaseCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(ReleaseCommandlet.class);

  public final StringProperty arguments;

  public ReleaseCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.arguments = add(new StringProperty("", false, true, "args"));
  }

  @Override
  public String getName() {

    return "release";
  }

  @Override
  protected void doRun() {

    Path projectPath = this.context.getCwd();
    GitContext git = this.context.getGitContext();
    Mvn buildTool = this.context.getCommandletManager().getCommandlet(Mvn.class);

    if (git.hasUntrackedFiles(projectPath)) {
      throw new CliException("Your local git repository has uncommitted changes. Please use 'git stash' and rerun on clean repo.");
    }
    if (warnIfFork(git, projectPath)) {
      confirmWarning("You seem to work on a fork. Releases should be done on the original repository!\nWe strongly recommend to abort and rerun on original repository.");
    }
    if (!this.context.isForceMode() && !isTopLevelProject(projectPath)) {
      throw new CliException("Release has to be performed from the top-level project or using force option.");
    }

    VersionIdentifier currentVersion = buildTool.getProjectVersion(projectPath);
    VersionIdentifier releaseVersion = VersionIdentifier.of(currentVersion.toString().replace("-SNAPSHOT", ""));
    VersionIdentifier nextVersion = VersionIdentifier.of(releaseVersion.incrementLastDigit(false) + "-SNAPSHOT");

    LOG.info("Current version: {}", currentVersion);
    LOG.info("Release version: {}", releaseVersion);
    LOG.info("Next version: {}", nextVersion);

    while (true) {
      if (this.context.question("Is the next version '" + nextVersion + "' correct?")) {
        break;
      }
      nextVersion = VersionIdentifier.of(this.context.askForInput("Please enter the next version:"));
    }

    buildTool.setProjectVersion(projectPath, releaseVersion);
    git.commit(projectPath, "set release version to " + releaseVersion, true);
    git.tag(projectPath, "release/" + releaseVersion, "tagged version " + releaseVersion);

    buildAndDeploy(buildTool);

    buildTool.setProjectVersion(projectPath, nextVersion);
    git.commit(projectPath, "set next version to " + nextVersion, true);
    LOG.info("Local commits and tag need to be pushed now.\nYou now have the chance to review these changes manually before they are pushed.");
    this.context.askToContinue("Do you want to continue?");
    git.push(projectPath, true);

    LOG.info("Successfully released version {}.", releaseVersion);
  }

  private boolean warnIfFork(GitContext git, Path projectPath) {

    String user = System.getProperty("user.name");
    for (String remote : git.retrieveGitRemotes(projectPath)) {
      if (remote.contains("upstream") || ((user != null) && remote.contains("github.com/" + user))) {
        return true;
      }
    }
    return false;
  }

  private boolean isTopLevelProject(Path projectPath) {

    // returns false in case there's no pom.xml present or if parent directory has a pom.xml
    return Files.exists(projectPath.resolve("pom.xml"))
        && !Files.exists(projectPath.getParent().resolve("pom.xml"));
  }

  private void buildAndDeploy(Mvn buildTool) {

    while (true) {
      ProcessResult result = buildTool.buildAndDeploy(this.arguments.asList());
      if (result.isSuccessful()) {
        return;
      }
      LOG.error("Release build failed!");
      if (!this.context.question("Do you want to retry the build (e.g. in case of a temporary network error)?")) {
        throw new CliException("Release build failed and process aborted!\nYou should reset your local commits via 'git reset HEAD^'.");
      }
    }
  }

  private void confirmWarning(String message) {

    LOG.warn(message);
    this.context.askToContinue("Do you want to continue anyway?");
  }

}
