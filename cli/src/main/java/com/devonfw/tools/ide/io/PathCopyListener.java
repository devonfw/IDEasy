package com.devonfw.tools.ide.io;

import java.nio.file.Path;

/**
 * Interface for a listener of {@link Path} while {@link FileAccess#copy(Path, Path, FileCopyMode, PathCopyListener) copying}.
 *
 * @see FileAccess#copy(Path, Path, FileCopyMode, PathCopyListener)
 */
@FunctionalInterface
public interface PathCopyListener {

  /**
   * An empty {@link PathCopyListener} instance doing nothing.
   */
  PathCopyListener NONE = (s, t, d) -> {
  };

  /**
   * @param source the {@link Path} of the source to copy.
   * @param target the {@link Path} of the copied target.
   * @param directory - {@code true} in case of {@link java.nio.file.Files#isDirectory(Path, java.nio.file.LinkOption...) directory}, {@code false}
   *     otherwise (regular file).
   */
  void onCopy(Path source, Path target, boolean directory);

}
