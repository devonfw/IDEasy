package com.devonfw.tools.ide.merge;

import java.nio.file.Path;

import com.devonfw.tools.ide.environment.EnvironmentVariables;

/**
 * Interface for a merger responsible for merging {@link com.devonfw.tools.ide.tool.ide.IdeToolCommandlet IDE} configuration files into the workspace.
 */
public interface WorkspaceMerger {

  /**
   * @param setup the setup {@link Path} for creation.
   * @param update the update {@link Path} for creation and update.
   * @param variables the {@link EnvironmentVariables} to {@link EnvironmentVariables#resolve(String, Object) resolve variables}.
   * @param workspace the workspace {@link Path} to create or update.
   * @return the number of errors that occurred. Should be {@code 0} for success.
   */
  int merge(Path setup, Path update, EnvironmentVariables variables, Path workspace);

  /**
   * @param workspace the workspace {@link Path} where to get the changes from.
   * @param variables the {@link EnvironmentVariables} to {@link EnvironmentVariables#inverseResolve(String, Object) inverse resolve variables}.
   * @param addNewProperties - {@code true} to also add new properties to the {@code updateFile}, {@code false} otherwise (to only update existing
   *     properties).
   * @param update the update {@link Path}
   */
  void inverseMerge(Path workspace, EnvironmentVariables variables, boolean addNewProperties, Path update);

  /**
   * @param workspace the {@link Path} to the {@link com.devonfw.tools.ide.context.IdeContext#FOLDER_WORKSPACE workspace} with IDE templates to upgrade
   *     (migrate and replace legacy constructs).
   */
  void upgrade(Path workspace);

}
