package com.devonfw.ide.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

/**
 * This class helps to create a fake project folder structure for testing. The projects are named in the format "project-{i}" and the workspaces are named
 * "main".
 */
public class FakeProjectFolderStructureHelper {

  /**
   * @param rootPath root path where fake structure should be created
   * @return the rootPath
   */
  public static Path createFakeProjectFolderStructure(Path rootPath) {

    for (int i = 0; i <= 5; i++) {
      String projectFolderName = "project-" + i;
      assertThat(rootPath.resolve(projectFolderName).toFile().mkdir())
          .as("Unable to create mock project directory for mock project " + i)
          .isTrue();
      assertThat(rootPath.resolve(projectFolderName).resolve("workspaces").toFile().mkdir())
          .as("Unable to create mock workspaces directory for mock project " + i)
          .isTrue();
      assertThat(rootPath.resolve(projectFolderName).resolve("workspaces").resolve("main").toFile().mkdir())
          .as(
              "Unable to create mock main workspace directory for mock project " + i)
          .isTrue();
    }

    return rootPath;
  }

}
