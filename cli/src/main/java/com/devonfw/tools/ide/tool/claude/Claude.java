package com.devonfw.tools.ide.tool.claude;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;

/**
 * {@link ToolCommandlet} for <a href="https://github.com/anthropics/claude-code">Claude Code CLI</a>.
 */
public class Claude extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Claude(IdeContext context) {

    super(context, "claude", Set.of(Tag.ARTIFICIAL_INTELLIGENCE));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }

  @Override
  protected void postInstallOnNewInstallation(ToolInstallRequest request) {

    super.postInstallOnNewInstallation(request);
    Path claudeBinary = getBinaryExecutable();
    if (claudeBinary != null) {
      Path localBinDir = this.context.getUserHome().resolve(".local").resolve("bin");
      this.context.getFileAccess().mkdirs(localBinDir);
      Path targetBinary = localBinDir.resolve(claudeBinary.getFileName());
      try {
        this.context.getFileAccess().symlink(claudeBinary, targetBinary, false);
      } catch (Exception e) {
        this.context.getFileAccess().copy(claudeBinary, targetBinary, FileCopyMode.COPY_FILE_OVERRIDE);
      }
    }
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    environmentContext.withEnvVar("DISABLE_AUTOUPDATER", "1");
  }
}
