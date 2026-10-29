package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Tests for {@link RewriteCommandlet}.
 */
class RewriteCommandletTest extends AbstractIdeContextTest {

  private static final String RECIPE_JSON = """
      [
        {
          "origin_name": "java.format_java_code",
          "ideasy_command": "FORMAT_JAVA_CODE",
          "description": "Format Java code.",
          "url": "https://docs.openrewrite.org/recipes/java/format/autoformat",
          "raw_cmd": "mvn rewrite:run -Drewrite.activeRecipes=org.openrewrite.java.format.AutoFormat"
        },
        {
          "origin_name": "java.remove_blank_lines",
          "ideasy_command": "REMOVE_BLANK_LINES",
          "description": "Add and/or remove blank lines.",
          "url": "https://docs.openrewrite.org/recipes/java/format/blanklines",
          "raw_cmd": "mvn rewrite:run -Drewrite.activeRecipes=org.openrewrite.java.format.BlankLines"
        }
      ]
      """;

  /**
   * Writes the recipe configuration into the context's {@code ide-settings} settings folder.
   *
   * @param context the {@link IdeTestContext}.
   */
  private void writeRecipeConfig(IdeTestContext context) {
    Path configPath = context.getSettingsPath().resolve("rewrite").resolve("openrewrite.json");
    context.getFileAccess().writeFileContent(RECIPE_JSON, configPath, true);
  }

  /**
   * Tests that the RewriteCommandlet is registered and accessible via the commandlet manager.
   */
  @Test
  void testCommandletExists() {
    IdeTestContext context = newContext("basic");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    assertThat(commandlet).isNotNull();
    assertThat(commandlet.getName()).isEqualTo("rewrite");
  }

  /**
   * Tests that the RewriteCommandlet requires an IDEasy project context (settings folder).
   */
  @Test
  void testIdeHomeRequired() {
    IdeTestContext context = newContext("basic");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    assertThat(commandlet.isIdeHomeRequired()).isTrue();
  }

  /**
   * Tests that {@link RewriteCommandlet#doRun()} throws a {@link CliException} when the recipe
   * configuration is absent from the settings folder (settings-only).
   */
  @Test
  void testDoRunThrowsWhenNoRecipeConfig() {
    IdeTestContext context = newContext("basic");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    commandlet.command.setValue("FORMAT_JAVA_CODE");
    assertThatThrownBy(commandlet::doRun).isInstanceOf(CliException.class)
        .hasMessageContaining("No OpenRewrite recipes configured");
  }

  /**
   * Tests that a valid recipe name passes validation and an unknown name is rejected.
   */
  @Test
  void testRecipeNameValidation() {
    IdeTestContext context = newContext("release");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    writeRecipeConfig(context);

    commandlet.command.setValue("FORMAT_JAVA_CODE");
    assertThat(commandlet.command.validate().isValid()).isTrue();

    commandlet.command.setValue("DOES_NOT_EXIST");
    assertThat(commandlet.command.validate().isValid()).isFalse();
    assertThat(commandlet.command.validate().getErrorMessage()).contains("Unknown recipe");
  }

  /**
   * Tests that {@link RewriteCommandlet#doRun()} throws a {@link CliException} for a recipe name that
   * is not present in the settings configuration.
   */
  @Test
  void testDoRunThrowsForUnknownRecipe() {
    IdeTestContext context = newContext("basic");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    writeRecipeConfig(context);
    commandlet.command.setValue("DOES_NOT_EXIST");
    assertThatThrownBy(commandlet::doRun).isInstanceOf(CliException.class).hasMessageContaining("Unknown recipe");
  }

  /**
   * Tests that {@link RewriteCommandlet#findProjectRoot()} locates the nearest pom.xml when the current
   * working directory contains one.
   */
  @Test
  void testFindProjectRootFindsPomInCurrentDirectory() {
    IdeTestContext context = newContext("release");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    // mvn directory contains pom.xml
    Path mvnDir = context.getWorkspacePath().resolve("mvn");
    context.setCwd(mvnDir, "main", context.getIdeHome());
    Path projectRoot = commandlet.findProjectRoot();
    assertThat(projectRoot).isEqualTo(mvnDir);
  }

  /**
   * Tests that {@link RewriteCommandlet#findProjectRoot()} walks up the directory tree to find the
   * nearest pom.xml.
   */
  @Test
  void testFindProjectRootWalksUpDirectoryTree() {
    IdeTestContext context = newContext("release");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    // empty directory does NOT contain pom.xml - should walk up to find it
    Path emptyDir = context.getWorkspacePath().resolve("empty");
    context.setCwd(emptyDir, "main", context.getIdeHome());
    Path projectRoot = commandlet.findProjectRoot();
    // Should find the pom.xml in the parent workspaces/main directory or above
    assertThat(projectRoot.resolve("pom.xml")).exists();
  }

  /**
   * Tests that {@link RewriteCommandlet#findProjectRoot()} throws when cwd is not set.
   */
  @Test
  void testFindProjectRootThrowsWhenNoCwd() {
    IdeTestContext context = newContext("basic");
    RewriteCommandlet commandlet = context.getCommandletManager().getCommandlet(RewriteCommandlet.class);
    context.setCwd(null, "main", context.getIdeHome());
    assertThatThrownBy(commandlet::findProjectRoot).isInstanceOf(CliException.class).hasMessageContaining("current working directory");
  }
}
