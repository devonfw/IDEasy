package com.devonfw.tools.ide.merge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * {@link WorkspaceMerger} responsible for a single type of file.
 */
public abstract class FileMerger extends AbstractWorkspaceMerger {

  protected final boolean legacySupport;

  /**
   * The constructor.
   *
   * @param context the {@link #context}.
   */
  public FileMerger(IdeContext context) {

    super(context);
    this.legacySupport = IdeVariables.IDE_VARIABLE_SYNTAX_LEGACY_SUPPORT_ENABLED.get(context).booleanValue();
  }

  /**
   * @param sourceFile Path to source file.
   * @param targetFile Path to target file.
   */
  protected void copy(Path sourceFile, Path targetFile) {

    ensureParentDirectoryExists(targetFile);
    try {
      Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to copy file " + sourceFile + " to " + targetFile, e);
    }
  }

  @Override
  public final int merge(Path setup, Path update, EnvironmentVariables variables, Path workspace) {
    try {
      doMerge(setup, update, variables, workspace);
    } catch (Exception e) {
      this.context.error(e, "Failed to merge workspace file {} with update template {} and setup file {}!", workspace, update, setup);
      return 1;
    }
    return 0;
  }

  /**
   * Same as {@link #merge(Path, Path, EnvironmentVariables, Path)} but without error handling.
   *
   * @param setup the setup {@link Path} for creation.
   * @param update the update {@link Path} for creation and update.
   * @param variables the {@link EnvironmentVariables} to {@link EnvironmentVariables#resolve(String, Object) resolve variables}.
   * @param workspace the workspace {@link Path} to create or update.
   */
  protected abstract void doMerge(Path setup, Path update, EnvironmentVariables variables, Path workspace);
}
