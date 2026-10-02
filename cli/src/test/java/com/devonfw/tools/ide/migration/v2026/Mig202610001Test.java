package com.devonfw.tools.ide.migration.v2026;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.FileAccess;

/**
 * Test of {@link Mig202610001}.
 */
class Mig202610001Test extends AbstractIdeContextTest {

  private static final String PROJECT_MIGRATION = "migration";

  /**
   * Tests that an existing {@code .intellij/config} folder is moved out of the workspace into {@code $IDE_HOME/.ide/intellij/«workspace»/config}.
   */
  @Test
  void testMovesJetBrainsConfigOutOfWorkspace() {

    // arrange
    IdeTestContext context = newContext(PROJECT_MIGRATION);
    FileAccess fileAccess = context.getFileAccess();
    Path legacyMetadata = context.getWorkspacePath().resolve(".intellij");
    fileAccess.writeFileContent("dummy", legacyMetadata.resolve("config").resolve("options").resolve("editor.xml"), true);

    // act
    new Mig202610001().run(context);

    // assert
    assertThat(ideMetadata(context, "intellij").resolve("config").resolve("options").resolve("editor.xml")).exists().hasContent("dummy");
    // the emptied legacy folder is removed so the workspace stays clean
    assertThat(legacyMetadata).doesNotExist();
  }

  /**
   * Tests that the migration also handles the other JetBrains based IDEs and backs up the obsolete generated properties files instead of deleting them.
   * <p>
   * The set of IDEs is intentionally frozen in {@link Mig202610001}: a migration is a historical record and must keep behaving as it did at 2026.10.001, so
   * this test pins that list rather than deriving it from the current commandlets.
   */
  @Test
  void testMigratesAllJetBrainsIdesAndBacksUpProperties() {

    // arrange
    IdeTestContext context = newContext(PROJECT_MIGRATION);
    FileAccess fileAccess = context.getFileAccess();
    Path workspace = context.getWorkspacePath();
    fileAccess.writeFileContent("studio", workspace.resolve(".android-studio").resolve("config").resolve("marker.txt"), true);
    fileAccess.writeFileContent("pycharm", workspace.resolve(".pycharm").resolve("config").resolve("marker.txt"), true);
    fileAccess.writeFileContent("obsolete", workspace.resolve("idea.properties"));
    fileAccess.writeFileContent("obsolete", workspace.resolve("studio.properties"));
    fileAccess.writeFileContent("obsolete", workspace.resolve("pycharm.properties"));

    // act
    new Mig202610001().run(context);

    // assert
    assertThat(ideMetadata(context, "android-studio").resolve("config").resolve("marker.txt")).exists().hasContent("studio");
    assertThat(ideMetadata(context, "pycharm").resolve("config").resolve("marker.txt")).exists().hasContent("pycharm");
    assertThat(workspace.resolve("idea.properties")).doesNotExist();
    assertThat(workspace.resolve("studio.properties")).doesNotExist();
    assertThat(workspace.resolve("pycharm.properties")).doesNotExist();
    // the properties are backed up, not deleted
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_BACKUPS)).exists();
  }

  /**
   * Tests that the migration leaves the workspaces untouched when they contain no JetBrains metadata.
   */
  @Test
  void testDoesNothingWhenNoLegacyMetadata() {

    // arrange
    IdeTestContext context = newContext(PROJECT_MIGRATION);
    List<Path> before = listWorkspaceFiles(context);

    // act
    new Mig202610001().run(context);

    // assert
    assertThat(listWorkspaceFiles(context)).containsExactlyElementsOf(before);
    assertThat(context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE)).doesNotExist();
  }

  /**
   * Tests that the migration skips a workspace whose target folder already exists, without failing and without overwriting the existing data.
   */
  @Test
  void testSkipsWhenTargetAlreadyExists() {

    // arrange
    IdeTestContext context = newContext(PROJECT_MIGRATION);
    FileAccess fileAccess = context.getFileAccess();
    Path legacyConfig = context.getWorkspacePath().resolve(".intellij").resolve("config");
    fileAccess.writeFileContent("old", legacyConfig.resolve("marker.txt"), true);
    Path config = ideMetadata(context, "intellij").resolve("config");
    fileAccess.writeFileContent("new", config.resolve("marker.txt"), true);

    // act
    new Mig202610001().run(context);

    // assert
    assertThat(config.resolve("marker.txt")).exists().hasContent("new");
    assertThat(legacyConfig.resolve("marker.txt")).exists().hasContent("old");
  }

  private static Path ideMetadata(IdeTestContext context, String ide) {

    return context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve(ide).resolve(context.getWorkspaceName());
  }

  private static List<Path> listWorkspaceFiles(IdeTestContext context) {

    return context.getFileAccess().listChildren(context.getWorkspacesBasePath(), path -> true).stream()
        .flatMap(workspace -> context.getFileAccess().listChildren(workspace, path -> true).stream())
        .sorted()
        .toList();
  }
}
