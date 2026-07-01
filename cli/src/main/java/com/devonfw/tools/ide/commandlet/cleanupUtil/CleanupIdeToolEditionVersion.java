package com.devonfw.tools.ide.commandlet.cleanupUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a version of an IDE tool edition in the global software folder as discovered by the {@code cleanup} commandlet.
 * <p>
 * For example, for IntelliJ's "ultimate" edition, versions could be "2022.3" and "2023.1".
 * This class contains the version name, the installation path, a list of projects that
 * use this version, and a flag indicating whether the version is marked for deletion.
 */
public class CleanupIdeToolEditionVersion {

  /** The name of this version. */
  public final String versionName;

  /** The installation {@link Path} of this version. */
  private final Path path;

  /** A list of project names that currently use this version. */
  private final List<String> usedBy;

  /** A flag indicating whether the version is marked for deletion. */
  private boolean delete;

  /**
   * Constructor.
   *
   * @param versionName the name of the version.
   * @param path the installation {@link Path} of this version.
   */
  public CleanupIdeToolEditionVersion(String versionName, Path path) {

    this.versionName = versionName;
    this.path = path;
    this.usedBy = new ArrayList<>();
    this.delete = false;
  }

  /**
   * @return the installation {@link Path} of this version.
   */
  public Path getPath() {

    return this.path;
  }

  /**
   * @return the list of project names that currently use this version.
   */
  public List<String> getUsedBy() {

    return this.usedBy;
  }

  /**
   * Marks this version as used by the given project.
   *
   * @param projectName the name of the project using this version.
   */
  public void addUsedBy(String projectName) {

    this.usedBy.add(projectName);
  }

  /**
   * @return {@code true} if this version is marked for deletion.
   */
  public boolean isDelete() {

    return this.delete;
  }

  /**
   * Sets the deletion flag.
   *
   * @param delete {@code true} to mark this version for deletion.
   */
  public void setDelete(boolean delete) {

    this.delete = delete;
  }

  /**
   * @return {@code true} if no project currently uses this version.
   */
  public boolean isUnused() {

    return this.usedBy.isEmpty();
  }

  @Override
  public String toString() {

    return "CleanupIdeToolEditionVersion[" + this.versionName + "]";
  }
}
