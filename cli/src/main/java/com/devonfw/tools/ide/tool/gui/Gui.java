package com.devonfw.tools.ide.tool.gui;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessContextImpl;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ToolEdition;
import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.BoundaryType;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * {@link Commandlet} to launch the IDEasy GUI.
 */
public class Gui extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Gui.class);

  //TODO: implement constant in sync with maven property gui.relative_pom_path
  private static final String POM_PATH = "gui-execution";

  /**
   * @param context the {@link IdeContext}.
   */
  public Gui(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {

    return "gui";
  }

  @Override
  protected void doRun() {

    ProcessContext processContext = new ProcessContextImpl(this.context);

    ToolInstallRequest toolInstallRequest = new ToolInstallRequest(true);
    toolInstallRequest.setProcessContext(processContext);
    toolInstallRequest.setRequested(
        new ToolEditionAndVersion(
            new ToolEdition("java", "25"),
            VersionRange.of(VersionIdentifier.of("25"), VersionIdentifier.of(""), BoundaryType.RIGHT_OPEN)
        )
    );

    Java java = this.context.getCommandletManager().getCommandlet(Java.class);
    java.installAsDependency(
        (VersionRange) toolInstallRequest.getRequested().getVersion(),
        toolInstallRequest
    );

    LOG.debug("Starting GUI via commandlet");

    Mvn mvn = context.getCommandletManager().getCommandlet(Mvn.class);

    List<String> args = List.of(
        "-f",
        Path.of(IdeVariables.IDE_ROOT.get(context).toString(), "_ide", "installation", POM_PATH, "pom.xml").toString(),
        "exec:java",
        "-Dexec.mainClass=com.devonfw.ide.gui.AppLauncher"
    );

    try {
      mvn.runTool(processContext, ProcessMode.DEFAULT, args);
    } catch (IllegalStateException e) {
      LOG.error("ERROR WHILE LAUNCHING GUI: Recommended to check if POM file exists in _ide/installation/gui-execution/pom.xml", e);
    }
  }
}
