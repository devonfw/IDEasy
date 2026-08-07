package com.devonfw.tools.ide.tool.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessContextImpl;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.version.VersionIdentifier;

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
  public boolean isIdeHomeRequired() {

    // The GUI is typically launched from a desktop shortcut and therefore starts outside of any IDEasy project.
    // Selecting the project is done within the GUI itself, which reads the available projects from IDE_ROOT.
    return false;
  }

  @Override
  protected void doRun() {

    ProcessContext processContext = new ProcessContextImpl(this.context);

    Java java = this.context.getCommandletManager().getCommandlet(Java.class);
    Mvn mvn = this.context.getCommandletManager().getCommandlet(Mvn.class);

    ToolInstallRequest mavenToolInstallRequest = new ToolInstallRequest(false);
    mavenToolInstallRequest.setProcessContext(processContext);
    mavenToolInstallRequest.setIgnoreProject(true);

    ToolInstallRequest javaToolInstallRequest = new ToolInstallRequest(mavenToolInstallRequest);
    javaToolInstallRequest.setRequested(
        new ToolEditionAndVersion(VersionIdentifier.of("25.*"))
    );

    ToolInstallation mvnInstallation = mvn.installTool(mavenToolInstallRequest);
    ToolInstallation javaInstallation = java.installTool(javaToolInstallRequest);

    // Without IDE_HOME there is no project software folder, so SystemPath cannot resolve the mvn binary on its own.
    // Registering the bin directory of the installation makes it findable, analogous to what LocalToolCommandlet does after a fresh installation.
    // The null check mirrors LocalToolCommandlet, since a null bin directory would end up in the tool map and break the next binary lookup.
    if (mvnInstallation.binDir() != null) {
      this.context.getPath().setPath(mvn.getName(), mvnInstallation.binDir());
    }

    LOG.debug("Starting GUI via commandlet");

    Path pomPath = context.getIdeInstallationPath().resolve("gui/pom.xml");
    if (!Files.exists(pomPath)) {
      throw new CliException("Fatal error: The pom.xml file required for launching the IDEasy GUI could not be found in expected location: " + pomPath);
    }

    List<String> args = List.of(
        "-f",
        pomPath.toString(),
        "exec:exec",
        "-Dexec.executable=java",
        "-Dexec.classpathScope=compile",
        "-Dexec.args=-classpath %classpath com.devonfw.ide.gui.AppLauncher"
    );

    /*
     * We manually update the PATH entry with our java version, as by default IDEasy includes the SymLink under /projectname/software/java/bin in the PATH
     * In case of projects using older Java Versions, this is important as the java version of the project could potentially older.
     */
    mvn.runTool(processContext.withPathEntry(javaInstallation.binDir()), ProcessMode.BACKGROUND_SILENT, args);
  }
}
