package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.intellij.Intellij;

/**
 * Test of the {@code --project} flag and the {@link IdeToolCommandlet#getOpenPath() open path} foundation of
 * {@link IdeToolCommandlet}.
 */
class IdeToolCommandletProjectTest extends AbstractIdeContextTest {

  /**
   * Tests that {@link IdeToolCommandlet#getOpenPath()} returns an explicitly configured absolute {@code --project} path.
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
   * Tests that without a {@code --project} flag {@link IdeToolCommandlet#getOpenPath()} falls back to the
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

  /**
   * Tests that the {@code --project} flag is consumed by IDEasy and not passed through to the IDE binary (the foundation behavior). Pointing the actual launch
   * arguments at the folder is covered separately. The flag arrives in the multi-valued passthrough {@code args} of the tool commandlet, so IDEasy extracts and
   * remembers it instead of forwarding it to the IDE.
   */
  @Test
  void testIntellijRunWithProjectFlagConsumedByIdeasy() {

    // arrange
    IdeTestContext context = newContext("intellij");
    context.setSystemInfo(SystemInfoMock.of("linux"));
    Path external = context.getIdeHome().resolve("external-project").normalize();
    context.getFileAccess().mkdirs(external);
    // act
    CliArguments args = new CliArguments("intellij", "--project", external.toString());
    args.next();
    int exitCode = context.run(args);
    // assert
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    assertThat(exitCode).isEqualTo(0);
    // IDEasy consumed the flag and remembered the folder
    assertThat(intellij.project.getValue()).isEqualTo(external);
    assertThat(intellij.getOpenPath()).isEqualTo(external);
    // the flag and its value were stripped from the arguments passed to the IDE binary
    String launchedArgs = context.getFileAccess().readFileContent(intellij.getToolBinPath().resolve("intellijtest")).trim();
    assertThat(launchedArgs).doesNotContain("--project");
    assertThat(launchedArgs).doesNotContain(external.toString());
  }
}
