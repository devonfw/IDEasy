package com.devonfw.tools.ide.tool.npm;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.CommandLetExtractorMock;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.repo.ToolRepositoryMock;

public class NpmTest extends AbstractIdeContextTest {

  private static AbstractIdeContext context;
  private static Npm commandlet;
  private final static String NPM_TEST_PATH = "workspaces/foo-test/my-git-repo";
  private final static String PROJECT_TEST_CASE_NAME = "npm";
  private static ToolRepositoryMock toolRepositoryMock;
  private static Path mockResultPath;
  
  @BeforeAll
  static void setUp() {

    toolRepositoryMock = buildToolRepositoryMockForNpm(PROJECT_TEST_CASE_NAME);

    context = newContext(PROJECT_TEST_CASE_NAME, NPM_TEST_PATH, true, toolRepositoryMock);
    toolRepositoryMock.setContext(context);
    mockResultPath = Path.of("target/test-projects/npm/project");
    context.setDefaultExecutionDirectory(mockResultPath);

    CommandLetExtractorMock commandLetExtractorMock = new CommandLetExtractorMock(context);
    commandlet = new Npm(context);
    commandlet.setCommandletFileExtractor(commandLetExtractorMock);
  }

  @Test
  public void npmPostInstallShouldMoveFiles() {
      assertThat(context.getSoftwarePath().resolve("node/npm")).hasContent("This is npm");
      assertThat(context.getSoftwarePath().resolve("node/npx")).hasContent("This is npx");

      // act
      commandlet.install();

      // assert
      String expectedMessage = "Successfully installed npm in version 9.9.2";
      assertLogMessage((IdeTestContext) context, IdeLogLevel.SUCCESS, expectedMessage, false);

      if (context.getSystemInfo().isWindows()) {
        assertThat(context.getSoftwarePath().resolve("node/npm")).exists();
        assertThat(context.getSoftwarePath().resolve("node/npm.cmd")).exists();
        assertThat(context.getSoftwarePath().resolve("node/npx")).exists();
        assertThat(context.getSoftwarePath().resolve("node/npx.cmd")).exists();

        assertThat(context.getSoftwarePath().resolve("node/npm")).hasContent("This is npm bin");
        assertThat(context.getSoftwarePath().resolve("node/npx")).hasContent("This is npx bin");
      }
  }

  @Test
  public void npmShouldRunExecutableSuccessful() {

    //arrange
    String expectedOutputWindowsNpm = "Dummy npm bin 9.9.2 on windows";

    // act
    commandlet.install();
    commandlet.run();

    //assert
    assertThat(mockResultPath.resolve("npmTestResult.txt")).exists();
    assertThat(mockResultPath.resolve("npmTestResult.txt")).hasContent(expectedOutputWindowsNpm);
  }

  private static ToolRepositoryMock buildToolRepositoryMockForNpm(String projectTestCaseName) {

    String windowsFileFolder = "npm-9.9.2";

    ToolRepositoryMock toolRepositoryMock = new ToolRepositoryMock("npm", "9.9.2", projectTestCaseName,
        windowsFileFolder, "", "");

    toolRepositoryMock.addAlreadyInstalledTool("node", "v18.19.1");

    return toolRepositoryMock;
  }
}