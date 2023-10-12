package com.devonfw.tools.ide.tool.vscode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://code.visualstudio.com/">vscode</a>.
 */
public class Vscode extends ToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Vscode (IdeContext context) {

    super(context, "vscode", Set.of(TAG_IDE));
  }

  @Override
  protected boolean isBinary(Path path) {
    String filename = path.getFileName().toString();
    if (filename.equals("code")) {
      return true;
    } else if (filename.startsWith("code")) {
      String suffix = filename.substring("code".length());
      return this.context.getSystemInfo().getOs().isExecutable(suffix);
    }
    return false;
  }

}
