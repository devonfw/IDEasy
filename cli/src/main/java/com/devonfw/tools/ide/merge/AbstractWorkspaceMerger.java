package com.devonfw.tools.ide.merge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link WorkspaceMerger} responsible for a single type of {@link Path}.
 */
public abstract class AbstractWorkspaceMerger implements WorkspaceMerger {

  /** The {@link IdeContext} for logging. */
  protected final IdeContext context;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public AbstractWorkspaceMerger(IdeContext context) {

    super();
    this.context = context;
  }

  /**
   * @param file the {@link Path} for which the {@link Path#getParent() parent directory} needs to exist and will be created if absent by this method.
   */
  protected static void ensureParentDirectoryExists(Path file) {

    Path parentDir = file.getParent();
    if (!Files.exists(parentDir)) {
      try {
        Files.createDirectories(parentDir);
      } catch (IOException e) {
        throw new IllegalStateException("Could not create required directories for file: " + file, e);
      }
    }
  }

}
