package com.devonfw.tools.ide.tool.lazydocker;

import java.util.Set;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.docker.Docker;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} for <a href="https://github.com/jesseduffield/lazydocker">lazydocker</a>.
 */
public class LazyDocker extends LocalToolCommandlet {

  private static final VersionIdentifier MIN_API_VERSION = VersionIdentifier.of("1.25");
  private static final VersionIdentifier MIN_COMPOSE_VERSION = VersionIdentifier.of("1.23.2");

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public LazyDocker(IdeContext context) {

    super(context, "lazydocker", Set.of(Tag.DOCKER, Tag.ADMIN));
  }

  @Override
  protected void installDependencies() {

    // TODO create lazydocker/lazydocker/dependencies.json file in ide-urls and delete this method
    getCommandlet(Docker.class).install();
    // verify docker API version requirements
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE).executable("docker").addArg("version").addArg("--format")
        .addArg("'{{.Client.APIVersion}}'");
    ProcessResult result = pc.run(ProcessMode.DEFAULT_CAPTURE);
    verifyDockerVersion(result, MIN_API_VERSION, "docker API");

    // verify docker compose version requirements
    pc = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE).executable("docker-compose").addArg("version").addArg("--short");
    result = pc.run(ProcessMode.DEFAULT_CAPTURE);
    verifyDockerVersion(result, MIN_COMPOSE_VERSION, "docker-compose");
  }

  private void verifyDockerVersion(ProcessResult result, VersionIdentifier minimumVersion, String kind) {
    // we have this pattern a lot that we want to get a single line output of a successful ProcessResult.
    // we should create a generic method in ProcessResult for this use-case.
    if (!result.isSuccessful()) {
      result.log(IdeLogLevel.WARNING);
    }
    if (result.getOut().isEmpty()) {
      throw new CliException("Docker is not installed, but required for lazydocker.\n" //
          + "To install docker, call the following command:\n" //
          + "ide install docker");
    }
    VersionIdentifier installedVersion = VersionIdentifier.of(result.getOut().get(0).toString());
    if (installedVersion.isLess(minimumVersion)) {
      throw new CliException("The installed " + kind + " version is '" + installedVersion + "'.\n" + //
          "But lazydocker requires at least " + kind + " version '" + minimumVersion + "'.\n" //
          + "Please upgrade your installation of docker, before installing lazydocker.");
    }
  }

}
