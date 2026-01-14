package com.devonfw.tools.ide.tool.eclipse;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.tool.intellij.Intellij;

/**
 * Test of {@link Eclipse}.
 */
public class EclipseTest extends AbstractIdeContextTest {

  private static final String PROJECT_ECLIPSE = "eclipse";

  /**
   * Tests if the {@link Intellij} can be installed properly.
   *
   * @param os String of the OS to use.
   * @throws IOException if reading the content of the mocked plugin fails
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  void testEclipse(String os) throws IOException {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    IdeTestContext context = newContext(PROJECT_ECLIPSE, "eclipseproject");
    context.setSystemInfo(systemInfo);
    context.getStartContext().setForceMode(true); // #663
    Eclipse eclipse = context.getCommandletManager().getCommandlet(Eclipse.class);

    // act
    eclipse.run();

    // assert
    Path eclipsePath = context.getSoftwarePath().resolve("eclipse");
    assertThat(eclipsePath.resolve(".ide.software.version")).exists().hasContent("2024-09");
    assertThat(context).log().hasEntries(
        new IdeLogEntry(IdeLogLevel.SUCCESS, "Successfully installed java in version 17.0.10_7", true),
        new IdeLogEntry(IdeLogLevel.SUCCESS, "Successfully installed eclipse in version 2024-09", true));
    assertThat(context).logAtSuccess().hasMessage("Successfully ended step 'Install plugin anyedit'.");
    assertThat(context.getPluginsPath().resolve("eclipse")).isDirectory();
    assertThat(eclipsePath.resolve("eclipsetest")).hasContent(
        "eclipse " + os + " -data " + context.getWorkspacePath() + " -keyring " + context.getUserHome().resolve(".eclipse").resolve(".keyring")
            + " -configuration " + context.getPluginsPath().resolve("eclipse").resolve("configuration") + " gui -showlocation eclipseproject");

    //if tool already installed
    eclipse.install();
    assertThat(context).logAtDebug().hasMessageContaining("Version 2024-09 of tool eclipse is already installed");
  }

}
