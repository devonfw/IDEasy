package com.devonfw.tools.ide.tool.gcviewer;

import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessContextImpl;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for GcViewer.
 */
public class GcViewer extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public GcViewer(IdeContext context) {

    super(context, "gcviewer", Set.of(TAG_JAVA));
  }
  @Override
  protected boolean isExtract() {

    return false;
  }

  @Override
  public void run() {

    install(true);
    ProcessContext pc = this.context.newProcess();
    pc.directory(getToolPath());
    pc.executable("java");
    pc.addArg("-jar");
    pc.addArg("gcviewer-" + getInstalledVersion() + ".jar");
    pc.run();
  }
}