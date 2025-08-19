package com.devonfw.tools.ide.tool.tomcat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.tool.java.Java;

/**
 * Test of {@link Tomcat}.
 */
public class TomcatTest extends AbstractIdeContextTest {

  private static final String PROJECT_TOMCAT = "tomcat";

  @Test
  public void testTomcat() {

    // in the tomcat project we have JAVA_VERSION=8u402b06 and TOMCAT_VERSION=10.1.14
    // that tomcat version requires Java but in version range [11,21_35] what is incompatible
    // so we test that Java 8u402b06 gets installed in the project but for tomcat
    // additionally Java version 21_35 gets installed in the software repository and is used by tomcat
    // instead of the project Java version 8u402b06

    // arrange
    String javaVersionProject = "8u402b06";
    String tomcatVersion = "10.1.14";
    String javaVersionTomcat = "21_35";
    String tomcatPort = "8088";
    IdeTestContext context = newContext(PROJECT_TOMCAT);
    Java javaCommandlet = context.getCommandletManager().getCommandlet(Java.class);
    Tomcat tomcatCommandlet = context.getCommandletManager().getCommandlet(Tomcat.class);
    Path javaTomcatPath = context.getToolRepositoryPath().resolve("default").resolve("java").resolve("java").resolve(javaVersionTomcat);

    // act
    javaCommandlet.arguments.addValue("--version");
    javaCommandlet.run();
    tomcatCommandlet.run();

    // assert
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent(javaVersionProject);
    assertThat(context.getSoftwarePath().resolve("tomcat/bin/catalina.bat")).exists();
    assertThat(context.getSoftwarePath().resolve("tomcat/bin/catalina.sh")).exists();
    assertThat(context.getSoftwarePath().resolve("tomcat/.ide.software.version")).exists().hasContent(tomcatVersion);
    assertThat(javaTomcatPath.resolve(".ide.software.version")).exists().hasContent(javaVersionTomcat);
    assertThat(context).log().hasEntries(
        new IdeLogEntry(IdeLogLevel.SUCCESS, "Successfully installed java in version " + javaVersionProject),
        new IdeLogEntry(IdeLogLevel.INFO, "OpenJDK version " + javaVersionProject),
        new IdeLogEntry(IdeLogLevel.INFO,
            "The tool tomcat requires java in the version range [11,22), but your project uses version 8u402b06, which does not match."
                + " Therefore, we install a compatible version in that range."),
        new IdeLogEntry(IdeLogLevel.DEBUG, "Installed java in version " + javaVersionTomcat + " at ", true),
        new IdeLogEntry(IdeLogLevel.SUCCESS, "Successfully installed tomcat in version " + tomcatVersion),
        new IdeLogEntry(IdeLogLevel.INFO, "JAVA_HOME=" + javaTomcatPath),
        new IdeLogEntry(IdeLogLevel.INFO, "tomcat run"),
        new IdeLogEntry(IdeLogLevel.INFO, "Tomcat is running at localhost on HTTP port " + tomcatPort + ":"),
        new IdeLogEntry(IdeLogLevel.INFO, "http://localhost:" + tomcatPort)
    );
  }

}
