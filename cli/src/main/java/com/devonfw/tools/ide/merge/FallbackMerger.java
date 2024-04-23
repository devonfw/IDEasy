package com.devonfw.tools.ide.merge;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implementation of {@link FileMerger} to use as fallback. It can not actually merge but will simply overwrite the files.
 *
 * @since 3.0.0
 */
public class FallbackMerger extends FileMerger {

  /**
   * The constructor.
   *
   * @param context the {@link #context}.
   */
  public FallbackMerger(IdeContext context) {

    super(context);
  }

  @Override
  public void merge(Path setup, Path update, EnvironmentVariables variables, Path workspace) {

    if (Files.exists(update)) {
      copy(update, workspace);
    } else if (Files.exists(setup) && !Files.exists(workspace)) {
      copy(setup, workspace);
    }
  }

  @Override
  public void inverseMerge(Path workspaceFile, EnvironmentVariables resolver, boolean addNewProperties, Path updateFile) {

    // nothing by default, we could copy the workspace file back to the update file if it exists...
  }

}
