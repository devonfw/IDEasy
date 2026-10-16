package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.WindowsSymlinkTestHelper;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.tool.intellij.Intellij;

/**
 * Test of {@link AbstractIdeToolCommandlet}.
 */
class IdeToolCommandletTest extends AbstractIdeContextTest {

  /**
   * Tests that launching an IDE configures its workspace exactly once.
   *
   * @param installed whether the IDE is already installed before launch.
   */
  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void testConfigureWorkspace(boolean installed) {

    WindowsSymlinkTestHelper.assumeSymlinksSupported();
    // arrange
    IdeTestContext context = newContext("intellij");
    Path workspace = context.getWorkspacePath();
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    if (installed) {
      intellij.install();
      assertThat(workspace.resolve(".editorconfig")).exists();
      context.getTestStartContext().getEntries().clear();
    }
    // act
    intellij.run();
    // assert
    assertThat(context.getTestStartContext().getEntries()).extracting(IdeLogEntry::message)
        .containsOnlyOnce("Start: Configuring workspace main for IDE intellij");
    assertThat(workspace.resolve(".editorconfig")).exists();
    assertThat(workspace.resolve(".intellij/config/idea.key")).exists();
    assertThat(workspace.resolve("user.properties")).exists().content().contains("ijversion=2023.3.3");
  }

  /**
   * Tests that {@link AbstractIdeToolCommandlet#getIdeMetadataPath()} resolves to {@code $IDE_HOME/.ide/«ide»/«workspace»} instead of the workspace itself.
   */
  @Test
  void testGetIdeMetadataPath() {
    // arrange
    IdeContext context = newContext("intellij");
    AbstractIdeToolCommandlet ide = context.getCommandletManager().getCommandlet(Intellij.class);
    // act
    Path metadataPath = ide.getIdeMetadataPath();
    // assert
    assertThat(metadataPath).startsWithRaw(context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE));
    assertThat(metadataPath).endsWithRaw(Path.of("intellij", context.getWorkspaceName()));
    assertThat(metadataPath).isNotEqualTo(context.getWorkspacePath());
  }
}
