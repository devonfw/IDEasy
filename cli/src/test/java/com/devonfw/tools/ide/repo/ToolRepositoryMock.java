package com.devonfw.tools.ide.repo;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Implementation of {@link ToolRepository} for testing.
 */
public class ToolRepositoryMock implements ToolRepository {

  private static final Path PROJECTS_TARGET_PATH = Path.of("target/test-projects");

  private static final String MOCK_DOWNLOAD_FOLDER = "downloadMockLocation";

  private final String projectTestCaseName;

  private IdeContext context;

  // Save data of multiple tools to provide different faking for different tools
  private final Map<String, ToolDataForMock> toolToToolData;

  private record ToolDataForMock(String version, String windowsFolderName, String linuxFolderName,
      String macFolderName) {
  }

  /**
   * Constructor of a ToolRepository Mock with information of an initial tool
   *
   * @param tool The name of the main tool which is under test.
   * @param projectTestCaseName the (folder)name of the project test case, in this folder a 'project' folder represents
   *        the test project in {@link #PROJECTS_TARGET_PATH}. E.g. "basic".
   * @param windowsFolderName Name of the folder which is used when test is run under Windows
   * @param linuxFolderName Name of the folder which is used when test is run under Linux
   * @param macFolderName Name of the folder which is used when test is run under Mac OS
   */
  public ToolRepositoryMock(String tool, String toolVersion, String projectTestCaseName, String windowsFolderName,
      String linuxFolderName, String macFolderName) {

    toolToToolData = new HashMap<>();

    this.projectTestCaseName = projectTestCaseName;
    ToolDataForMock toolData = new ToolDataForMock(toolVersion, windowsFolderName, linuxFolderName, macFolderName);
    toolToToolData.put(tool, toolData);

  }

  public void setContext(IdeContext context) {

    this.context = context;

  }

  /**
   * Under the assumption that a tool is already installed (a .ide.software.version does exist in the path
   * ..._ide/software/default/YOUR_TOOL/YOUR_TOOL/toolVersion). this method is used to configure that the correct
   * version of the tool (the one in .ide.software.version) is used.
   *
   * @param tool The name of the tool.
   * @param toolVersion The version of the tool.
   */
  public void addAlreadyInstalledTool(String tool, String toolVersion) {

    addToolToInstall(tool, toolVersion, null, null, null);
  }

  /**
   * Method to add required information in order to mock out the tool installation. Please make sure that an
   * installation folder does exist.
   *
   * @param tool The name of the tool.
   * @param toolVersion The version of the tool.
   * @param windowsFolderName Name of the folder which is used when test is run under Windows
   * @param linuxFolderName Name of the folder which is used when test is run under Linux
   * @param macFolderName Name of the folder which is used when test is run under Mac OS
   */
  public void addToolToInstall(String tool, String toolVersion, String windowsFolderName, String linuxFolderName,
      String macFolderName) {

    ToolDataForMock toolData = new ToolDataForMock(toolVersion, windowsFolderName, linuxFolderName, macFolderName);
    toolToToolData.put(tool, toolData);
  }

  @Override
  public String getId() {

    return ID_DEFAULT;
  }

  @Override
  public VersionIdentifier resolveVersion(String tool, String edition, VersionIdentifier version) {

    ToolDataForMock toolData = toolToToolData.get(tool);
    return VersionIdentifier.of(toolData.version);
  }

  @Override
  public Path download(String tool, String edition, VersionIdentifier version) {

    ToolDataForMock toolData = toolToToolData.get(tool);
    String windowsFolderName = toolData.windowsFolderName;
    String linuxFolderName = toolData.linuxFolderName;
    String macFolderName = toolData.macFolderName;

    Path baseDownloadMockPath = PROJECTS_TARGET_PATH.resolve(projectTestCaseName).resolve(MOCK_DOWNLOAD_FOLDER);
    String mockProgram = "";

    if (context == null) {
      throw new IllegalStateException("Please set an IdeContext!");
    }

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
