package com.devonfw.tools.ide.tool.gcviewer;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;

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

    super(context, "gcviewer", Set.of(Tag.JAVA));
  }

  @Override
  protected boolean isExtract() {

    return false;
  }

  @Override
  public void run() {

    getCommandlet(Java.class).install();
    install(true);
    ProcessContext pc = this.context.newProcess();
    pc.directory(getToolPath());
    pc.executable("java");
    pc.addArg("-jar");
    pc.addArg("gcviewer-" + getInstalledVersion() + ".jar");
    pc.run(ProcessMode.BACKGROUND_SILENT);
  }
}
