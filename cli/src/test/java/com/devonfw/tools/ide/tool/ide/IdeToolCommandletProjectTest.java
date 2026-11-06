package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.intellij.Intellij;

/**
 * Tests the {@link AbstractIdeToolCommandlet#getOpenPath() open path} resolution of the {@code --project} flag of
 * {@link AbstractIdeToolCommandlet}: an explicit absolute folder, a relative folder resolved against the current working directory, the default fallback to the
 * managed workspace, and the invalid-path guard that aborts the launch. Because the flag is remembered on the {@code AbstractIdeToolCommandlet}
 * (the {@code project} field is package-private to the {@code tool.ide} package), these foundation tests live in this package. The Intellij-specific launch
 * behavior is covered by {@link com.devonfw.tools.ide.tool.intellij.IntellijTest}.
 */
class IdeToolCommandletProjectTest extends AbstractIdeContextTest {

  /**
   * Tests that {@link AbstractIdeToolCommandlet#getOpenPath()} returns an explicitly configured absolute {@code --project} path.
   */
  @Test
  void testGetOpenPathAbsoluteProject() {

    // arrange
    IdeTestContext context = newContext("intellij");
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    Path external = Path.of("/external/some-project");
    intellij.project.setValue(external);
    // act
    Path openPath = intellij.getOpenPath();
    // assert
    assertThat(openPath).isEqualTo(external);
    assertThat(openPath).isNotEqualTo(context.getWorkspacePath());
  }

  /**
   * Tests that a relative {@code --project} path is resolved against the current working directory.
   */
  @Test
  void testGetOpenPathRelativeProject() {

    // arrange
    IdeTestContext context = newContext("intellij");
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    Path cwd = context.getCwd();
    Path expected = cwd.resolve("some-project").normalize();
    intellij.project.setValue(Path.of("some-project"));
    // act
    Path openPath = intellij.getOpenPath();
    // assert
    assertThat(openPath).isEqualTo(expected);
  }

  /**
   * Tests that without a {@code --project} flag {@link AbstractIdeToolCommandlet#getOpenPath()} falls back to the
   * {@link com.devonfw.tools.ide.context.IdeContext#getWorkspacePath() workspace path} (regression guard).
   */
  @Test
  void testGetOpenPathWithoutProjectIsWorkspacePath() {

    // arrange
    IdeTestContext context = newContext("intellij");
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    // act
    Path openPath = intellij.getOpenPath();
    // assert
    assertThat(openPath).isEqualTo(context.getWorkspacePath());
  }

  /**
   * Tests that an invalid {@code --project} path (non-existing folder or a file) results in a {@link CliException} so the IDE is not started.
   */
  @Test
  void testGetOpenPathInvalidPathThrows() {

    // arrange
    IdeTestContext context = newContext("intellij");
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    intellij.project.setValue(Path.of("does-not-exist-folder-12345"));
    // act & assert - a non-existing path
    assertThatThrownBy(() -> intellij.runTool(ProcessMode.BACKGROUND, null, new ArrayList<>())).isInstanceOf(CliException.class);
    // act & assert - a file is not a folder
    Path file = context.getWorkspacePath().resolve("a-file.txt");
    context.getFileAccess().writeFileContent("content", file);
    intellij.project.setValue(file);
    assertThatThrownBy(() -> intellij.runTool(ProcessMode.BACKGROUND, null, new ArrayList<>())).isInstanceOf(CliException.class);
  }
}
