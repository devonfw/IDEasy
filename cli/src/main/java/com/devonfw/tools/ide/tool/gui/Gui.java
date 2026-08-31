package com.devonfw.tools.ide.tool.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.version.IdeVersion;
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

    return false;
  }

  @Override
  protected void doRun() {

    ProcessContext processContext = context.newProcess();

    Java java = this.context.getCommandletManager().getCommandlet(Java.class);
    Mvn mvn = this.context.getCommandletManager().getCommandlet(Mvn.class);

    ToolInstallRequest mavenToolInstallRequest = new ToolInstallRequest(false);
    mavenToolInstallRequest.setProcessContext(processContext);
    mavenToolInstallRequest.setIgnoreProject(true);

    ToolInstallRequest javaToolInstallRequest = new ToolInstallRequest(mavenToolInstallRequest);
    javaToolInstallRequest.setRequested(
        new ToolEditionAndVersion(VersionIdentifier.of("25.*"))
    );

    ToolInstallation mvnToolInstallation = mvn.installTool(mavenToolInstallRequest);
    ToolInstallation javaInstallation = java.installTool(javaToolInstallRequest);

    /*
     * Register the freshly installed mvn on the IDEasy-managed PATH so the IDEasy-controlled maven (from the software
     * repository) launches the GUI instead of any maven on the system PATH. installTool only installs into the software
     * repository, so we register the bin directory here (install() would normally do this).
     */
    context.getPath().setPath(mvn.getName(), mvnToolInstallation.binDir());

    LOG.debug("Starting GUI via commandlet");

    Path installationPath = context.getIdeInstallationPath();
    Path pomPath = installationPath.resolve("gui/pom.xml");
    if (!Files.exists(pomPath)) {
      throw new CliException("Fatal error: The pom.xml file required for launching the IDEasy GUI could not be found in expected location: " + pomPath);
    }

    List<String> args = buildMvnArgs(installationPath, pomPath, getGuiExecutable(javaInstallation));

    /*
     * We manually update the PATH entry with our java version, as by default IDEasy includes the SymLink under /projectname/software/java/bin in the PATH
     * In case of projects using older Java Versions, this is important as the java version of the project could potentially older.
     */
    processContext = processContext.withPathEntry(javaInstallation.binDir());
    try {
      mvn.runTool(processContext, ProcessMode.DEFAULT, args);
    } catch (RuntimeException e) {
      throw new CliException(
          "Failed to launch the GUI. If maven reports issues with dependency resolution, the self-contained maven repository of the IDEasy "
              + "installation may be incomplete - re-run the local-dev build (build-local-dev.sh) to repopulate it.",
          e);
    }
  }

  /**
   * Builds the arguments passed to {@code mvn} to launch the GUI via the launcher POM in the IDEasy installation.
   * <p>
   * A local-dev installation (created by {@code build-local-dev.sh}) is self-contained: its launcher POM resolves {@code ide-gui} from a maven repository
   * inside the installation and runs offline.
   * </p>
   *
   * @param installationPath the {@link IdeContext#getIdeInstallationPath() IDEasy installation} directory containing {@code gui/pom.xml}.
   * @param pomPath the launcher POM ({@code gui/pom.xml}) to launch the GUI from. Must be verified to exist before this is called.
   * @param execExecutable the executable to pass as {@code -Dexec.executable} (see {@link #getGuiExecutable(ToolInstallation)}).
   * @return the {@code mvn} arguments to launch the GUI.
   */
  static List<String> buildMvnArgs(Path installationPath, Path pomPath, String execExecutable) {

    List<String> args = new ArrayList<>(List.of(
        "-f", //use specified POM file
        pomPath.toString(),
        "org.codehaus.mojo:exec-maven-plugin:3.1.0:exec",
        "-Dexec.executable=" + execExecutable,
        "-Dexec.classpathScope=compile",
        "-Dexec.args=-classpath %classpath com.devonfw.ide.gui.AppLauncher",
        "-Dexec.async=true"
    ));

    if (IdeVersion.isLocalDevBuild()) {
      LOG.warn("Launching gui from the self-contained maven repository of the local-dev installation");
      args.add("-Dmaven.repo.local=" + installationPath.resolve(".m2").toString());
      args.add("-o"); // run offline so the local build is used and no remote snapshot is resolved
    } else if (!IdeVersion.getVersionIdentifier().isStable()) {
      LOG.warn("Launching gui in snapshot mode");
      args.add("-U"); //Adding this flag forces maven to download the latest SNAPSHOT version
    }
    return args;
  }

  private static final String GUI_INFO_PLIST = """
      <?xml version="1.0" encoding="UTF-8"?>
      <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
      <plist version="1.0">
      <dict>
        <key>CFBundleExecutable</key>
        <string>IDEasy</string>
        <key>CFBundleIdentifier</key>
        <string>com.devonfw.tools.ideasy.gui</string>
        <key>CFBundleName</key>
        <string>IDEasy</string>
        <key>CFBundlePackageType</key>
        <string>APPL</string>
        <key>CFBundleShortVersionString</key>
        <string>1.0</string>
      </dict>
      </plist>
      """;

  /**
   * The GUI is launched as a plain {@code java} process without a native app bundle. On macOS this makes the Dock and menu bar show the executable's
   * filename "java" instead of "IDEasy" (see issue #2206). Merely renaming/symlinking the bare executable is not reliable: without a real bundle,
   * LaunchServices may still fall back to the underlying JDK's own identity. Instead, we wrap the launch in a minimal {@code .app} bundle: a proper
   * {@code Info.plist} declaring our own {@code CFBundleName}/{@code CFBundleIdentifier}, with {@code Contents/MacOS/IDEasy} as a symlink pointing
   * directly at the real java executable (not a wrapper script - an intermediate {@code exec} would replace the process image and lose the bundle
   * identity again).
   *
   * @param javaInstallation the {@link ToolInstallation} of the java tool used to launch the GUI.
   * @return the executable to pass to Maven's {@code exec:exec} goal in order to launch the GUI.
   */
  String getGuiExecutable(ToolInstallation javaInstallation) {

    if (!this.context.getSystemInfo().isMac()) {
      return "java";
    }
    Path javaExecutable = javaInstallation.binDir().resolve("java");
    Path contentsDir = this.context.getTempPath().resolve("IDEasy.app").resolve("Contents");
    Path launcher = contentsDir.resolve("MacOS").resolve("IDEasy");
    this.context.getFileAccess().mkdirs(launcher.getParent());
    this.context.getFileAccess().writeFileContent(GUI_INFO_PLIST, contentsDir.resolve("Info.plist"));
    this.context.getFileAccess().symlink(javaExecutable, launcher, false);
    return launcher.toString();
  }
}
