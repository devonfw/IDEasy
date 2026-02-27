package com.devonfw.tools.ide.tool.openrewrite;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

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
    assertTrue(manager.isValidRecipeNameRawName(manager.listAvailableRecipes().stream().findAny().get().origin_name));
  }

  @Test
  public void testEnumValidation() {
    assertFalse(manager.isValidRecipeEnum(RefactorRecipeEnum.UNRECOGNIZED_RECIPE));
    assertTrue(manager.isValidRecipeEnum(
        Arrays.stream(RefactorRecipeEnum.values())
        .filter(x -> !x.equals(RefactorRecipeEnum.UNRECOGNIZED_RECIPE)).findAny().get()));
  }
}
