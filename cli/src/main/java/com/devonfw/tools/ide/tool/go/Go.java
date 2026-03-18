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
    Path makeScript = findGoBootstrapScript(installationPath);
    if (makeScript == null) {
      LOG.debug("No Go bootstrap script found in {} or {} - skipping source bootstrap.", installationPath,
          installationPath.resolve("src"));
      return;
    }
    runGoBootstrapScript(makeScript, makeScript.getParent());
  }

  private Path findGoBootstrapScript(Path installationPath) {

    Path script = findGoBootstrapScript(installationPath, "make.bash");
    if ((script == null) && this.context.getSystemInfo().isWindows()) {
      script = findGoBootstrapScript(installationPath, "make.bat");
    }
    return script;
  }

  private Path findGoBootstrapScript(Path installationPath, String fileName) {

    Path rootScript = installationPath.resolve(fileName);
    if (Files.isRegularFile(rootScript)) {
      return rootScript;
    }
    Path srcScript = installationPath.resolve("src").resolve(fileName);
    if (Files.isRegularFile(srcScript)) {
      return srcScript;
    }
    return null;
  }

  protected void runGoBootstrapScript(Path makeScript, Path workingDir) {

    LOG.info("Running Go bootstrap script {}", makeScript);
    String scriptName = makeScript.getFileName().toString();
    if ("make.bat".equals(scriptName)) {
      this.context.newProcess().executable(makeScript).directory(workingDir).run(ProcessMode.DEFAULT);
    } else if (this.context.getSystemInfo().isWindows()) {
      Path bash = this.context.findBashRequired();
      this.context.newProcess().executable(bash).directory(workingDir).addArgs("./make.bash").run(ProcessMode.DEFAULT);
    } else {
      this.context.newProcess().executable(makeScript).directory(workingDir).run(ProcessMode.DEFAULT);
    }
  }

}
