package com.devonfw.tools.ide.tool.go;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;

/**
 * {@link ToolCommandlet} for the Go programming language.
 */
public class Go extends LocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Go.class);

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Go(IdeContext context) {

    super(context, "go", Set.of(Tag.GO));
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }

  @Override
  protected void performToolInstallation(ToolInstallRequest request, Path installationPath) {
    super.performToolInstallation(request, installationPath);
    runGoBootstrapIfPresent(installationPath);
  }

  private void runGoBootstrapIfPresent(Path installationPath) {
    Path makeBash = installationPath.resolve("make.bash");
    Path workingDir = installationPath;
    if (!Files.isRegularFile(makeBash)) {
      workingDir = installationPath.resolve("src");
      makeBash = workingDir.resolve("make.bash");
    }
    if (!Files.isRegularFile(makeBash)) {
      LOG.debug("No make.bash found in {} or {} - skipping source bootstrap.", installationPath, installationPath.resolve("src"));
      return;
    }

    LOG.info("Running Go bootstrap script {}", makeBash);
    if (this.context.getSystemInfo().isWindows()) {
      Path bash = this.context.findBashRequired();
      this.context.newProcess().executable(bash).directory(workingDir).addArgs("./make.bash").run(ProcessMode.DEFAULT);
    } else {
      this.context.newProcess().executable(makeBash).directory(workingDir).run(ProcessMode.DEFAULT);
    }
  }

}
