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

  /**
   * @return the {@link Path} where the tool is located (installed). Will be {@code null} for global tools that do not know the {@link Path} since it is
   *     determined by the installer.
   */
  @Override
  default Path getToolPath() {

    if (getContext().getSoftwarePath() == null) {
      return null;
    }
    return getContext().getSoftwarePath().resolve(getName());
  }

  /**
   * @return Bash completion command for this tool or {@code null} if this tool does not provide Bash completion.
   */
  default String getBashCompletion() {

    return null;
  }
}
