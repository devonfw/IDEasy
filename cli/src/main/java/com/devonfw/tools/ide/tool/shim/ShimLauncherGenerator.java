package com.devonfw.tools.ide.tool.shim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Experimental launcher generator for the Node/npm shim activation spike.
 * <p>
 * These launchers deliberately delegate back into IDEasy instead of resolving a concrete tool binary themselves. This is not a production activation model.
 */
public class ShimLauncherGenerator {

  public Path generateWindowsShim(Path shimsDirectory, String toolName) throws IOException {

    Files.createDirectories(shimsDirectory);

    Path shim = shimsDirectory.resolve(toolName + ".cmd");
    String content = "@echo off\r\nide " + toolName + " %*\r\nexit /b %ERRORLEVEL%\r\n";

    Files.writeString(shim, content);

    return shim;
  }

  public Path generateShellShim(Path shimsDirectory, String toolName) throws IOException {

    Files.createDirectories(shimsDirectory);

    Path shim = shimsDirectory.resolve(toolName);
    String content = "#!/usr/bin/env bash\nexec ide " + toolName + " \"$@\"\n";

    Files.writeString(shim, content);
    shim.toFile().setExecutable(true, false);

    return shim;
  }
}
