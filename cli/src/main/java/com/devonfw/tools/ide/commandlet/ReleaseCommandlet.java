package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.tool.mvn.Mvn;

/**
 * {@link Commandlet} to build and deploy a release of the current project.
 */
public class ReleaseCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(ReleaseCommandlet.class);

  private static final String REVISION_FLAG = "-Drevision=";

  private static final String DEFAULT_MVN_RELEASE_OPTS = "clean deploy -Dchangelist= -Pdeploy";

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

    if (git.hasUntrackedFiles(projectPath)) {
      throw new CliException("Your local git repository has uncommitted changes. Please use 'git stash' and rerun on a clean repository.");
    }
    if (warnIfFork(git, projectPath)) {
      confirmWarning("You seem to work on a fork. Releases should be done on the original repository!\nWe strongly recommend to abort and rerun on original repository.");
    }
    if (!this.context.isForceMode() && !isTopLevelProject(projectPath)) {
      throw new CliException("Release has to be performed from the top-level project or using the force option (-f).");
    }

    String currentVersion = getProjectVersion(projectPath);
    String releaseVersion = currentVersion.replace("-SNAPSHOT", "");
    String nextVersion = getNextVersion(releaseVersion) + "-SNAPSHOT";

    LOG.info("Current version: {}", currentVersion);
    LOG.info("Release version: {}", releaseVersion);
    LOG.info("Next version: {}", nextVersion);

    while (true) {
      if (this.context.question("Is the next version '" + nextVersion + "' correct?")) {
        break;
      }
      nextVersion = this.context.askForInput("Please enter the next version:");
    }

    setProjectVersion(projectPath, releaseVersion);
    git.commit(projectPath, "#release: set release version " + releaseVersion);
    git.tag(projectPath, releaseVersion);

    buildAndDeploy();

    setProjectVersion(projectPath, nextVersion);
    git.commit(projectPath, "#release: set next snapshot version " + nextVersion);
    git.push(projectPath);

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

  private Path getMavenConfig(Path projectPath) {

    Path mavenConfig = projectPath.resolve(".mvn").resolve("maven.config");
    if (!Files.exists(mavenConfig)) {
      throw new CliException("Could not find the maven configuration at " + mavenConfig);
    }
    return mavenConfig;
  }

  private String getProjectVersion(Path projectPath) {

    Path mavenConfig = getMavenConfig(projectPath);
    String content = this.context.getFileAccess().readFileContent(mavenConfig);
    for (String token : content.split("\\s+")) {
      if (token.startsWith(REVISION_FLAG)) {
        return token.substring(REVISION_FLAG.length());
      }
    }
    throw new CliException("Could not find '" + REVISION_FLAG + "' in " + mavenConfig);
  }

  private String getNextVersion(String version) {

    int lastDot = version.lastIndexOf('.');
    String prefix = version.substring(0, lastDot + 1);
    String lastSegment = version.substring(lastDot + 1);
    String incrementedSegment = String.valueOf(Long.parseLong(lastSegment) + 1);
    incrementedSegment = "0".repeat(lastSegment.length() - incrementedSegment.length()) + incrementedSegment;
    return prefix + incrementedSegment;
  }

  private void setProjectVersion(Path projectPath, String version) {

    Path mavenConfig = getMavenConfig(projectPath);
    String content = this.context.getFileAccess().readFileContent(mavenConfig);
    if (!content.contains(REVISION_FLAG)) {
      throw new CliException("Could not find '" + REVISION_FLAG + "' in " + mavenConfig);
    }
    content = content.replaceAll(REVISION_FLAG + "\\S+", REVISION_FLAG + version);
    this.context.getFileAccess().writeFileContent(content, mavenConfig);
  }

  private void buildAndDeploy() {

    Mvn mvn = this.context.getCommandletManager().getCommandlet(Mvn.class);
    List<String> args = new ArrayList<>(getReleaseOptions());
    args.addAll(this.arguments.asList());
    while (true) {
      ProcessResult result = mvn.runTool(ProcessMode.DEFAULT, null, ProcessErrorHandling.NONE, args);
      if (result.isSuccessful()) {
        return;
      }
      LOG.error("Release build failed!");
      if (!this.context.question("Do you want to retry the release build?")) {
        throw new CliException("Release build failed and was aborted! The release commit and tag were already created locally - "
            + "undo them via 'git reset --hard HEAD~1' and 'git tag -d <version>' before retrying.");
      }
    }
  }

  private List<String> getReleaseOptions() {

    String options = this.context.getVariables().get("MVN_RELEASE_OPTS");
    if ((options == null) || options.isBlank()) {
      options = DEFAULT_MVN_RELEASE_OPTS;
    }
    return List.of(options.split("\\s+"));
  }

  private void confirmWarning(String message) {

    LOG.warn(message);
    this.context.askToContinue("Do you want to continue anyway?");
  }

}
