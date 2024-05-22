package com.devonfw.tools.ide.tool.tomcat;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;

public class Tomcat extends LocalToolCommandlet {

  private final HashMap<String, String> dependenciesEnvVariableNames = new HashMap<>();

  public final EnumProperty<TomcatCommand> command;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Tomcat(IdeContext context) {

    super(context, "tomcat", Set.of(Tag.JAVA));
    this.command = add(new EnumProperty<>("", true, "command", TomcatCommand.class));
    //  add(this.arguments);
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

    // Empty on purpose, because no initial properties are added to the tool
  }

  @Override
  public void run() {

    TomcatCommand command = this.command.getValue();

    switch (command) {
      case START:
        printTomcatPort();
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

  @Override
  protected HashMap<String, String> listOfDependencyEnvVariableNames() {

    dependenciesEnvVariableNames.put("java", "JRE_HOME");
    return dependenciesEnvVariableNames;
  }

  private void printTomcatPort() {

    this.context.info("Tomcat is running at localhost on the following port (default 8080):");
    Path tomcatPropertiesPath = getToolPath().resolve("conf/server.xml");

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(tomcatPropertiesPath.toString());

      NodeList connectorNodes = document.getElementsByTagName("Connector");
      if (connectorNodes.getLength() > 0) {
        Element ConnectorElement = (Element) connectorNodes.item(0);
        String portNumber = ConnectorElement.getAttribute("port");
        this.context.info(portNumber);
      } else {
        this.context.warning("Port element not found in server.xml");
      }

    } catch (ParserConfigurationException | IOException | SAXException e) {
      this.context.error(e);
    }
  }

}
