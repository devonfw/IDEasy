package com.devonfw.tools.ide.tool.uv;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
 * {@link ToolCommandlet} for <a href="https://docs.astral.sh/uv/">uv</a>.
 */
public class Uv extends LocalToolCommandlet {


  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Uv(IdeContext context) {

    super(context, "uv", Set.of(Tag.PYTHON));
  }

  /**
   * Installs a specified version of {@code Python} in the given directory using the {@code uv} environment manager.
   *
   * @param installationPath the target {@link Path} where {@code Python} should be installed
   * @param resolvedVersion the {@link VersionIdentifier} of the {@code Python} version to install
   * @param processContext the {@link ProcessContext} used to execute the {@code uv} command
   */
  public void installPython(Path installationPath, VersionIdentifier resolvedVersion, ProcessContext processContext) {

    processContext.directory(installationPath);

    List<String> uvInstallCommands = new ArrayList<>();
    uvInstallCommands.add("venv");
    uvInstallCommands.add("--python");
    uvInstallCommands.add(resolvedVersion.toString());

    ProcessResult result = runTool(ProcessMode.DEFAULT_CAPTURE, ProcessErrorHandling.THROW_ERR, processContext, uvInstallCommands.toArray(String[]::new));
    assert result.isSuccessful();
  }
}
