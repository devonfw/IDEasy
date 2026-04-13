package com.devonfw.tools.ide.tool.gui;

import java.nio.file.Files;
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
import com.devonfw.tools.ide.version.BoundaryType;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * {@link Commandlet} to launch the IDEasy GUI.
 */
public class Gui extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Gui.class);

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

    Path pomPath = context.getIdeInstallationPath().resolve("gui/pom.xml");
    if (!Files.exists(pomPath)) {
      LOG.error("Fatal error: The pom.xml file required for launching the IDEasy GUI could not be found in expected location: {}", pomPath);
      return;
    }

    List<String> args = List.of(
        "-f",
        pomPath.toString(),
        "exec:exec",
        "-Dexec.executable=java",
        "-Dexec.classpathScope=compile",
        "-Dexec.args=-classpath %classpath com.devonfw.ide.gui.AppLauncher"
    );

    mvn.runTool(processContext, ProcessMode.DEFAULT, args);
  }
}
