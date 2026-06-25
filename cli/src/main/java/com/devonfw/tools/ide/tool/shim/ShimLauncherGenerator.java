package com.devonfw.tools.ide.tool.shim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ShimLauncherGenerator {

  public Path generateWindowsShim(Path shimsDirectory, String toolName) throws IOException {

    Files.createDirectories(shimsDirectory);

    Path shim = shimsDirectory.resolve(toolName + ".cmd");
    String content = "@echo off\r\nide " + toolName + " %*\r\n";

    Files.writeString(shim, content);

    return shim;
  }
}
