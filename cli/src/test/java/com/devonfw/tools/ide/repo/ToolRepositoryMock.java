package com.devonfw.tools.ide.repo;

import java.nio.file.Path;
import java.util.Map;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Implementation of {@link ToolRepository} for testing.
 */
public class ToolRepositoryMock implements ToolRepository {

  private static final Path PROJECTS_TARGET_PATH = Path.of("target/test-projects");

  private static final String MOCK_DOWNLOAD_FOLDERNAME = "downloadMockLocation";

  private final Map<String, String> toolToVersion;

  private final String projectTestCaseName;

  private final String windowsFolderName;

  private final String linuxFolderName;

  private final String macFolderName;

  private IdeContext context;

  /**
   * Constructor of a ToolRepository Mock
   *
   * @param toolToVersion Mapping of which Version of a tool should be mocked
   * @param projectTestCaseName the (folder)name of the project test case, in this folder a 'project' folder represents
   * the test project in {@link #PROJECTS_TARGET_PATH}. E.g. "basic".
   * @param windowsFolderName Name of the folder which is used when test is run under Windows
   * @param linuxFolderName Name of the folder which is used when test is run under Linux
   * @param macFolderName Name of the folder which is used when test is run under Mac OS
   */
  public ToolRepositoryMock(Map<String, String> toolToVersion, String projectTestCaseName, String windowsFolderName,
      String linuxFolderName, String macFolderName) {

    this.toolToVersion = toolToVersion;
    this.projectTestCaseName = projectTestCaseName;
    this.windowsFolderName = windowsFolderName;
    this.linuxFolderName = linuxFolderName;
    this.macFolderName = macFolderName;
  }

  public void setContext(IdeContext context) {

    this.context = context;

  }

  @Override
  public String getId() {

    return ID_DEFAULT;
  }

  @Override
  public VersionIdentifier resolveVersion(String tool, String edition, VersionIdentifier version) {

    return VersionIdentifier.of(toolToVersion.get(tool));
  }

  @Override
  public Path download(String tool, String edition, VersionIdentifier version) {

    Path baseDownloadMockPath = PROJECTS_TARGET_PATH.resolve(projectTestCaseName).resolve(MOCK_DOWNLOAD_FOLDERNAME);
    String mockProgram = "";

    if (context == null) {
      throw new IllegalStateException("Please set a IdeContext!");
    }

    // TODO MOCK CONTEXT SYSTEMINFO ???
    if (context.getSystemInfo().isWindows()) {
      mockProgram = windowsFolderName;
    } else if (context.getSystemInfo().isLinux()) {
      mockProgram = linuxFolderName;
    } else if (context.getSystemInfo().isMac()) {
      mockProgram = macFolderName;
    }

    return baseDownloadMockPath.resolve(mockProgram);
  }
}
