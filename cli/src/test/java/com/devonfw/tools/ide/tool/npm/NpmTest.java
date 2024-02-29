package com.devonfw.tools.ide.tool.npm;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.CommandLetExtractorMock;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.repo.ToolRepositoryMock;

public class NpmTest extends AbstractIdeContextTest {

    @Test
  public void npmPostInstallShouldMoveFiles() throws IOException {
      // arrange
      String path = "workspaces/foo-test/my-git-repo";
      String projectTestCaseName = "npm";

      ToolRepositoryMock toolRepositoryMock = buildToolRepositoryMockForNpm(projectTestCaseName);

      IdeContext context = newContext(projectTestCaseName, path, true, toolRepositoryMock);
      toolRepositoryMock.setContext(context);

      CommandLetExtractorMock commandLetExtractorMock = new CommandLetExtractorMock(context);
      Npm commandlet = new Npm(context);
      commandlet.setCommandletFileExtractor(commandLetExtractorMock);

      assertThat(context.getSoftwarePath().resolve("node/npm")).hasContent("# This is npm");
      assertThat(context.getSoftwarePath().resolve("node/npx")).hasContent("# This is npx");

      // act
      commandlet.install();

      // assert
      String expectedMessage = "Successfully installed npm in version 9.9.2";
      assertLogMessage((IdeTestContext) context, IdeLogLevel.SUCCESS, expectedMessage, false);
      if (context.getSystemInfo().isWindows()) {
        Path test = context.getSoftwarePath();
        assertThat(context.getSoftwarePath().resolve("node/npm")).exists();
        assertThat(context.getSoftwarePath().resolve("node/npm.cmd")).exists();
        assertThat(context.getSoftwarePath().resolve("node/npx")).exists();
        assertThat(context.getSoftwarePath().resolve("node/npx.cmd")).exists();

        assertThat(context.getSoftwarePath().resolve("node/npm")).hasContent("# This is npm bin");
        assertThat(context.getSoftwarePath().resolve("node/npx")).hasContent("# This is npx bin");
      }
  }

  private static ToolRepositoryMock buildToolRepositoryMockForNpm(String projectTestCaseName) {

    String windowsFileFolder = "npm-9.9.2";

    ToolRepositoryMock toolRepositoryMock = new ToolRepositoryMock("npm", "9.9.2", projectTestCaseName,
        windowsFileFolder, "", "");

    toolRepositoryMock.addAlreadyInstalledTool("node", "v18.19.1");

    return toolRepositoryMock;
  }
}