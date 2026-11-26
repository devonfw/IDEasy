package com.devonfw.tools.ide.tool.openrewrite;

import java.util.Map;

import com.devonfw.tools.ide.json.JsonObject;

/**
 * The OpenRewrite recipe configuration as defined in the {@code rewrite/openrewrite.json} file of the {@code ide-settings} settings folder.
 */
public class RecipeWrapper implements JsonObject {

  /** The identifier of the recipe, used by the user to select it (and as the auto-completion candidate). */
  public String id;

  public String description;

  public String url;

  /** The {@code rewrite.*} properties passed to the OpenRewrite Maven plugin as {@code -D} options. */
  public Map<String, String> properties;

  public RecipeWrapper(String id, String description, String url, Map<String, String> properties) {

    this.id = id;
    this.description = description;
    this.url = url;
    this.properties = properties;
  }

}
