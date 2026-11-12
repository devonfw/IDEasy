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
          "id": "java.format_code",
          "description": "Format Java code.",
          "url": "https://docs.openrewrite.org/recipes/java/format/autoformat",
          "properties": {
            "rewrite.activeRecipes": "org.openrewrite.java.format.AutoFormat"
          }
        },
        {
          "id": "java.blank_lines",
          "description": "Add and/or remove blank lines.",
          "url": "https://docs.openrewrite.org/recipes/java/format/blanklines",
          "properties": {
            "rewrite.activeRecipes": "org.openrewrite.java.format.BlankLines"
          }
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
    assertThat(manager.getRecipeNames()).containsExactlyInAnyOrder("java.format_code", "java.blank_lines");
    assertThat(manager.hasRecipe("java.format_code")).isTrue();
    assertThat(manager.hasRecipe("DOES_NOT_EXIST")).isFalse();

    RecipeWrapper wrapper = manager.getRecipe("java.format_code");
    assertThat(wrapper).isNotNull();
    assertThat(wrapper.id).isEqualTo("java.format_code");
    assertThat(wrapper.description).isEqualTo("Format Java code.");
    assertThat(wrapper.properties)
        .containsEntry("rewrite.activeRecipes", "org.openrewrite.java.format.AutoFormat");

    // unknown id yields no recipe
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
    assertThat(manager.hasRecipe("java.format_code")).isFalse();
    assertThat(manager.getRecipe("java.format_code")).isNull();
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
