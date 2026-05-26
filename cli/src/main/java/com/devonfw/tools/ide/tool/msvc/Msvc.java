package com.devonfw.tools.ide.tool.msvc;

import java.nio.file.Path;
import java.util.Set;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.version.VersionIdentifier;

public class Msvc extends LocalToolCommandlet {
  private static final String MSVC_INSTALLER_URL = "https://aka.ms/vs/17/release/vs_BuildTools.exe";

  public Msvc(IdeContext context) {
    super(context, "msvc", Set.of());
  }

  @Override
  protected boolean isExtract() {
    return false; // Native .exe installer
  }

  @Override
  protected Path downloadTool(String edition, VersionIdentifier resolvedVersion) {
    Path tempFile = this.context.getFileAccess().createTempFile("msvc", ".exe");
    this.context.getFileAccess().download(MSVC_INSTALLER_URL, tempFile);
    return tempFile;
  }

  @Override
  protected void onInstall(ToolInstallRequest request, Path installationPath, Path installerScript) {
    this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI).executable(installerScript)
        .withExitCodeAcceptor(code -> (code == 0) || (code == 3010))
        .addArgs("--quiet", "--wait", "--norestart", "--nocache", "--add", "Microsoft.VisualStudio.Workload.VCTools")
        .run();
  }
}
