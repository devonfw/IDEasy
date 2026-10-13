package com.devonfw.tools.ide.tool.gcloud;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.python.Python;

/**
 * {@link AbstractLocalToolCommandlet} for <a href="https://cloud.google.com/sdk/gcloud">Google Cloud CLI</a>.
 */
public class Gcloud extends AbstractLocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Gcloud.class);

  private static final String CONFIG_FOLDER = ".gcloud";

  private static final String ENV_CLOUDSDK_CONFIG = "CLOUDSDK_CONFIG";

  private static final String ENV_CLOUDSDK_PYTHON = "CLOUDSDK_PYTHON";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Gcloud(IdeContext context) {

    super(context, "gcloud", Set.of(Tag.CLOUD));
  }

  @Override
  protected void postInstallOnNewInstallation(ToolInstallRequest request) {

    super.postInstallOnNewInstallation(request);
    Path gcloudConfigDir = this.context.getConfPath().resolve(CONFIG_FOLDER);
    this.context.getFileAccess().mkdirs(gcloudConfigDir);
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }

  @Override
  public void printHelp(NlsBundle bundle) {

    LOG.info("To get detailed help about the usage of the Google Cloud CLI, use \"gcloud help\"");
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    Path gcloudConfigDir = this.context.getConfPath().resolve(CONFIG_FOLDER);
    environmentContext.withEnvVar(ENV_CLOUDSDK_CONFIG, gcloudConfigDir.toString());
    if (!hasBundledPython(toolInstallation.rootDir())) {
      Path pythonInterpreter = findIdePythonInterpreter();
      if (pythonInterpreter != null) {
        environmentContext.withEnvVar(ENV_CLOUDSDK_PYTHON, pythonInterpreter.toString());
      }
    }
  }

  /**
   * @param installationRoot the root directory of the gcloud installation.
   * @return {@code true} if the installation contains a bundled Python interpreter, {@code false} otherwise.
   */
  private boolean hasBundledPython(Path installationRoot) {

    if (installationRoot == null) {
      return false;
    }
    Path unixBundled = installationRoot.resolve("platform").resolve("bundledpythonunix").resolve("bin").resolve("python3");
    Path windowsBundled = installationRoot.resolve("platform").resolve("bundledpython").resolve("python.exe");
    return Files.isRegularFile(unixBundled) || Files.isRegularFile(windowsBundled);
  }

  private Path findIdePythonInterpreter() {

    Python python = this.context.getCommandletManager().getCommandlet(Python.class);
    Path pythonBinPath = python.getToolBinPath();
    if (pythonBinPath == null) {
      return null;
    }
    Path python3 = pythonBinPath.resolve("python3");
    if (Files.isRegularFile(python3)) {
      return python3;
    }
    Path pythonExe = pythonBinPath.resolve("python.exe");
    if (Files.isRegularFile(pythonExe)) {
      return pythonExe;
    }
    Path pythonBin = pythonBinPath.resolve("python");
    if (Files.isRegularFile(pythonBin)) {
      return pythonBin;
    }
    return null;
  }

}
