package com.devonfw.tools.ide.commandlet.cleanup;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an installed IDE tool in the global software folder as discovered by the {@code cleanup} commandlet.
 * <p>
 * Contains a list of {@link InstalledSoftwareEdition editions} belonging to this tool.
 */
public class InstalledSoftwareTool extends AbstractInstalledSoftwareItem {

  /** A list of {@link InstalledSoftwareEdition editions} belonging to this tool. */
  private final List<InstalledSoftwareEdition> editions;

  /**
   * Constructor.
   *
   * @param name the name of the tool.
   * @param path the installation {@link Path} of this tool.
   */
  public InstalledSoftwareTool(String name, Path path) {

    super(name, path);
    this.editions = new ArrayList<>();
  }

  /**
   * @return the list of {@link InstalledSoftwareEdition editions} belonging to this tool.
   */
  public List<InstalledSoftwareEdition> getEditions() {

    return this.editions;
  }

  @Override
  public String toString() {

    return "InstalledSoftwareTool[" + getName() + "]";
  }
}
