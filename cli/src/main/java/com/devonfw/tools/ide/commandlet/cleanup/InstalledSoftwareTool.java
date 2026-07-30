package com.devonfw.tools.ide.commandlet.cleanup;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an installed IDE tool in the global software folder as discovered by the {@code cleanup} commandlet.
 * <p>
 * Contains a flag indicating whether the tool is marked for deletion and a list of
 * {@link InstalledSoftwareEdition editions} belonging to this tool.
 */
public class InstalledSoftwareTool extends AbstractInstalledSoftwareItem {

  /** A flag indicating whether the tool is marked for deletion. */
  private boolean delete;

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
    this.delete = false;
    this.editions = new ArrayList<>();
  }

  /**
   * @return {@code true} if this tool is marked for deletion.
   */
  public boolean isDelete() {

    return this.delete;
  }

  /**
   * Sets the deletion flag.
   *
   * @param delete {@code true} to mark this tool for deletion.
   */
  public void setDelete(boolean delete) {

    this.delete = delete;
  }

  /**
   * @return {@code true} if all editions of this tool are unused.
   */
  public boolean isUnused() {

    return this.editions.stream().allMatch(InstalledSoftwareEdition::isUnused);
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
