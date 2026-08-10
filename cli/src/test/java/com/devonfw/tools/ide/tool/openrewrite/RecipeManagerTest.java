package com.devonfw.tools.ide.tool.openrewrite;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RecipeManager}.
 */
class RecipeManagerTest extends Assertions {

  static RecipeManager manager;

  @BeforeAll
  static void init() {
    manager = new RecipeManager();
  }

  @Test
  void testCreation() {
    assertThat(manager.listAvailableRecipes()).isNotEmpty();
  }

  @Test
  void testStringValidation() {
    assertThat(manager.isValidRecipeNameRawName("NONSENSE")).isFalse();
    var anyRecipe = manager.listAvailableRecipes().stream().findAny().get();
    assertThat(manager.isValidRecipeNameRawName(anyRecipe.originName)).isTrue();
  }

  @Test
  void testEnumValidation() {
    assertThat(manager.isValidRecipeEnum(RewriteRecipeEnum.UNRECOGNIZED_RECIPE)).isFalse();
    var validRecipe = java.util.Arrays.stream(RewriteRecipeEnum.values())
        .filter(x -> x != RewriteRecipeEnum.UNRECOGNIZED_RECIPE).findAny().get();
    assertThat(manager.isValidRecipeEnum(validRecipe)).isTrue();
  }
}
