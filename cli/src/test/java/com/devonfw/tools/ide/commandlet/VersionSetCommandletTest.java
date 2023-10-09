package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Integration test of {@link VersionSetCommandlet}.
 */
public class VersionSetCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link VersionSetCommandlet} run.
   */
  @Test
  public void testVersionSetCommandletRun() {
    //arrange
    String path = "workspaces/foo-test/my-git-repo";
    IdeContext context = newContext("basic", path, true);
    VersionSetCommandlet versionSet = context.getCommandletManager().getCommandlet(VersionSetCommandlet.class);
    versionSet.tool.setValueAsString("mvn");
    versionSet.version.setValueAsString("3.1.0");
    //act
    versionSet.run();
    //assert
      try {
      assertThat(Files.readAllLines(context.getSettingsPath().resolve("ide.properties")).contains("MVN_VERSION=3.1.0"));
    } catch (IOException e) {
      fail("no ide.properties was found");
    }
  }
}
