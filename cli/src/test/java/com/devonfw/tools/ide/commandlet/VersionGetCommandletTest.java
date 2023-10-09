package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;


/**
   * Integration test of {@link VersionGetCommandlet}.
   */
public class VersionGetCommandletTest extends AbstractIdeContextTest{

    /**
     * Test of {@link VersionGetCommandlet} run, when Installed Version is null.
     */
    @Test
    public void testVersionGetCommandletRunThrowsCliExeption(){
      // arrange
      String path = "workspaces/foo-test/my-git-repo";
      IdeContext context = newContext("basic", path, false);
      VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
      versionGet.tool.setValueAsString("java");
      // act
      try {
        versionGet.run();
      } catch (CliException e) {
        //assert
        assertThat(e).isInstanceOf(Exception.class)
            .hasMessageContaining("Tool java is not installed!");
        return;
      }
      failBecauseExceptionWasNotThrown(CliException.class);
    }

    /**
     * Test of {@link VersionGetCommandlet} run.
     */
    @Test
    public void testVersionGetCommandletRun(){
      //arrange
      String path = "workspaces/foo-test/my-git-repo";
      IdeContext context = newContext("basic", path, false);
      VersionGetCommandlet versionGet = context.getCommandletManager().getCommandlet(VersionGetCommandlet.class);
      //act
      versionGet.tool.setValueAsString("mvn");
      versionGet.run();
      //assert
      assertLogMessage(context, IdeLogLevel.INFO, "3.9.4");
    }
  }

