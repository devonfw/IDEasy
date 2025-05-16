package com.devonfw.ide.gui;

import java.nio.file.Path;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.io.IdeProgressBar;

/**
 * Implementation of {@link AbstractIdeContext} for the IDEasy dashbaord (GUI).
 */
public class IdeGuiContext extends AbstractIdeContext {


  /**
   * The constructor.
   *
   * @param startContext the {@link IdeStartContextImpl}.
   * @param workingDirectory the optional {@link Path} to current working directory.
   */
  public IdeGuiContext(IdeStartContextImpl startContext, Path workingDirectory) {
    super(startContext, workingDirectory);
  }

  @Override
  protected String readLine() {
    return "";
  }

  @Override
  public IdeProgressBar newProgressBar(String title, long size, String unitName, long unitSize) {
    return null;
  }
}
