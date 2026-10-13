package com.devonfw.tools.ide.merge;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesPropertiesMock;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;

/**
 * Test of {@link IniMerger}.
 */
class IniMergerTest extends AbstractIdeContextTest {

  /**
   * Creates a clean {@link EnvironmentVariables} instance (same approach as {@code XmlMergerTest}) so that {@code resolve} does not wrap plain values,
   * allowing the test to assert on the exact merged values.
   *
   * @param context the {@link IdeTestContext} to base the mock variables on.
   * @return the clean {@link EnvironmentVariables} to use for merging.
   */
  private EnvironmentVariables cleanVariables(IdeTestContext context) {

    EnvironmentVariablesPropertiesMock mockVariables = new EnvironmentVariablesPropertiesMock(null, EnvironmentVariablesType.SETTINGS, context);
    return mockVariables.resolved();
  }

  /**
   * Tests that {@link IniMerger#merge} merges the update template into the existing workspace: values that exist in both are overridden by the update template,
   * workspace-only values are preserved (user modifications), and update-only values are added.
   *
   * @param tempDir the temporary folder to use as workspace for this test.
   * @throws Exception on error.
   */
  @Test
  void testMergeOverridesSharedKeepsWorkspaceAndAddsUpdateKeys(@TempDir Path tempDir) throws Exception {

    // arrange
    IdeTestContext context = new IdeTestContext();
    EnvironmentVariables variables = cleanVariables(context);
    IniMerger iniMerger = new IniMerger(context);

    Path workspaceIni = tempDir.resolve("settings.ini");
    Files.writeString(workspaceIni, """
        [editor]
        editor.fontSize = 12
        editor.tabSize = 4
        """);

    Path updateIni = tempDir.resolve("update.ini");
    Files.writeString(updateIni, """
        [editor]
        editor.tabSize = 2
        editor.wordWrap = on
        """);

    // act
    iniMerger.merge(null, updateIni, variables, workspaceIni);

    // assert
    String result = Files.readString(workspaceIni);
    // update template wins over the workspace for the shared key
    assertThat(result).contains("editor.tabSize = 2");
    // workspace-only value is preserved (not overwritten, not dropped)
    assertThat(result).contains("editor.fontSize = 12");
    // update-only value is added to the workspace
    assertThat(result).contains("editor.wordWrap = on");
  }
}
