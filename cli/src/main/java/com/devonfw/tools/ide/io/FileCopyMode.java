package com.devonfw.tools.ide.io;

/**
 * {@link Enum} with the available modes to {@link FileAccess#copy(java.nio.file.Path, java.nio.file.Path, FileCopyMode) copy} files and folders.
 */
public enum FileCopyMode {

  /** Copy {@link #isFileOnly() only a single file} and {@link #isFailIfExists() fail if the target-file already exists}. */
  COPY_FILE_FAIL_IF_EXISTS(true, false, true),

  /** Copy {@link #isFileOnly() only a single file} and override the target-file if it already exists. */
  COPY_FILE_OVERRIDE(true, true, true),

  /** Copy {@link #isFileOnly() only a single file} and override the target-file if it already exists. */
  COPY_FILE_TO_TARGET_OVERRIDE(true, true, false),

  /** Copy {@link #isRecursive() recursively} and {@link #isFailIfExists() fail if the target-path already exists}. */
  COPY_TREE_FAIL_IF_EXISTS(false, false, true),

  /** Copy {@link #isRecursive() recursively} and override existing files but merge existing folders. */
  COPY_TREE_OVERRIDE_FILES(false, true, true),

  /** Copy {@link #isRecursive() recursively} from virtual filesystem of a compressed archive and override existing files but merge existing folders. */
  EXTRACT(false, null, true),

  /** Copy {@link #isRecursive() recursively} and {@link FileAccess#delete(java.nio.file.Path) delete} the target-file if it exists before copying. */
  COPY_TREE_OVERRIDE_TREE(false, true, true),

  /** Copy {@link #isRecursive() recursively} and append the file name to the target. */
  COPY_TREE_CONTENT(false, null, false);

  private final boolean fileOnly;

  private final Boolean override;

  private final boolean useSourceFilename;

  private FileCopyMode(boolean fileOnly, Boolean override, boolean useSourceFilename) {
    this.fileOnly = fileOnly;
    this.override = override;
    this.useSourceFilename = useSourceFilename;
  }

  /**
   * @return {@code true} if only a single file shall be copied. Will fail if a directory is given to copy, {@code false} otherwise (to copy folders
   *     recursively).
   */
  public boolean isFileOnly() {

    return this.fileOnly;
  }

  /**
   * @return {@code true} if files and folders shall be copied recursively, {@code false} otherwise ({@link #isFileOnly() copy file copy}).
   */
  public boolean isRecursive() {

    return !isFileOnly();
  }

  /**
   * @return {@code true} to fail if the target file or folder already exists, {@code false} otherwise.
   */
  public boolean isFailIfExists() {

    return Boolean.FALSE.equals(this.override);
  }

  /**
   * @return {@code true} to override existing files and merge existing directories, {@code false} otherwise.
   */
  public boolean isOverride() {

    return Boolean.TRUE.equals(this.override);
  }

  /**
   * @return {@code true} if we copy from a virtual filesystem of a compressed archive.
   */
  public boolean isExtract() {

    return (this == EXTRACT);
  }

  /**
   * @return the name of the operation (typically "copy" but may also be e.g. "extract").
   */
  public String getOperation() {

    if (isExtract()) {
      return "extract";
    }
    return "copy";
  }

  /**
   * @return {@code true} to copy to the target as directory with the source filename appended, {@code false} copy to the target as given.
   */
  public boolean isUseSourceFilename() {

    return this.useSourceFilename;
  }
}
