package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

import com.devonfw.tools.ide.commandlet.FileExtractor.CommandletFileExtractor;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;

/**
 * Implementation of {@link CommandletFileExtractor} for testing.
 */
public class CommandLetExtractorMock implements CommandletFileExtractor {

  private final IdeContext context;

  /**
   * The constructor.
   * 
   * @param context the {@link IdeContext}
   */
  public CommandLetExtractorMock(IdeContext context) {

    this.context = context;
  }

  @Override
  public void extract(Path file, Path targetDir, boolean isExtract) {

    FileAccess fileAccess = context.getFileAccess();
    fileAccess.mkdirs(targetDir);

    if (Files.isDirectory(file)) {

      try (Stream<Path> childStream = Files.list(file)) {
        Iterator<Path> iterator = childStream.iterator();
        while (iterator.hasNext()) {
          Path child = iterator.next();
          fileAccess.copy(child, targetDir.resolve(child.getFileName()));
        }
      } catch (IOException e) {
        throw new IllegalStateException("Failed to list files to copy in " + file, e);
      }

    } else {
      throw new IllegalStateException("Testing mocks only supports copying folders to install location!");
    }
  }

  @Override
  public void moveAndProcessExtraction(Path from, Path to) {

    // do nothing in mock
  }
}
