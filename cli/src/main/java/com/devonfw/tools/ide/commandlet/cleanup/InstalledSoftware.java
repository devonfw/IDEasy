package com.devonfw.tools.ide.commandlet.cleanup;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains installed software and provides efficient lookup by installation path.
 */
public class InstalledSoftware {

  /** The installed software tools. */
  private final List<InstalledSoftwareTool> tools;

  /** Maps installation paths to their software versions. */
  private final Map<Path, InstalledSoftwareVersion> versionsByPath;

  /**
   * Constructor.
   */
  public InstalledSoftware() {

    this.tools = new ArrayList<>();
    this.versionsByPath = new HashMap<>();
  }

  /**
   * Adds an installed tool.
   *
   * @param tool the tool to add.
   */
  public void addTool(InstalledSoftwareTool tool) {

    this.tools.add(tool);
  }

  /**
   * Adds an edition to the given tool.
   *
   * @param tool the owning tool.
   * @param edition the edition to add.
   */
  public void addEdition(InstalledSoftwareTool tool, InstalledSoftwareEdition edition) {

    tool.addEdition(edition);
  }

  /**
   * Adds a version to the given edition and indexes its installation path.
   *
   * @param edition the owning edition.
   * @param version the version to add.
   */
  public void addVersion(InstalledSoftwareEdition edition, InstalledSoftwareVersion version) {

    edition.addVersion(version);
    this.versionsByPath.put(version.getPath(), version);
  }

  /**
   * Finds the installed version containing the given path.
   *
   * @param path the referenced installation path.
   * @return the matching installed version or {@code null} if no version was found.
   */
  public InstalledSoftwareVersion findVersion(Path path) {

    Path currentPath = path;
    while (currentPath != null) {
      InstalledSoftwareVersion version = this.versionsByPath.get(currentPath);
      if (version != null) {
        return version;
      }
      currentPath = currentPath.getParent();
    }
    return null;
  }

  /**
   * @return the installed software tools.
   */
  public List<InstalledSoftwareTool> getTools() {

    return this.tools;
  }
}
