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
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.gradle.Gradle;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.npm.Npm;
import com.devonfw.tools.ide.tool.yarn.Yarn;

/**
 * {@link Commandlet} to perform a release of the current project. See <a href="https://github.com/devonfw/IDEasy/issues/1594">#1594</a>.
 */
public class ReleaseCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(ReleaseCommandlet.class);

  private static final List<Class<? extends LocalToolCommandlet>> BUILD_TOOLS = List.of(Mvn.class, Gradle.class, Yarn.class, Npm.class);

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
      throw new CliException("Your project has uncommitted or untracked changes. Please commit or revert them before releasing.");
    }

    String releaseVersion = this.context.askForInput("Enter the release version to publish (e.g. 1.5.0):");
    String nextVersion = getNextSnapshotVersion(releaseVersion);
    this.context.askToContinue("About to release version {} and then continue development on {}. Proceed?", releaseVersion, nextVersion);

    setProjectVersion(projectPath, releaseVersion);
    git.commit(projectPath, "release/" + releaseVersion);
    git.tag(projectPath, "release/" + releaseVersion);

    buildAndDeploy(projectPath);

    setProjectVersion(projectPath, nextVersion);
    git.commit(projectPath, "set next development version " + nextVersion);
    git.push(projectPath);

    LOG.info("Successfully released version {}.", releaseVersion);
  }

  private String getNextSnapshotVersion(String releaseVersion) {

    int lastDot = releaseVersion.lastIndexOf('.');
    String prefix = releaseVersion.substring(0, lastDot + 1);
    int lastNumber = Integer.parseInt(releaseVersion.substring(lastDot + 1));
    return prefix + (lastNumber + 1) + "-SNAPSHOT";
  }

  private void setProjectVersion(Path projectPath, String version) {

    Mvn mvn = getCommandlet(Mvn.class);
    mvn.runTool(List.of("versions:set", "-DnewVersion=" + version, "-DgenerateBackupPoms=false"));
  }

  private void buildAndDeploy(Path projectPath) {

    for (Class<? extends LocalToolCommandlet> toolClass : BUILD_TOOLS) {
      LocalToolCommandlet tool = getCommandlet(toolClass);
      if (tool.findBuildDescriptor(projectPath) != null) {
        String releaseOptsVariable = tool.getName().toUpperCase(Locale.ROOT) + "_RELEASE_OPTS";
        List<String> args = getReleaseOptions(releaseOptsVariable);
        args.addAll(this.arguments.asList());
        tool.runTool(args);
        return;
      }
    }
    throw new CliException("Could not find a build descriptor - no pom.xml, build.gradle, or package.json found!");
  }

  private List<String> getReleaseOptions(String releaseOptsVariable) {

    String value = this.context.getVariables().get(releaseOptsVariable);
    if ((value == null) || value.isBlank()) {
      return new ArrayList<>();
    }
    return new ArrayList<>(List.of(value.split(" ")));
  }
}
