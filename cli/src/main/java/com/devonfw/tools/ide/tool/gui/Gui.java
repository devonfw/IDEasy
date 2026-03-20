package com.devonfw.tools.ide.tool.gui;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
import com.devonfw.tools.ide.tool.mvn.MvnBasedLocalToolCommandlet;

/**
 * {@link MvnBasedLocalToolCommandlet} to launch the IDEasy GUI.
 */
public class Gui extends MvnBasedLocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Gui.class);

  private final ProcessContext pc = this.context.newProcess();

  private final Mvn mvn = this.context.getCommandletManager().getCommandlet(Mvn.class);
  private static final MvnArtifact ARTIFACT = MvnArtifact.ofIdeasy("ide-gui", "*!", "tar.gz", "${os}-${arch}");

  /**
   * @param context the {@link IdeContext}.
   */
  public Gui(IdeContext context) {

    super(context, "gui", ARTIFACT, Set.of(Tag.GUI));
  }

  @Override
  protected void doRun() {
    install(true);

    LOG.debug("Starting GUI via commandlet");

    List<String> args = List.of(
        "exec:java",
        "-Dexec.mainClass=com.devonfw.ide.gui.AppLauncher"
    );

    mvn.runTool(pc, ProcessMode.DEFAULT, args);
  }
}
