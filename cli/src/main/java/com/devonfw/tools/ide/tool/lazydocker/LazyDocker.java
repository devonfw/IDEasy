package com.devonfw.tools.ide.tool.lazydocker;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} for <a href="https://github.com/jesseduffield/lazydocker">lazydocker</a>.
 */
public class LazyDocker extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public LazyDocker(IdeContext context) {

    super(context, "lazydocker", Set.of(Tag.DOCKER));
  }

  @Override
  public boolean install(boolean silent) {

    String bashPath = this.context.findBash();
    String command = "docker version --format '{{.Client.APIVersion}}'";
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE).executable(bashPath)
        .addArgs("-c", command);
    ProcessResult result = pc.run(ProcessMode.DEFAULT_CAPTURE);

    if (result.getOut().isEmpty()) {
      this.context.info("Docker is not installed, but required for lazydocker.");
      this.context.info("To install docker, call the following command:");
      this.context.info("ide install docker");
      return false;
    }

    VersionIdentifier dockerAPIversion = VersionIdentifier.of(result.getOut().get(0).toString());

    if (dockerAPIversion.compareVersion(VersionIdentifier.of("1.25")).isLess()) {
      this.context.info("The installed version of docker does not meet the requirements of lazydocker.");
      this.context.info("Please upgrade your installation of docker, before installing lazydocker.");
      return false;
    }

    command = "docker-compose version --short";
    pc = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE).executable(bashPath).addArgs("-c", command);
    result = pc.run(ProcessMode.DEFAULT_CAPTURE);

    if (!result.getOut().isEmpty()) {
      VersionIdentifier dockercomposeversion = VersionIdentifier.of(result.getOut().get(0).toString());

      if (dockercomposeversion.compareVersion(VersionIdentifier.of("1.23.2")).isLess()) {
        this.context.info("Found docker-compose version:" + dockercomposeversion);
        this.context.info(
            "If you want to use docker-compose with lazydocker, then you'll need at least version 1.23.2 of docker-compose");
      }
    }

    return super.doInstall(silent);
  }
}
