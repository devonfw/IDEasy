package com.devonfw.tools.ide.merge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;

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
    // the answers are consumed in the order the questions are asked, i.e. the order the keys are resolved in the template
    context.setAnswers("dummy-secret-value", "http://llama.local");
    DirectoryMerger merger = context.getWorkspaceMerger();
    Path templates = TEST_RESOURCES.resolve("templates-expression");

    // act
    merger.merge(templates.resolve(IdeContext.FOLDER_SETUP), templates.resolve(IdeContext.FOLDER_UPDATE), context.getVariables(), workspaceDir);

    // assert
    Properties properties = context.getFileAccess().readProperties(workspaceDir.resolve("config/ai.properties"));
    assertThat(properties.getProperty("api.key")).isEqualTo("dummy-secret-value");
    assertThat(properties.getProperty("backend.url")).isEqualTo("http://llama.local");
    // foreign syntax must never be resolved by IDEasy
    assertThat(properties.getProperty("css.rule")).isEqualTo("@media(max-width:600px)");
    // @path normalises the backslashes of a windows IDE_HOME
    assertThat(properties.getProperty("node.path")).endsWith("/software/node/node").doesNotContain("\\");

    // the values are persisted so that the user is only asked once
    Path confProperties = context.getIdeHome().resolve("conf").resolve("ide.properties");
    assertThat(confProperties).exists();
    String conf = Files.readString(confProperties);
    assertThat(conf).contains("AI_API_KEY=dummy-secret-value");
    assertThat(conf).contains("AI_BACKEND_URL=http://llama.local");
  }

  /**
   * Test that an invalid expression in a workspace template is reported with a readable message and without a stacktrace, since it is an authoring error in the
   * settings and not a technical error of IDEasy.
   *
   * @param workspaceDir the temporary folder to use as workspace for this test.
   */
  @Test
  void testInvalidExpressionIsReportedWithoutStacktrace(@TempDir Path workspaceDir) {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, true);
    DirectoryMerger merger = context.getWorkspaceMerger();
    Path templates = TEST_RESOURCES.resolve("templates-expression-invalid");

    // act
    merger.merge(templates.resolve(IdeContext.FOLDER_SETUP), templates.resolve(IdeContext.FOLDER_UPDATE), context.getVariables(), workspaceDir);

    // assert
    List<IdeLogEntry> errors = context.getTestStartContext().getEntries().stream().filter(e -> e.level() == IdeLogLevel.ERROR).toList();
    assertThat(errors).isNotEmpty();
    IdeLogEntry error = errors.get(0);
    assertThat(error.message()).contains("invalid mode 'dos'");
    // no exception is attached to the log entry, so no stacktrace is printed for the end-user
    assertThat(error.error()).isNull();
  }

}
