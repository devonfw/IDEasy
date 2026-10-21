package com.devonfw.tools.ide.tool;

import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link ToolCommandlet} that is installed locally (inside {@code $IDE_HOME/tools}) as opposed to globally (e.g. via the OS package manager).
 */
public interface LocalToolCommandlet extends ToolCommandlet {

  /**
   * @return the {@link Path} where the executables of the tool can be found. Typically, a "bin" folder inside {@link #getToolPath() tool path}.
   */
  default Path getToolBinPath() {

    Path toolPath = getToolPath();
    if (toolPath == null) {
      return null;
    }
    Path binPath = toolPath.resolve(IdeContext.FOLDER_BIN);
    if (Files.isDirectory(binPath)) {
      return binPath;
    }
    return toolPath;
  }
}
