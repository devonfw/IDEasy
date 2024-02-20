package com.devonfw.tools.ide.tool.sonar;

import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.util.PropertiesFileUtil;

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
  public boolean install(boolean silent) {

    getCommandlet(Java.class).install();
    return super.install(silent);
  }

  @Override
  public void run() {

    SonarCommand command = this.command.getValue();

    switch (command) {
      case ANALYZE:
        getCommandlet(Mvn.class).runTool(null, "sonar:sonar");
        break;
      case START:
        printSonarWebPort();
        arguments.setValueAsString("start", context);
        super.run();
        break;
      case STOP:
        arguments.setValueAsString("stop", context);
        super.run();
        break;
      default:
    }
  }

  @Override
  protected String getBinaryName() {

    SonarCommand command = this.command.getValue();

    Path toolBinPath = getToolBinPath();
    String sonarLocation = null;

    if (this.context.getSystemInfo().isWindows()) {
      if (command.equals(SonarCommand.START)) {
        sonarLocation = "windows-x86-64/StartSonar.bat";
      } else if (command.equals(SonarCommand.STOP)) {
        sonarLocation = "windows-x86-64/SonarService.bat";
      }
    } else if (this.context.getSystemInfo().isMac()) {
      sonarLocation = "macosx-universal-64/sonar.sh";
    } else {
      sonarLocation = "linux-x86-64/sonar.sh";
    }
    return toolBinPath.resolve(sonarLocation).toString();
  }

  private void printSonarWebPort() {

    this.context.info("SonarQube is running at localhost on the following port (default 9000):");
    Path sonarPropertiesPath = getToolPath().resolve("conf/sonar.properties");

    Properties sonarProperties = PropertiesFileUtil.loadProperties(sonarPropertiesPath);
    String sonarWebPort = sonarProperties.getProperty("sonar.web.port");
    if (sonarWebPort != null) {
      this.context.info(sonarWebPort);
    }
  }
}
