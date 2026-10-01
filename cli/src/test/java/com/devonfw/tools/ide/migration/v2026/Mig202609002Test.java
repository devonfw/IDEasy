package com.devonfw.tools.ide.migration.v2026;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.FileAccess;

/**
 * Test of {@link Mig202609002}.
 */
class Mig202609002Test extends AbstractIdeContextTest {

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
    new Mig202609002().run(context);
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
    new Mig202609002().run(context);
    // assert
    assertThat(vscodeMeta).doesNotExist();
  }

  /**
   * Tests that the migration skips a workspace whose target folder already exists, without failing and without overwriting the existing data.
   */
  @Test
  void testSkipsWhenTargetAlreadyExists() {

    // arrange
    IdeTestContext context = newContext("vscode");
    FileAccess fileAccess = context.getFileAccess();
    Path oldUserData = context.getWorkspacePath().resolve(".vscode").resolve(".userdata");
    fileAccess.mkdirs(oldUserData);
    fileAccess.writeFileContent("old", oldUserData.resolve("state.json"));
    Path target = context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve("vscode").resolve(context.getWorkspaceName()).resolve("config");
    fileAccess.mkdirs(target);
    fileAccess.writeFileContent("new", target.resolve("state.json"));
    // act
    new Mig202609002().run(context);
    // assert
    assertThat(oldUserData).exists();
    assertThat(target.resolve("state.json")).exists().hasContent("new");
  }
}
