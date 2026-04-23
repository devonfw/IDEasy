package com.devonfw.tools.ide.tool.openrewrite;

import com.devonfw.tools.ide.json.JsonMapping;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


public class RecipeManager {

  private static final String OPEN_REWRITE_CONFIG_JSON_PATH = "refactor/openrewrite.json";
  private final Map<RefactorRecipeEnum, RecipeWrapper> recipes = new HashMap<>();


  public RecipeManager() {
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(
          Objects.requireNonNull(RecipeManager.class.getClassLoader().getResourceAsStream(OPEN_REWRITE_CONFIG_JSON_PATH)), StandardCharsets.UTF_8));
      ObjectMapper objectMapper = JsonMapping.create();

      List<RecipeWrapper> wrapperList  = objectMapper.readValue(reader, objectMapper.getTypeFactory().constructCollectionType(List.class, RecipeWrapper.class));

      for(RecipeWrapper one: wrapperList) {
        recipes.put(one.ideasy_command, one);
      }


    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public List<RecipeWrapper> listAvailableRecipes() {
    return Collections.unmodifiableList(recipes.values().stream().toList());
  }

  private Optional<RecipeWrapper> findRecipeByName(String rawName) {
    return recipes.values().stream().filter(x -> x.origin_name.equals(rawName)).findAny();
  }

  public boolean isValidRecipeNameRawName(String rawName) {
    return findRecipeByName(rawName).isPresent();
  }

  public boolean isValidRecipeEnum(RefactorRecipeEnum recipeEnum) {
    return recipes.containsKey(recipeEnum);
  }

  public RecipeWrapper getRecipeWrapper(RefactorRecipeEnum recipeEnum) {
    return recipes.get(recipeEnum);
  }

}
