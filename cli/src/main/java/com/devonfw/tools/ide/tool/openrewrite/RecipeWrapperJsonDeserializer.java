package com.devonfw.tools.ide.tool.openrewrite;

import java.io.IOException;

import com.devonfw.tools.ide.json.JsonBuilder;
import com.devonfw.tools.ide.json.JsonObjectDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * {@link JsonObjectDeserializer} for {@link RecipeWrapper}.
 */
public class RecipeWrapperJsonDeserializer extends JsonObjectDeserializer<RecipeWrapper> {

  @Override
  protected JsonBuilder<RecipeWrapper> createBuilder() {

    return new RecipeWrapperBuilder();
  }

  private class RecipeWrapperBuilder extends JsonBuilder<RecipeWrapper> {

    private String description;
    private String originName;
    private String url;
    private RewriteRecipeEnum ideasyCommand;
    private String rawCmd;

    @Override
    public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

      switch (property) {
        case "description" -> {
          this.description = readValueAsString(p, property, this.description);
        }
        case "origin_name" -> {
          this.originName = readValueAsString(p, property, this.originName);
        }
        case "url" -> {
          this.url = readValueAsString(p, property, this.url);
        }
        case "ideasy_command" -> {
          String value = readValueAsString(p, property, null);
          if (value != null) {
            try {
              this.ideasyCommand = RewriteRecipeEnum.valueOf(value);
            } catch (IllegalArgumentException e) {
              this.ideasyCommand = RewriteRecipeEnum.UNRECOGNIZED_RECIPE;
            }
          }
        }
        case "raw_cmd" -> {
          this.rawCmd = readValueAsString(p, property, this.rawCmd);
        }
        default -> {
          super.setProperty(property, p, ctxt);
        }
      }
    }

    @Override
    public RecipeWrapper build() {

      return new RecipeWrapper(this.description, this.originName, this.url, this.ideasyCommand, this.rawCmd);
    }
  }
}
