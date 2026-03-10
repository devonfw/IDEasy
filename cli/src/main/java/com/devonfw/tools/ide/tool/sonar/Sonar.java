package com.devonfw.tools.ide.tool.sonar;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.mvn.Mvn;

/**
 * {@link LocalToolCommandlet} for <a href="https://sonarqube.org/">SonarQube</a>.
 */
public class Sonar extends LocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Sonar.class);

  /** The {@link SonarCommand} to run. */
  public final EnumProperty<SonarCommand> command;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}. method.
   */
  public Sonar(IdeContext context) {

    super(context, "sonar", Set.of(Tag.CODE_QA));

    this.command = add(new EnumProperty<>("", true, "command", SonarCommand.class));
    add(this.arguments);
  }

  @Override
  protected void initProperties() {

    // Empty on purpose
  }

  @Override
  public ToolInstallation install(boolean silent) {

    getCommandlet(Java.class).install();
    return super.install(silent);
  }

  @Override
  protected void doRun() {

    SonarCommand command = this.command.getValue();

    Path toolPath = getToolPath();
    if (!toolPath.toFile().exists()) {
      super.install(true);
    }

    switch (command) {
      case ANALYZE:
        getCommandlet(Mvn.class).runTool(List.of("sonar:sonar"));
        break;
      case START:
        printSonarWebPort();
        this.arguments.setValueAsString("start", this.context);
        super.run();
        break;
      case STOP:
        this.arguments.setValueAsString("stop", this.context);
        super.run();
        break;
      default:
    }
  }

  @Override
  protected String getBinaryName() {
    SonarCommand command = this.command.getValue();
    if (this.context.getSystemInfo().isWindows()) {
      if (command != null && command.equals(SonarCommand.STOP)) {
        return "windows-x86-64/SonarService.bat";

      } else {
        return "windows-x86-64/StartSonar.bat";
      }
    } else if (this.context.getSystemInfo().isMac()) {
      return "macosx-universal-64/sonar.sh";
    }
    return "linux-x86-64/sonar.sh";
  }

  //  Instead of relying on relative Path (which breaks), we directly construct the absolute path
//  so getToolBinPath would return /software/sonar/bin and .resolve adds x86-x64
//  ProcessContext can now execute the abs path without any issues.
//   Worst case, we fall back to the default configureToolBinary, which uses relative path.
  @Override
  protected void configureToolBinary(ProcessContext pc, ProcessMode processMode) {
    Path toolBinPath = getToolBinPath();
    if (toolBinPath != null) {
      Path binaryPath = toolBinPath.resolve(getBinaryName());
      pc.executable(binaryPath);
    } else {
      super.configureToolBinary(pc, processMode);
    }
  }

  private void printSonarWebPort() {

    LOG.info("SonarQube is running at localhost on the following port (default 9000):");
    Path sonarPropertiesPath = getToolPath().resolve("conf/sonar.properties");

    Properties sonarProperties = this.context.getFileAccess().readProperties(sonarPropertiesPath);
    String sonarWebPort = sonarProperties.getProperty("sonar.web.port");
    if (sonarWebPort != null) {
      LOG.info(sonarWebPort);
    }
  }
}
