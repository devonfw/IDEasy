package com.devonfw.tools.ide.merge;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * Test of {@link JsonMerger}.
 */
class JsonMergerTest extends AbstractIdeContextTest {

  /**
   * Test that {@link JsonMerger} can merge a workspace file containing JSONC (JSON with Comments).
   * VSCode settings files routinely contain single-line ({@code //}) and block comments.
   *
   * @param tempDir the temporary folder to use as workspace for this test.
   * @throws Exception on error.
   */
  @Test
  void testMergeJsoncWithComments(@TempDir Path tempDir) throws Exception {

    // arrange
    IdeContext context = newContext(PROJECT_BASIC, null, false);
    JsonMerger jsonMerger = new JsonMerger(context);

    Path workspaceJson = tempDir.resolve("settings.json");
    Files.writeString(workspaceJson, """
        {
          // Single-line comment
          "editor.fontSize": 12,
          /* Block comment */
          "editor.tabSize": 4
        }
        """);

    Path updateJson = tempDir.resolve("update.json");
    Files.writeString(updateJson, """
        {
          "editor.tabSize": 2,
          "editor.wordWrap": "on"
        }
        """);

    // act
    jsonMerger.merge(null, updateJson, context.getVariables(), workspaceJson);

    // assert
    String result = Files.readString(workspaceJson);
    assertThat(result).contains("\"editor.fontSize\"");
    assertThat(result).contains("\"editor.tabSize\": 2");
    assertThat(result).contains("\"editor.wordWrap\": \"on\"");
    // comments from the original workspace file must be preserved above their associated property
    assertThat(result).contains("// Single-line comment");
    assertThat(result).contains("/* Block comment */");
    assertThat(result.indexOf("// Single-line comment")).isLessThan(result.indexOf("\"editor.fontSize\""));
    assertThat(result.indexOf("/* Block comment */")).isLessThan(result.indexOf("\"editor.tabSize\""));
  }
}
