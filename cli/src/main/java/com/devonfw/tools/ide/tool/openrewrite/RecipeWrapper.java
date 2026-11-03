package com.devonfw.tools.ide.tool.openrewrite;

import com.devonfw.tools.ide.json.JsonObject;

public class RecipeWrapper implements JsonObject {

  public String description;
  public String originName;
  public String url;
  public String ideasyCommand;
  public String rawCmd;

  public RecipeWrapper(String description, String originName, String url, String ideasyCommand, String rawCmd) {
    this.description = description;
    this.originName = originName;
    this.url = url;
    this.ideasyCommand = ideasyCommand;
    this.rawCmd = rawCmd;
  }

}
