package com.devonfw.tools.ide.tool.openrewrite;

import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.cli.CliException;

/**
 * Tests for {@link RecipeManager}.
 */
class RecipeManagerTest extends Assertions {

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

  private Path writeConfig(Path dir) throws Exception {
    Path configPath = dir.resolve("openrewrite.json");
    Files.writeString(configPath, RECIPE_JSON);
    return configPath;
  }

  /**
   * Tests that recipes are loaded and lookable up by their recipe name.
   *
   * @param tempDir the temporary directory holding the config file.
   * @throws Exception on I/O errors.
   */
  @Test
  void testLoadRecipes(@TempDir Path tempDir) throws Exception {
    Path path = writeConfig(tempDir);
    RecipeManager manager = new RecipeManager(() -> path);
    assertThat(manager.isLoaded()).isTrue();
    assertThat(manager.listAvailableRecipes()).hasSize(2);
    assertThat(manager.getRecipeNames()).containsExactlyInAnyOrder("FORMAT_JAVA_CODE", "REMOVE_BLANK_LINES");
    assertThat(manager.hasRecipe("FORMAT_JAVA_CODE")).isTrue();
    assertThat(manager.hasRecipe("DOES_NOT_EXIST")).isFalse();

    RecipeWrapper wrapper = manager.getRecipe("FORMAT_JAVA_CODE");
    assertThat(wrapper).isNotNull();
    assertThat(wrapper.rawCmd).contains(":run").startsWith("mvn");
    assertThat(wrapper.description).isEqualTo("Format Java code.");

    // unknown name yields no recipe
    assertThat(manager.getRecipe("DOES_NOT_EXIST")).isNull();
  }

  /**
   * Tests that a missing config file does not throw and yields an empty, not-loaded manager.
   *
   * @param tempDir the temporary directory (no config file present).
   */
  @Test
  void testMissingConfigDoesNotThrow(@TempDir Path tempDir) {
    Path configPath = tempDir.resolve("rewrite").resolve("openrewrite.json");
    RecipeManager manager = new RecipeManager(() -> configPath);
    assertThat(manager.isLoaded()).isFalse();
    assertThat(manager.listAvailableRecipes()).isEmpty();
    assertThat(manager.getRecipeNames()).isEmpty();
    assertThat(manager.hasRecipe("FORMAT_JAVA_CODE")).isFalse();
    assertThat(manager.getRecipe("FORMAT_JAVA_CODE")).isNull();
  }

  /**
   * Tests that a null config path yields an empty, not-loaded manager.
   */
  @Test
  void testNullPathDoesNotThrow() {
    RecipeManager manager = new RecipeManager(null);
    assertThat(manager.isLoaded()).isFalse();
    assertThat(manager.listAvailableRecipes()).isEmpty();
    assertThat(manager.getRecipeNames()).isEmpty();
  }

  /**
   * Tests that an unparseable config file is reported as a {@link CliException}.
   *
   * @param tempDir the temporary directory holding the malformed config file.
   * @throws Exception on I/O errors.
   */
  @Test
  void testMalformedConfigThrows(@TempDir Path tempDir) throws Exception {
    Path configPath = tempDir.resolve("openrewrite.json");
    Files.writeString(configPath, "{ not valid json for a recipe list");
    RecipeManager manager = new RecipeManager(() -> configPath);
    assertThatThrownBy(manager::isLoaded).isInstanceOf(CliException.class);
  }
}
