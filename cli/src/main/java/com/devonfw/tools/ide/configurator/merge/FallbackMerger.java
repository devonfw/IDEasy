package com.devonfw.tools.ide.configurator.merge;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.devonfw.tools.ide.configurator.resolve.VariableResolver;

/**
 * Implementation of {@link FileTypeMerger} to use as fallback. It can not actually merge but will simply overwrite the
 * files.
 *
 * @since 3.0.0
 */
public class FallbackMerger extends FileTypeMerger {

  @Override
  public void merge(File setupFile, File updateFile, VariableResolver resolver, File workspaceFile) {

    if (updateFile.exists()) {
      copy(updateFile, workspaceFile);
    } else if (setupFile.exists() && !workspaceFile.exists()) {
      copy(setupFile, workspaceFile);
    }
  }

  @Override
  public void inverseMerge(File workspaceFile, VariableResolver resolver, boolean addNewProperties, File updateFile) {

    // nothing by default, we could copy the workspace file back to the update file if it exists...
  }

  private void copy(File sourceFile, File targetFile) {

    ensureParentDirecotryExists(targetFile);
    try {
      Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to copy file " + sourceFile + " to " + targetFile, e);
    }
  }

}
