package com.devonfw.tools.ide.configurator.merge;

import java.io.File;

import com.devonfw.tools.ide.logging.Log;

/**
 * {@link FileMerger} responsible for a single type of {@link File}.
 *
 * @since 3.0.0
 */
public abstract class FileTypeMerger implements FileMerger {

  /**
   * @param file the {@link File} for which the {@link File#getParentFile() parent directory} needs to exists and will
   *        be created if absent by this method.
   */
  protected static void ensureParentDirecotryExists(File file) {

    File parentDir = file.getParentFile();
    if (!parentDir.exists()) {
      if (!parentDir.mkdirs()) {
        Log.err("Could not create required directories for file: " + file.getAbsolutePath());
        return;
      }
    }
  }

}
