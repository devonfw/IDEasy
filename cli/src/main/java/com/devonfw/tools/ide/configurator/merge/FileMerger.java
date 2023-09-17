package com.devonfw.tools.ide.configurator.merge;

import java.io.File;

import com.devonfw.tools.ide.configurator.resolve.VariableResolver;

/**
 * Interface for a merger responsible for merging files.
 *
 * @since 3.0.0
 */
public interface FileMerger {

  /**
   * @param setupFile the setup {@link File} for creation.
   * @param updateFile the update {@link File} for creation and update.
   * @param resolver the {@link VariableResolver} to {@link VariableResolver#resolve(String) resolve variables}.
   * @param workspaceFile the workspace {@link File} to create or update.
   */
  void merge(File setupFile, File updateFile, VariableResolver resolver, File workspaceFile);

  /**
   * @param workspaceFile the workspace {@link File} where to get the changes from.
   * @param resolver the {@link VariableResolver} to {@link VariableResolver#inverseResolve(String) generalize variables}.
   * @param addNewProperties - {@code true} to also add new properties to the {@code updateFile}, {@code false}
   *        otherwise (to only update existing properties).
   * @param updateFile the update {@link File}
   */
  void inverseMerge(File workspaceFile, VariableResolver resolver, boolean addNewProperties, File updateFile);

}
