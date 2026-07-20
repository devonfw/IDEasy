package com.devonfw.tools.ide.migration.v2026;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.FileAccess;

/**
 * Test of {@link Mig202608001}.
 */
class Mig202608001Test extends AbstractIdeContextTest {

  /**
   * Tests that an existing {@code .vscode/.userdata} folder is moved out of the workspace into {@code $IDE_HOME/.ide/vscode/«workspace»/config}.
   */
  @Test
  void testMovesVscodeUserDataOutOfWorkspace() {

    // arrange
    IdeTestContext context = newContext("vscode");
    FileAccess fileAccess = context.getFileAccess();
    Path workspace = context.getWorkspacePath();
    Path oldUserData = workspace.resolve(".vscode").resolve(".userdata");
    fileAccess.mkdirs(oldUserData);
    fileAccess.writeFileContent("dummy", oldUserData.resolve("state.json"));
    // act
    new Mig202608001().run(context);
    // assert
    Path newConfig = context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve("vscode").resolve(context.getWorkspaceName()).resolve("config");
    assertThat(newConfig.resolve("state.json")).exists().hasContent("dummy");
    assertThat(oldUserData).doesNotExist();
  }

  /**
   * Tests that the migration is a no-op (and does not fail) when no {@code .vscode/.userdata} folder exists.
   */
  @Test
  void testDoesNothingWhenNoUserData() {

    // arrange
    IdeTestContext context = newContext("vscode");
    Path vscodeMeta = context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve("vscode");
    // act
    new Mig202608001().run(context);
    // assert
    assertThat(vscodeMeta).doesNotExist();
  }
}
