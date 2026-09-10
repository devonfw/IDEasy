package com.devonfw.tools.ide.tool.openrewrite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.json.JsonMapping;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Manages the loading and lookup of OpenRewrite {@link RecipeWrapper} configurations from JSON.
 */
public class RecipeManager {

  private static final String OPEN_REWRITE_CONFIG_JSON_PATH = "refactor/openrewrite.json";

  private final Map<RewriteRecipeEnum, RecipeWrapper> recipes;

  public RecipeManager() {
    InputStream is = RecipeManager.class.getClassLoader().getResourceAsStream(OPEN_REWRITE_CONFIG_JSON_PATH);
    if (is == null) {
      throw new CliException("Failed to load " + OPEN_REWRITE_CONFIG_JSON_PATH + " from classpath");
    }
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      ObjectMapper objectMapper = JsonMapping.create();
      List<RecipeWrapper> wrapperList = objectMapper.readValue(reader, objectMapper.getTypeFactory().constructCollectionType(List.class, RecipeWrapper.class));

      java.util.LinkedHashMap<RewriteRecipeEnum, RecipeWrapper> map = new java.util.LinkedHashMap<>();
      for (RecipeWrapper one : wrapperList) {
        map.put(one.ideasyCommand, one);
      }
      this.recipes = java.util.Collections.unmodifiableMap(map);
    } catch (IOException e) {
      throw new CliException("Failed to load " + OPEN_REWRITE_CONFIG_JSON_PATH, e);
    }
  }

  /**
   * Returns the list of all loaded {@link RecipeWrapper} configurations.
   *
   * @return an unmodifiable list of available recipes.
   */
  public List<RecipeWrapper> listAvailableRecipes() {

    return List.copyOf(recipes.values());
  }

  /**
   * Checks if a recipe for the given {@link RewriteRecipeEnum} exists in the configuration.
   *
   * @param recipeEnum the enum constant identifying the recipe.
   * @return {@code true} if a matching recipe was found.
   */
  public boolean isValidRecipeEnum(RewriteRecipeEnum recipeEnum) {
    return recipes.containsKey(recipeEnum);
  }

  /**
   * Returns the {@link RecipeWrapper} for the given {@link RewriteRecipeEnum}.
   *
   * @param recipeEnum the enum constant identifying the recipe.
   * @return the recipe wrapper.
   */
  public RecipeWrapper getRecipeWrapper(RewriteRecipeEnum recipeEnum) {
    return recipes.get(recipeEnum);
  }
}
