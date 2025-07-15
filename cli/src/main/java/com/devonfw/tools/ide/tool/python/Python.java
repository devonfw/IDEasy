package com.devonfw.tools.ide.tool.python;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://www.python.org/">python</a>.
 */
public class Python extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Python(IdeContext context) {

    super(context, "python", Set.of(Tag.PYTHON));
  }

  @Override
  public void postInstall() {

    super.postInstall();
    
    // Only on Windows: create symlinks to make Scripts folder accessible via bin
    if (this.context.getSystemInfo().isWindows()) {
      Path toolPath = getToolPath();
      Path scriptsPath = toolPath.resolve("Scripts");
      
      // Check if Scripts folder exists (typical Python Windows installation)
      if (Files.isDirectory(scriptsPath)) {
        // Create bin folder as symlink to Scripts so SystemPath finds it
        Path binPath = toolPath.resolve("bin");
        this.context.getFileAccess().symlink(scriptsPath, binPath);
        
        // Create python.exe link in Scripts folder pointing to main python.exe
        Path mainPythonExe = toolPath.resolve("python.exe");
        if (Files.exists(mainPythonExe)) {
          Path scriptsPythonExe = scriptsPath.resolve("python.exe");
          if (!Files.exists(scriptsPythonExe)) {
            this.context.getFileAccess().symlink(Path.of("../python.exe"), scriptsPythonExe);
          }
        }
      }
    }
  }

  @Override
  protected boolean isIgnoreSoftwareRepo() {

    return true;
  }
}
