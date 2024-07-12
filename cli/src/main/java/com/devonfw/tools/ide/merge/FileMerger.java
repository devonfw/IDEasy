package com.devonfw.tools.ide.merge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.devonfw.tools.ide.context.IdeContext;
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

}
