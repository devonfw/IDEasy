package com.devonfw.tools.ide.tool.openrewrite;

import java.util.Locale;

public class RecipeWrapper {
  public String description;
  public String origin_name;
  public String url;
  public RefactorRecipeEnum ideasy_command;
  public String raw_cmd;

  //in case of future need
  public String getName() {
    return this.origin_name;
  }

}
