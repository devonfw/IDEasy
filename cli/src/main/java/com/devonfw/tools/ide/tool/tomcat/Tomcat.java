package com.devonfw.tools.ide.tool.tomcat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.version.GenericVersionRange;

/**
 * {@link ToolCommandlet} for <a href="https://tomcat.apache.org/">tomcat</a>.
 */
public class Tomcat extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Tomcat(IdeContext context) {

    super(context, "tomcat", Set.of(Tag.JAVA));
  }

  @Override
  public ProcessResult runTool(ProcessMode processMode, GenericVersionRange toolVersion, ProcessErrorHandling errorHandling, String... args) {

    if (args.length == 0) {
      args = new String[] { "start" };
    }
    ProcessResult processResult = super.runTool(processMode, toolVersion, errorHandling, args);
    if (processResult.isSuccessful() && (args[0].equals("start") || args[0].equals("run"))) {
      printTomcatPort();
    }
    return processResult;
  }

  @Override
  protected void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean extraInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, extraInstallation);
    environmentContext.withEnvVar("CATALINA_HOME", toolInstallation.linkDir().toString());
  }

  @Override
  public String getBinaryName() {

    return "catalina.sh";
  }

  private void printTomcatPort() {

    String portNumber = findTomcatPort();
    if (!portNumber.isEmpty()) {
      this.context.info("Tomcat is running at localhost on HTTP port {}:", portNumber);
      this.context.info("http://localhost:{}", portNumber);
    }
  }

  private String findTomcatPort() {

    String portNumber = "";
    Path tomcatPropertiesPath = getToolPath().resolve("conf/server.xml");
    if (Files.exists(tomcatPropertiesPath)) {
      try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(tomcatPropertiesPath.toFile());
        NodeList connectorNodes = document.getElementsByTagName("Connector");
        if (connectorNodes.getLength() > 0) {
          Element ConnectorElement = (Element) connectorNodes.item(0);
          portNumber = ConnectorElement.getAttribute("port");
        } else {
          this.context.warning("Port element not found in server.xml");
        }
      } catch (ParserConfigurationException | IOException | SAXException e) {
        this.context.error(e);
      }
    }
    if (portNumber.isEmpty()) {
      this.context.warning("Could not find HTTP port in {}", tomcatPropertiesPath);
    }
    return portNumber;
  }

}
