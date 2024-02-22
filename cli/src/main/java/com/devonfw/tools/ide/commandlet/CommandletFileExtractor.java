package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.tool.ToolCommandlet;

import java.nio.file.Path;

/**
 * {@link CommandletFileExtractor} class which handles the extraction of downloaded installations of a
 * {@link ToolCommandlet}.
 */
public interface CommandletFileExtractor {

  /**
   *
   * @param file file the {@link Path} to the file to extract.
   * @param targetDir targetDir the {@link Path} to the directory where to extract (or copy) the file.
   * @param isExtract {@code true} if the tool is truly extracted, {@code false} the tool will be moved to the targetDir
   *        (e.g. when an installer exists).
   */
  void extract(Path file, Path targetDir, boolean isExtract);

  /**
   * Moves the extracted content to the final destination {@link Path}. May be overridden to customize the extraction
   * process.
   *
   * @param from the source {@link Path} to move.
   * @param to the target {@link Path} to move to.
   */
  void moveAndProcessExtraction(Path from, Path to);

}
