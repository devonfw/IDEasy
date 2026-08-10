package com.devonfw.tools.ide.tool.openrewrite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
        Objects.requireNonNull(RecipeManager.class.getClassLoader().getResourceAsStream(OPEN_REWRITE_CONFIG_JSON_PATH)), StandardCharsets.UTF_8))) {
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

  private Optional<RecipeWrapper> findRecipeByName(String rawName) {
    return recipes.values().stream().filter(x -> x.originName.equals(rawName)).findFirst();
  }

  /**
   * Checks if a recipe with the given original OpenRewrite name exists in the configuration.
   *
   * @param rawName the original recipe name (e.g. {@code "java.format_autoformat"}).
   * @return {@code true} if a matching recipe was found.
   */
  public boolean isValidRecipeNameRawName(String rawName) {
    return findRecipeByName(rawName).isPresent();
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
