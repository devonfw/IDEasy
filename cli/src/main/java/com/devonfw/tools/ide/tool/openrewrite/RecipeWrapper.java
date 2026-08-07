package com.devonfw.tools.ide.tool.openrewrite;

import com.devonfw.tools.ide.json.JsonObject;

public class RecipeWrapper implements JsonObject {

  public String description;
  public String originName;
  public String url;
  public RewriteRecipeEnum ideasyCommand;
  public String rawCmd;

  public RecipeWrapper(String description, String originName, String url, RewriteRecipeEnum ideasyCommand, String rawCmd) {
    this.description = description;
    this.originName = originName;
    this.url = url;
    this.ideasyCommand = ideasyCommand;
    this.rawCmd = rawCmd;
  }

  //in case of future need
  public String getName() {
    return this.originName;
  }

}
