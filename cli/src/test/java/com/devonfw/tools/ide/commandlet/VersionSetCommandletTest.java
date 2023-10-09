package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import org.junit.jupiter.api.Test;


/**
 * Integration test of {@link VersionSetCommandlet}.
 */
public class VersionSetCommandletTest extends AbstractIdeContextTest{

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
    versionSet.version.setValueAsString("1.0.0");
    //act
    versionSet.run();
    //assert
    assertThat(versionSet.version.getValueAsString()).isEqualTo("1.0.0");
  }
}
