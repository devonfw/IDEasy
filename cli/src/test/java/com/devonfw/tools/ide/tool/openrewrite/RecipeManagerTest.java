package com.devonfw.tools.ide.tool.openrewrite;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RecipeManagerTest {

  static RecipeManager manager;

  @BeforeAll
  static void init() {
    manager = new RecipeManager();
  }

  @Test
  public void testCreation() {
    assertFalse(manager.listAvailableRecipes().isEmpty());
  }

  @Test
  public void testStringValidation() {
    assertFalse(manager.isValidRecipeNameRawName("NONSENSE"));
    assertTrue(manager.isValidRecipeNameRawName(manager.listAvailableRecipes().stream().findAny().get().originName));
  }

  @Test
  public void testEnumValidation() {
    assertFalse(manager.isValidRecipeEnum(RewriteRecipeEnum.UNRECOGNIZED_RECIPE));
    assertTrue(manager.isValidRecipeEnum(
        Arrays.stream(RewriteRecipeEnum.values())
            .filter(x -> !x.equals(RewriteRecipeEnum.UNRECOGNIZED_RECIPE)).findAny().get()));
  }
}
