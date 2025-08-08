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
import com.devonfw.tools.ide.version.VersionIdentifier;

public class Uv extends LocalToolCommandlet {


  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Uv(IdeContext context) {

    super(context, "uv", Set.of(Tag.PYTHON));
  }

  public void installPython(Path installationPath, VersionIdentifier resolvedVersion, ProcessContext processContext) {

    processContext.directory(installationPath);

    List<String> uvInstallCommands = new ArrayList<>();
    uvInstallCommands.add("venv");
    uvInstallCommands.add("--python");
    uvInstallCommands.add(resolvedVersion.toString());

    ProcessResult result = runTool(ProcessMode.DEFAULT_CAPTURE, ProcessErrorHandling.THROW_ERR, processContext, uvInstallCommands.toArray(String[]::new));

    if (result.isSuccessful()) {
      this.context.success("Successfully installed and created virtual environment for Python version: {}", resolvedVersion);
    } else {
      this.context.warning("Failed to install and create virtual environment for Python version: {}", resolvedVersion);
    }
  }

}
