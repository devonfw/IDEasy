package com.devonfw.tools.ide.tool.tomcat;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

public class Tomcat extends LocalToolCommandlet {

  public final EnumProperty<TomcatCommand> command;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Tomcat(IdeContext context) {

    super(context, "tomcat", Set.of(Tag.JAVA));
    this.command = add(new EnumProperty<>("", true, "command", TomcatCommand.class));
    add(this.arguments);
  }

  @Override
  public boolean install(boolean silent) {

    return super.install(silent);
  }

  @Override
  public void postInstall() {

    super.postInstall();

    EnvironmentVariables variables = this.context.getVariables();
    EnvironmentVariables typeVariables = variables.getByType(EnvironmentVariablesType.CONF);

    typeVariables.set("CATALINA_HOME", getToolPath().toString(), true);
    typeVariables.save();
  }

  @Override
  protected void initProperties() {

    // Empty on purpose
  }

  @Override
  public void run() {

    TomcatCommand command = this.command.getValue();

    switch (command) {
      case START:
        // printTomcatPort();
        arguments.setValueAsString("start", context);
        super.run();
        break;
      case STOP:
        arguments.setValueAsString("stop", context);
        break;
      default:
    }
  }

  @Override
  protected String getBinaryName() {

    TomcatCommand command = this.command.getValue();

    Path toolBinPath = getToolBinPath();

    String tomcatHome = null;

    if (this.context.getSystemInfo().isWindows()) {
      if (command.equals(TomcatCommand.START)) {
        tomcatHome = "startup.bat";
      } else if (command.equals(TomcatCommand.STOP)) {
        tomcatHome = "shutdown.bat";
      } else {
        this.context.error("Unknown tomcat command");
      }
    } else {
      if (command.equals(TomcatCommand.START)) {
        tomcatHome = "startup.sh";
      } else if (command.equals(TomcatCommand.STOP)) {
        tomcatHome = "shutdown.sh";
      } else {
        this.context.error("Unknown tomcat command");
      }
    }

    return toolBinPath.resolve(tomcatHome).toString();
  }

  // private void printTomcatPort() {
  //
  // this.context.info("Tomcat is running at localhost on the following port (default 8080):");
  // Path tomcatPropertiesPath = getToolPath().resolve("conf/server.xml");
  //
  // Properties tomcatProperties = PropertiesFileUtil.loadProperties(tomcatPropertiesPath);
  // String tomcatWebPort = tomcatProperties.getProperty("redirectPort");
  // if (tomcatWebPort != null) {
  // this.context.info("TEST: " + tomcatWebPort);
  // }
  // }

}
