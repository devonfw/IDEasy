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
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.property.FlagProperty;
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

  final FlagProperty enableExtendedLogging;

  /**
   * @param context the {@link IdeContext}.
   */
  public Gui(IdeContext context) {

    super(context);
    addKeyword(getName());
    enableExtendedLogging = add(new FlagProperty("--enableLogging", false, "-l"));
  }

  @Override
  public String getName() {

    return "gui";
  }

  @Override
  protected void doRun() {

    ProcessContext processContext = context.newProcess();

    Java java = this.context.getCommandletManager().getCommandlet(Java.class);
    Mvn mvn = this.context.getCommandletManager().getCommandlet(Mvn.class);

    ToolInstallRequest mavenToolInstallRequest = new ToolInstallRequest(false);
    mavenToolInstallRequest.setProcessContext(processContext);

    ToolInstallRequest javaToolInstallRequest = new ToolInstallRequest(mavenToolInstallRequest);
    javaToolInstallRequest.setRequested(
        new ToolEditionAndVersion(VersionIdentifier.of("25.*"))
    );

    ToolInstallation mvnToolInstallation = mvn.installTool(mavenToolInstallRequest);
    ToolInstallation javaInstallation = java.installTool(javaToolInstallRequest);

    /* Register the freshly installed mvn on the IDEasy managed PATH so that the IDEasy controlled maven is used to launch
     the GUI instead of any maven that happens to be on the system PATH. We install via installTool (software repository
     only) and therefore have to register the bin directory ourselves (install() would normally do this). I tried to achieve this alternatively via .withPathEntry(), however, this did not work as expected.
     This was tested on a Mac; potentially, withPathVariable() works correctly on Windows.
    */
    context.getPath().setPath(mvn.getName(), mvnToolInstallation.binDir());

    LOG.debug("Starting GUI via commandlet");

    Path pomPath = context.getIdeInstallationPath().resolve("gui/pom.xml");
    if (!Files.exists(pomPath)) {
      throw new CliException("Fatal error: The pom.xml file required for launching the IDEasy GUI could not be found in expected location: " + pomPath);
    }

    List<String> args = List.of(
        "-U", //required for latest snapshot versions
        "-f", //use specified POM file
        pomPath.toString(),
        "org.codehaus.mojo:exec-maven-plugin:3.1.0:exec",
        "-Dexec.executable=java",
        "-Dexec.classpathScope=compile",
        "-Dexec.args=-classpath %classpath com.devonfw.ide.gui.AppLauncher"
    );

    /*
     * We manually update the PATH entry with our java version, as by default IDEasy includes the SymLink under /projectname/software/java/bin in the PATH
     * In case of projects using older Java Versions, this is important as the java version of the project could potentially older.
     */
    ProcessMode processMode = this.enableExtendedLogging.isTrue() ? ProcessMode.DEFAULT : ProcessMode.BACKGROUND_SILENT;
    try {
      mvn.runTool(processContext.withPathEntry(javaInstallation.binDir()), processMode, args);
    } catch (Exception e) {
      LOG.error(
          "Failed to launch the GUI. If maven states issues with dependency resolution, check whether the maven M2 repo is enabled in your project.",
          e);
    }
  }
}
