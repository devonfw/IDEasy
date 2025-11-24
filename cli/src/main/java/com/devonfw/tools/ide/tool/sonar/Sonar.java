package com.devonfw.tools.ide.tool.sonar;

import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.mvn.Mvn;

public class Sonar extends LocalToolCommandlet {

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
  public void run() {

    SonarCommand command = this.command.getValue();

    Path toolPath = getToolPath();
    if (!toolPath.toFile().exists()) {
      super.install(true);
    }

    switch (command) {
      case ANALYZE:
        getCommandlet(Mvn.class).runTool("sonar:sonar");
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

    if (this.context.getSystemInfo().isWindows()) {
      SonarCommand command = this.command.getValue();
      if (command.equals(SonarCommand.START)) {
        return "windows-x86-64/StartSonar.bat";
      } else if (command.equals(SonarCommand.STOP)) {
        return "windows-x86-64/SonarService.bat";
      }
    } else if (this.context.getSystemInfo().isMac()) {
      return "macosx-universal-64/sonar.sh";
    }
    return "linux-x86-64/sonar.sh";
  }

  private void printSonarWebPort() {

    this.context.info("SonarQube is running at localhost on the following port (default 9000):");
    Path sonarPropertiesPath = getToolPath().resolve("conf/sonar.properties");

    Properties sonarProperties = this.context.getFileAccess().readProperties(sonarPropertiesPath);
    String sonarWebPort = sonarProperties.getProperty("sonar.web.port");
    if (sonarWebPort != null) {
      this.context.info(sonarWebPort);
    }
  }
}
