package com.devonfw.tools.ide.io;

import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Implementation of {@link FileAccessImpl} used for tests.
 */
public class FileAccessTestImpl extends FileAccessImpl {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext} to use.
   */
  public FileAccessTestImpl(IdeContext context) {

    super(context);
  }

  /**
   * Overrides getFileSize and returns always -1.
   *
   * @param path of the file.
   * @return always -1.
   */
  protected long getFileSize(Path path) {

    return -1;
  }
}
