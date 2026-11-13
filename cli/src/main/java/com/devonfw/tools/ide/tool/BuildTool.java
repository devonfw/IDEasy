package com.devonfw.tools.ide.tool;

import java.nio.file.Path;
import java.util.List;

import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Interface for a build-tool (e.g. {@link com.devonfw.tools.ide.tool.mvn.Mvn maven}, gradle, npm, yarn) that is able to build and release a project.
 */
public interface BuildTool {

  /**
   * @return the name of this build-tool.
   */
  String getName();

  /**
   * @param args the arguments for the build-tool.
   * @return the {@link ProcessResult}.
   */
  ProcessResult runTool(List<String> args);

  /**
   * @param directory the {@link Path} to the build directory.
   * @return the build descriptor file for this build-tool or {@code null} if not found.
   */
  Path findBuildDescriptor(Path directory);

  /**
   * @param projectPath the {@link Path} to the top-level directory of the project.
   * @return the current {@link VersionIdentifier version} of the project or {@code null} if this build-tool does not support releasing.
   */
  VersionIdentifier getProjectVersion(Path projectPath);

  /**
   * Sets the {@link #getProjectVersion(Path) version} of the project.
   *
   * @param projectPath the {@link Path} to the top-level directory of the project.
   * @param version the new {@link VersionIdentifier version} to set.
   */
  void setProjectVersion(Path projectPath, VersionIdentifier version);

  /**
   * Performs a single build-and-deploy (release) build of the project in the current working directory.
   *
   * @param additionalArgs the additional arguments to append to the build command (may be {@link List#isEmpty() empty}).
   * @return the {@link ProcessResult} of the build (allowing the caller to react on {@link ProcessResult#isSuccessful() failures}, e.g. by retrying).
   */
  ProcessResult buildAndDeploy(List<String> additionalArgs);
}
