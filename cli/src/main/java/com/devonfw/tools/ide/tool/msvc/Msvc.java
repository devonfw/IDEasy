package com.devonfw.tools.ide.tool.msvc;

import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;

public class Msvc extends LocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Msvc.class);

  public Msvc(IdeContext context) {
    super(context, "msvc", Set.of(Tag.BUILD, Tag.CPP));
  }

  @Override
  protected boolean isExtract() {
    return false;
  }

  @Override
  public ToolInstallation installTool(ToolInstallRequest request) {

    if (this.context.getSystemInfo().isWindows()) {
      return super.installTool(request);
    }
    LOG.trace("Skipping msvc installation that is only available on Windows.");
    return createExistingToolInstallation(request);
  }

  @Override
  protected void installDownloadedToolPayload(ToolInstallRequest request, Path installationPath, Path installer) {

    this.context.getFileAccess().mkdirs(installationPath);

    this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI)
        .executable(installer)
        .withExitCodeAcceptor(code -> (code == 0) || (code == 3010))
        .addArgs("--installPath", installationPath.toString(),
            "--add", "Microsoft.VisualStudio.Workload.VCTools",
            "--quiet", "--wait", "--norestart", "--nocache")
        .run();
  }
}
