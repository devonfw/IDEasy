package com.devonfw.tools.ide.tool.openrewrite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.json.JsonMapping;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Manages the loading and lookup of OpenRewrite {@link RecipeWrapper} configurations from the
 * {@code rewrite/openrewrite.json} file in the {@code ide-settings} settings folder.
 * <p>
 * The location of the settings file is resolved lazily (on first query) via the given
 * {@link Supplier}, because the commandlet is constructed before the {@code IdeContext} has resolved
 * its settings folder. Loading is also tolerant: a missing settings file (e.g. a fresh install that
 * has not pulled the settings folder yet) yields an empty set rather than an error, so the commandlet
 * can be constructed for {@code ide help} and auto-completion without a settings file present.
 */
public class RecipeManager {

  private final Supplier<Path> configPathProvider;

  private Map<String, RecipeWrapper> recipes = Collections.emptyMap();

  private boolean loaded;

  private boolean missing;

  /**
   * The constructor.
   *
   * @param configPathProvider the {@link Supplier} resolving the {@link Path} to the
   *     {@code rewrite/openrewrite.json} settings file on first use, or {@code null} if no settings
   *     folder is available.
   */
  public RecipeManager(Supplier<Path> configPathProvider) {
    this.configPathProvider = configPathProvider;
  }

  /**
   * @return the currently resolved {@link Path} to the settings file, or {@code null} if none.
   */
  private Path resolveConfigPath() {

    return this.configPathProvider == null ? null : this.configPathProvider.get();
  }

  /**
   * Loads the recipes from the configured settings file (idempotent).
   */
  private synchronized void load() {

    if (this.loaded || this.missing) {
      return;
    }
    Path configPath = resolveConfigPath();
    if ((configPath == null) || !Files.exists(configPath)) {
      this.recipes = Collections.emptyMap();
      this.missing = true;
      this.loaded = true;
      return;
    }
    try (InputStream is = Files.newInputStream(configPath)) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      ObjectMapper objectMapper = JsonMapping.create();
      List<RecipeWrapper> wrapperList = objectMapper.readValue(reader,
          objectMapper.getTypeFactory().constructCollectionType(List.class, RecipeWrapper.class));
      Map<String, RecipeWrapper> map = new LinkedHashMap<>();
      for (RecipeWrapper one : wrapperList) {
        if ((one.ideasyCommand != null) && !one.ideasyCommand.isBlank()) {
          map.put(one.ideasyCommand, one);
        }
      }
      this.recipes = Collections.unmodifiableMap(map);
    } catch (IOException e) {
      throw new CliException("Failed to load OpenRewrite recipes from " + configPath, e);
    }
    this.loaded = true;
  }

  /**
   * @return {@code true} if the settings file was present and the recipes were loaded, {@code false}
   *     otherwise (no settings file / no settings folder).
   */
  public boolean isLoaded() {
    load();
    return !this.missing;
  }

  /**
   * @return the {@link Path} to the configured settings file, or {@code null} if none was configured.
   */
  public Path getConfigPath() {

    return resolveConfigPath();
  }

  /**
   * @return an unmodifiable list of the loaded recipes.
   */
  public List<RecipeWrapper> listAvailableRecipes() {
    load();
    return List.copyOf(this.recipes.values());
  }

  /**
   * @return the recipe names (the {@code ideasy_command} keys) of the loaded recipes.
   */
  public List<String> getRecipeNames() {
    load();
    return List.copyOf(this.recipes.keySet());
  }

  /**
   * Checks whether a recipe with the given name has been loaded.
   *
   * @param recipeName the recipe name ({@code ideasy_command} key).
   * @return {@code true} if a matching recipe was loaded.
   */
  public boolean hasRecipe(String recipeName) {
    load();
    return this.recipes.containsKey(recipeName);
  }

  /**
   * @param recipeName the recipe name ({@code ideasy_command} key).
   * @return the {@link RecipeWrapper} for the given name, or {@code null} if not loaded.
   */
  public RecipeWrapper getRecipe(String recipeName) {
    load();
    return this.recipes.get(recipeName);
  }
}
