package com.devonfw.tools.ide.tool.gui;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
import com.devonfw.tools.ide.tool.mvn.MvnBasedLocalToolCommandlet;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * {@link MvnBasedLocalToolCommandlet} to launch the IDEasy GUI.
 */
public class Gui extends MvnBasedLocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Gui.class);

  private static final MvnArtifact ARTIFACT = new MvnArtifact("ide-gui", "2026.04.001", "jar", "");

  private Java java;

  /**
   * @param context the {@link IdeContext}.
   */
  public Gui(IdeContext context) {

    super(context, "gui", ARTIFACT, Set.of(Tag.GUI));
  }

  @Override
  protected void installToolDependencies(ToolInstallRequest request) {

    super.installToolDependencies(request);
    LOG.info(ARTIFACT.getDownloadUrl());
    LOG.info("Installing ide-gui dependencies: {}", request.getRequested().getVersion().toString());

    java = this.context.getCommandletManager().getCommandlet(Java.class);
    java.installAsDependency(VersionRange.of("25"), request);
  }

  @Override
  protected boolean isExtract() {

    //return false to avoid extracting the downloaded GUI jar (as we will run the jar in doRun())
    return false;
  }

  @Override
  protected void doRun() {

    install(true);

    LOG.debug("Starting GUI via commandlet");

    Path jarPath = getToolPath().resolve(ARTIFACT.getFilename());

    List<String> args = List.of(
        "-jar",
        jarPath.toAbsolutePath().toString()
    );

    java.runTool(context.newProcess(), ProcessMode.DEFAULT, args);
  }
}
