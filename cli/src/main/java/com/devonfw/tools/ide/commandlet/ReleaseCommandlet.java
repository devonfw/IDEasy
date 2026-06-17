package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.gradle.Gradle;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.npm.Npm;
import com.devonfw.tools.ide.tool.yarn.Yarn;

/**
 * {@link Commandlet} to build and deploy a release of the current project. See
 * <a href="https://github.com/devonfw/IDEasy/issues/1594">#1594</a>.
 */
public class ReleaseCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(ReleaseCommandlet.class);

  private static final List<Class<? extends LocalToolCommandlet>> BUILD_TOOLS = List.of(Mvn.class, Gradle.class, Yarn.class, Npm.class);

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
    warnIfFork(git, projectPath);
    if (!this.context.isForceMode() && !isTopLevelProject(projectPath)) {
      throw new CliException("Release has to be performed from the top-level project or using the force option (-f).");
    }

    String currentVersion = getProjectVersion();
    LOG.info("Current version of your project is {}.", currentVersion);
    boolean devVersion = isDevVersion(currentVersion);
    String releaseVersion;
    String nextVersion;
    if (devVersion) {
      LOG.info("Current version is {} so we assume the version is not tracked in your version control.", currentVersion);
      LOG.info("Existing tags are:");
      for (String tag : git.retrieveGitTags(projectPath)) {
        LOG.info(tag);
      }
      releaseVersion = this.context.askForInput("Please enter the release version:");
      nextVersion = currentVersion;
    } else {
      releaseVersion = currentVersion.replace("-SNAPSHOT", "");
      nextVersion = getNextVersion(releaseVersion);
    }
    if (currentVersion.equals(releaseVersion + "-SNAPSHOT")) {
      nextVersion = nextVersion + "-SNAPSHOT";
    }
    if (currentVersion.equals(releaseVersion)) {
      confirmWarning("Current version is not a SNAPSHOT version!");
    }
    nextVersion = confirmNextVersion(devVersion, currentVersion, releaseVersion, nextVersion);

    if (!devVersion) {
      setProjectVersion(releaseVersion);
      git.commit(projectPath, "set release version to " + releaseVersion);
    }
    git.tag(projectPath, "release/" + releaseVersion);

    buildAndDeploy(projectPath, devVersion ? releaseVersion : null);

    if (devVersion) {
      LOG.info("Local tag needs to be pushed now. You now have the chance to review these changes manually before they are pushed.");
    } else {
      setProjectVersion(nextVersion);
      git.commit(projectPath, "set next version to " + nextVersion);
      LOG.info("Local commits and tag need to be pushed now. You now have the chance to review these changes manually before they are pushed.");
    }

    this.context.askToContinue("Do you want to push the changes and the tag now?");
    git.push(projectPath);
    LOG.info("Successfully released version {}.", releaseVersion);
  }

  private String confirmNextVersion(boolean devVersion, String currentVersion, String releaseVersion, String nextVersion) {

    while (true) {
      LOG.info("Current version: {}", currentVersion);
      LOG.info("Release version: {}", releaseVersion);
      LOG.info("Next version: {}", nextVersion);
      if (!nextVersion.endsWith("-SNAPSHOT")) {
        LOG.warn("Next version is not a SNAPSHOT version!");
      }
      if (devVersion) {
        return nextVersion;
      }
      if (this.context.question("Is the next version correct?")) {
        return nextVersion;
      }
      nextVersion = this.context.askForInput("Please enter the next version:");
    }
  }

  private void warnIfFork(GitContext git, Path projectPath) {

    String user = System.getProperty("user.name");
    for (String remote : git.retrieveGitRemotes(projectPath)) {
      if (remote.contains("upstream") || ((user != null) && remote.contains("github.com/" + user))) {
        confirmWarning("You seem to work on a fork. Releases should be done on the original repository! We strongly recommend to abort and rerun on the original repository.");
        return;
      }
    }
  }

  private boolean isTopLevelProject(Path projectPath) {

    Path parent = projectPath.getParent();
    if (parent == null) {
      return true;
    }
    for (Class<? extends LocalToolCommandlet> toolClass : BUILD_TOOLS) {
      if (getCommandlet(toolClass).findBuildDescriptor(parent) != null) {
        return false;
      }
    }
    return true;
  }

  private void confirmWarning(String message) {

    LOG.warn(message);
    this.context.askToContinue("Do you want to continue anyway?");
  }

  private boolean isDevVersion(String version) {

    return "dev-SNAPSHOT".equals(version) || "0-SNAPSHOT".equals(version);
  }

  private String getProjectVersion() {

    Mvn mvn = getCommandlet(Mvn.class);
    ProcessResult result = mvn.runTool(ProcessMode.DEFAULT_CAPTURE, null, ProcessErrorHandling.NONE,
        List.of("help:evaluate", "-Dexpression=project.version", "-q", "-DforceStdout"));
    List<String> out = result.getOut();
    if (!result.isSuccessful() || out.isEmpty()) {
      throw new CliException("Failed to determine the current version of your project. You need to run 'mvn install' before.");
    }
    return out.get(out.size() - 1).trim();
  }

  private void setProjectVersion(String version) {

    Mvn mvn = getCommandlet(Mvn.class);
    mvn.runTool(List.of("versions:set", "-DnewVersion=" + version, "-DgenerateBackupPoms=false"));
  }

  private void buildAndDeploy(Path projectPath, String revision) {

    LocalToolCommandlet tool = null;
    for (Class<? extends LocalToolCommandlet> toolClass : BUILD_TOOLS) {
      LocalToolCommandlet candidate = getCommandlet(toolClass);
      if (candidate.findBuildDescriptor(projectPath) != null) {
        tool = candidate;
        break;
      }
    }
    if (tool == null) {
      throw new CliException("Could not find a build descriptor - no pom.xml, build.gradle, or package.json found!");
    }
    String releaseOptsVariable = tool.getName().toUpperCase(Locale.ROOT) + "_RELEASE_OPTS";
    while (true) {
      List<String> args = new ArrayList<>();
      if (revision != null) {
        args.add("-Drevision=" + revision);
      }
      args.addAll(getReleaseOptions(releaseOptsVariable, tool));
      args.addAll(this.arguments.asList());
      try {
        tool.runTool(args);
        return;
      } catch (CliException e) {
        LOG.error("Release build failed!");
        if (!this.context.question("Do you want to retry the build (e.g. in case of a temporary network error)?")) {
          throw new CliException("Release build failed and the process was aborted! You should reset your local commits via 'git reset HEAD^'.");
        }
      }
    }
  }

  private List<String> getReleaseOptions(String releaseOptsVariable, LocalToolCommandlet tool) {

    String value = this.context.getVariables().get(releaseOptsVariable);
    if ((value == null) || value.isBlank()) {
      if (tool instanceof Mvn) {
        value = DEFAULT_MVN_RELEASE_OPTS;
      } else {
        return new ArrayList<>();
      }
    }
    return new ArrayList<>(List.of(value.split(" ")));
  }

  private String getNextVersion(String version) {

    int end = version.length();
    while ((end > 0) && !Character.isDigit(version.charAt(end - 1))) {
      end--;
    }
    int start = end;
    while ((start > 0) && Character.isDigit(version.charAt(start - 1))) {
      start--;
    }
    if (start == end) {
      return version;
    }
    String prefix = version.substring(0, start);
    String number = version.substring(start, end);
    String suffix = version.substring(end);
    String next = String.valueOf(Long.parseLong(number) + 1);
    if (number.length() > next.length()) {
      next = number.substring(0, number.length() - next.length()) + next;
    }
    return prefix + next + suffix;
  }
}
