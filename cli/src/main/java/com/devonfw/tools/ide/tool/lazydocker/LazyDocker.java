package com.devonfw.tools.ide.tool.lazydocker;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.util.Set;

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
      this.context.info("Docker is not installed, but required for lazydocker");
      this.context.info("To install docker, call the following command");
      this.context.info("ide install docker");
      return false;
    }

    VersionIdentifier dockerAPIversion = VersionIdentifier.of(result.getOut().get(0).toString());
    this.context.debug("Found docker API version:" + dockerAPIversion.toString());

    if (dockerAPIversion.compareVersion(VersionIdentifier.of("1.25")).isLess()) {
      this.context.info("The installed version of docker does not meet the requirements of lazydocker");
      this.context.info("Please upgrade your installation of docker");
      return false;
    }

    return super.doInstall(silent);
  }
}
