package com.devonfw.tools.ide.merge;

import com.devonfw.tools.ide.context.IdeContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * {@link WorkspaceMerger} responsible for a single type of file.
 */
public abstract class FileMerger extends AbstractWorkspaceMerger {

  /**
   * The constructor.
   *
   * @param context the {@link #context}.
   */
  public FileMerger(IdeContext context) {

    super(context);
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
