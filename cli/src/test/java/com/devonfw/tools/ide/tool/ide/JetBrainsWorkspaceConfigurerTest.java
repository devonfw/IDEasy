package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link JetBrainsWorkspaceConfigurer} — the targeted merge that keeps the JetBrains metadata out of the workspace (see #2531).
 */
class JetBrainsWorkspaceConfigurerTest extends AbstractIdeContextTest {

  /**
   * Tests that the JetBrains tool-specific templates are split into targeted merges: the {@code .intellij/config} subtree is merged into the
   * out-of-workspace metadata config folder, the top-level {@code idea.properties} is never merged, and the generic templates (editorconfig,
   * user.properties) are still merged into the workspace (see #2531).
   */
  @Test
  void testConfigureWorkspaceSplitsIntoTargetedMerges() {

    // arrange
    IdeTestContext context = newContext("intellij");
    Path workspace = context.getWorkspacePath();
    Path metadataConfig = context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve("intellij").resolve(context.getWorkspaceName())
        .resolve("config");
    JetBrainsWorkspaceConfigurer configurer = new JetBrainsWorkspaceConfigurer(context, "intellij");

    // act
    configurer.configureWorkspace(w -> Map.of());

    // assert: the .intellij/config subtree is merged into the out-of-workspace metadata config folder (user-home setup + settings update)...
    assertThat(metadataConfig.resolve("idea.key")).exists();
    assertThat(metadataConfig.resolve("options/code.style.schemes.xml")).exists().content().contains("ideasyTestOption");
    // ...and the JetBrains metadata is NOT in the workspace
    assertThat(workspace.resolve(".intellij")).doesNotExist();
    // ...and the top-level idea.properties in the JetBrains settings template is NOT merged into the workspace
    assertThat(workspace.resolve("idea.properties")).doesNotExist();
    // ...while the generic templates (editorconfig, user.properties) are still merged into the workspace
    assertThat(workspace.resolve(".editorconfig")).exists();
    assertThat(workspace.resolve("user.properties")).exists();
  }

  /**
   * Tests that {@link JetBrainsWorkspaceConfigurer#getIdeMetadataConfigPath()} resolves to {@code $IDE_HOME/.ide/«ide»/«workspace»/config} (the folder
   * used for {@code idea.config.path}, see #2531).
   */
  @Test
  void testGetIdeMetadataConfigPath() {

    // arrange
    IdeTestContext context = newContext("intellij");
    JetBrainsWorkspaceConfigurer configurer = new JetBrainsWorkspaceConfigurer(context, "intellij");

    // act & assert
    assertThat(configurer.getIdeMetadataConfigPath())
        .isEqualTo(context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve("intellij").resolve(context.getWorkspaceName()).resolve("config"));
  }

  /**
   * Tests that {@link JetBrainsWorkspaceConfigurer#resolveExtraSdkTarget} resolves an extra-SDK template (declared under the tool metadata folder, e.g.
   * {@code .intellij/config/options/jdk.table.xml}) into the out-of-workspace metadata config folder, not the workspace (see #2531).
   */
  @Test
  void testResolveExtraSdkTargetTargetsMetadataConfig() {

    // arrange
    IdeTestContext context = newContext("intellij");
    JetBrainsWorkspaceConfigurer configurer = new JetBrainsWorkspaceConfigurer(context, "intellij");

    // act
    Path target = configurer.resolveExtraSdkTarget(Path.of(".intellij/config/options/jdk.table.xml"), Map.of());

    // assert
    Path expected = context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve("intellij").resolve(context.getWorkspaceName())
        .resolve("config/options/jdk.table.xml");
    assertThat(target).isEqualTo(expected);
  }
}
