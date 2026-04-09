package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;

/**
 * Test of {@link EnvironmentCommandlet} for global configuration.
 */
class EnvironmentCommandletGlobalTest extends AbstractIdeContextTest {

  @Test
  void testRunOutsideProject(@TempDir Path tempDir) throws IOException {

    // arrange
    Path userHome = tempDir.resolve("user");
    Path userHomeIde = userHome.resolve(".ide");
    Files.createDirectories(userHomeIde);
    Files.writeString(userHomeIde.resolve("ide.properties"), "export FOO=bar\n");

    IdeTestContext context = new IdeTestContext(tempDir.resolve("cwd"), null);
    context.setUserHome(userHome);

    EnvironmentCommandlet env = context.getCommandletManager().getCommandlet(EnvironmentCommandlet.class);

    // act
    env.run();

    // assert
    assertThat(context).log().hasEntries( //
        IdeLogEntry.ofProcessable("export FOO=\"bar\"") //
    );
  }
}
