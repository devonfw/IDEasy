package com.devonfw.tools.ide.merge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Integration test of expressions (see {@link com.devonfw.tools.ide.expression.ExpressionParser}) applied to a workspace template by the
 * {@link DirectoryMerger}.
 */
class DirectoryMergerExpressionTest extends AbstractIdeContextTest {

  /**
   * Test that expressions in a workspace template are resolved, that the user is asked for undefined variables and that the entered values are persisted to
   * {@code conf/ide.properties}.
   *
   * @param workspaceDir the temporary folder to use as workspace for this test.
   * @throws Exception on error.
   */
  @Test
  void testExpressionsInWorkspaceTemplate(@TempDir Path workspaceDir) throws Exception {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, true);
    // NOTE: the answers are consumed in the order the questions are asked. PropertiesMerger iterates the Properties
    // and therefore does not preserve the order of the lines in the template file.
    context.setAnswers("sk-TOPSECRET", "http://llama.local");
    DirectoryMerger merger = context.getWorkspaceMerger();
    Path templates = Path.of("src/test/resources/templates-expression");

    // act
    merger.merge(templates.resolve(IdeContext.FOLDER_SETUP), templates.resolve(IdeContext.FOLDER_UPDATE), context.getVariables(), workspaceDir);

    // assert
    Properties properties = context.getFileAccess().readProperties(workspaceDir.resolve("config/ai.properties"));
    assertThat(properties.getProperty("api.key")).isEqualTo("sk-TOPSECRET");
    assertThat(properties.getProperty("backend.url")).isEqualTo("http://llama.local");
    // foreign syntax must never be resolved by IDEasy
    assertThat(properties.getProperty("css.rule")).isEqualTo("@media(max-width:600px)");
    // @path normalises the backslashes of a windows IDE_HOME
    assertThat(properties.getProperty("node.path")).endsWith("/software/node/node").doesNotContain("\\");

    // the values are persisted so that the user is only asked once
    Path confProperties = context.getIdeHome().resolve("conf").resolve("ide.properties");
    assertThat(confProperties).exists();
    String conf = Files.readString(confProperties);
    assertThat(conf).contains("AI_API_KEY=sk-TOPSECRET");
    assertThat(conf).contains("AI_BACKEND_URL=http://llama.local");
  }
}
