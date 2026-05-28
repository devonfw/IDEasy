package com.devonfw.tools.ide.tool.msvc;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;

public class Msvc extends LocalToolCommandlet {

  private static final String MSVC_SETUP_URL = "https://aka.ms/vs/17/release/vs_BuildTools.exe";

  public Msvc(IdeContext context) {
    super(context, "msvc", Set.of(Tag.BUILD, Tag.CPP));
  }

  @Override
  protected void performToolInstallation(ToolInstallRequest request, Path installationPath) {

    if (!this.context.getSystemInfo().isWindows()) {
      throw new CliException("The tool 'msvc' is only available on Windows.");
    }

    this.context.getFileAccess().mkdirs(installationPath);

    Path installer = this.context.getDownloadPath().resolve("vs_BuildTools.exe");
    this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI)
        .executable("curl.exe")
        .addArgs("-fSL", "-o", installer.toString(), MSVC_SETUP_URL)
        .run();

    this.context.writeVersionFile(request.getRequested().getResolvedVersion(), installationPath);

    this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI)
        .executable(installer)
        .withExitCodeAcceptor(code -> (code == 0) || (code == 3010))
        .addArgs("--installPath", installationPath.toString(),
            "--add", "Microsoft.VisualStudio.Workload.VCTools",
            "--quiet", "--wait", "--norestart", "--nocache")
        .run();
  }
}
