package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Integration test of {@link InstallPluginCommandlet}.
 */
public class InstallPluginCommandletTest extends AbstractIdeContextTest {

  /**
   * Tests if {@link InstallPluginCommandlet} can install intellij plugins with custom url.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testInstallPlugin(String os) {

    // arrange
    IdeTestContext context = newContext("intellij");
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    InstallPluginCommandlet commandlet = new InstallPluginCommandlet(context);
    commandlet.tool.setValueAsString("intellij", context);
    commandlet.plugin.setValueAsString("ActivePlugin", context);
    // act
    commandlet.run();

    // assert
    assertThat(context).logAtSuccess().hasEntries("Successfully ended step 'Install plugin: ActivePlugin'.");
  }
}
