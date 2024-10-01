package com.devonfw.tools.ide.repo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Implementation of {@link ToolRepository} for testing.
 */
public class ToolRepositoryMock extends DefaultToolRepository {

  private final Path repositoryFolder;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param repositoryFolder the {@link Path} to the mock repository.
   */
  public ToolRepositoryMock(IdeContext context, Path repositoryFolder) {

    super(context);
    this.repositoryFolder = repositoryFolder;
  }

  @Override
  public VersionIdentifier resolveVersion(String tool, String edition, GenericVersionRange version) {

    try {
      return super.resolveVersion(tool, edition, version);
    } catch (CliException e) {
      this.context.error(e, "Invalid test project using version {} that cannot be resolved in urls folder", version);
      return version.getMax();
    }
  }

  @Override
  public Path download(String tool, String edition, VersionIdentifier version) {

    Path editionFolder = this.repositoryFolder.resolve(tool).resolve(edition);
    Path versionFolder = editionFolder.resolve(version.toString());
    if (!Files.isDirectory(versionFolder)) {
      this.context.debug("Could not find version {} so using 'default' for {}/{}", version, tool, edition);
      versionFolder = editionFolder.resolve("default");
    }
    if (!Files.isDirectory(versionFolder)) {
      throw new IllegalStateException("Mock download failed - could not find folder " + editionFolder);
    }
    Path archiveFolder = versionFolder.resolve(this.context.getSystemInfo().getOs().toString());
    if (!Files.isDirectory(archiveFolder)) {
      archiveFolder = versionFolder;
    }
    Path contentArchive = null;
    try (Stream<Path> children = Files.list(archiveFolder)) {
      Iterator<Path> iterator = children.iterator();
      while (iterator.hasNext()) {
        if (contentArchive == null) {
          Path child = iterator.next();
          if (Files.isRegularFile(child) && child.getFileName().startsWith("content.")) {
            contentArchive = child;
            this.context.debug("Using compressed archive {} for mock download of {}/{}", child.getFileName(), tool,
                edition);
          } else {
            break;
          }
        } else {
          contentArchive = null;
          break;
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to list children of folder " + archiveFolder);
    }
    if (contentArchive != null) {
      return contentArchive;
    }
    return archiveFolder;
  }

}
