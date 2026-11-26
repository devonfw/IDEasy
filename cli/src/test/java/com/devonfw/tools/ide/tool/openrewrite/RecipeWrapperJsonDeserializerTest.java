package com.devonfw.tools.ide.tool.openrewrite;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.json.JsonMapping;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for {@link RecipeWrapperJsonDeserializer}.
 */
class RecipeWrapperJsonDeserializerTest extends Assertions {

  /**
   * Tests that a {@link RecipeWrapper} is deserialized from JSON carrying an {@code id} and a
   * {@code properties} object.
   *
   * @throws Exception on error.
   */
  @Test
  void testDeserializeWithProperties() throws Exception {
    ObjectMapper mapper = JsonMapping.create();
    String json = """
        {
          "id": "java.format_code",
          "description": "Format Java code.",
          "url": "https://docs.openrewrite.org/recipes/java/format/autoformat",
          "properties": {
            "rewrite.activeRecipes": "org.openrewrite.java.format.AutoFormat",
            "rewrite.exportDatatables": "true"
          }
        }
        """;

    RecipeWrapper wrapper = mapper.readValue(json, RecipeWrapper.class);

    assertThat(wrapper.id).isEqualTo("java.format_code");
    assertThat(wrapper.description).isEqualTo("Format Java code.");
    assertThat(wrapper.url).isEqualTo("https://docs.openrewrite.org/recipes/java/format/autoformat");
    assertThat(wrapper.properties)
        .containsEntry("rewrite.activeRecipes", "org.openrewrite.java.format.AutoFormat")
        .containsEntry("rewrite.exportDatatables", "true")
        .hasSize(2);
  }

  /**
   * Tests that a {@link RecipeWrapper} without a {@code properties} object is deserialized with an empty
   * properties map (an empty property set is a valid recipe).
   *
   * @throws Exception on error.
   */
  @Test
  void testDeserializeWithoutProperties() throws Exception {
    ObjectMapper mapper = JsonMapping.create();
    String json = """
        {
          "id": "java.format_code",
          "description": "Format Java code."
        }
        """;

    RecipeWrapper wrapper = mapper.readValue(json, RecipeWrapper.class);

    assertThat(wrapper.id).isEqualTo("java.format_code");
    assertThat(wrapper.properties).isNotNull().isEmpty();
  }
}
