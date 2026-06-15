package com.devonfw.tools.ide.commandlet.cleanupUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an edition of an IDE tool in the global software folder as discovered by the {@code cleanup} commandlet.
 * <p>
 * For example, for IntelliJ, the editions could be "community" and "ultimate". This class
 * contains the edition name, the installation path, a list of projects that use this
 * edition, and a flag indicating whether the edition is marked for deletion. It also holds
 * a list of {@link CleanupIdeToolEditionVersion versions} belonging to this edition.
 */
public class CleanupIdeToolEdition {

  /** The name of this edition. */
  public final String editionName;

  /** The installation {@link Path} of this edition. */
  private final Path path;

  /** A list of project names that currently use this edition. */
  private final List<String> usedBy;

  /** A flag indicating whether the edition is marked for deletion. */
  private boolean delete;

  /** A list of {@link CleanupIdeToolEditionVersion versions} belonging to this edition. */
  private final List<CleanupIdeToolEditionVersion> versions;

  /**
   * Constructor.
   *
   * @param editionName the name of the edition.
   * @param path the installation {@link Path} of this edition.
   */
  public CleanupIdeToolEdition(String editionName, Path path) {

    this.editionName = editionName;
    this.path = path;
    this.usedBy = new ArrayList<>();
    this.delete = false;
    this.versions = new ArrayList<>();
  }

  /**
   * @return the installation {@link Path} of this edition.
   */
  public Path getPath() {

    return this.path;
  }

  /**
   * @return the list of project names that currently use this edition.
   */
  public List<String> getUsedBy() {

    return this.usedBy;
  }

  /**
   * Marks this edition as used by the given project.
   *
   * @param projectName the name of the project using this edition.
   */
  public void addUsedBy(String projectName) {

    this.usedBy.add(projectName);
  }

  /**
   * @return {@code true} if this edition is marked for deletion.
   */
  public boolean isDelete() {

    return this.delete;
  }

  /**
   * Sets the deletion flag.
   *
   * @param delete {@code true} to mark this edition for deletion.
   */
  public void setDelete(boolean delete) {

    this.delete = delete;
  }

  /**
   * @return {@code true} if no project currently uses this edition.
   */
  public boolean isUnused() {

    return this.usedBy.isEmpty();
  }

  /**
   * @return the list of {@link CleanupIdeToolEditionVersion versions} belonging to this edition.
   */
  public List<CleanupIdeToolEditionVersion> getVersions() {

    return this.versions;
  }

  @Override
  public String toString() {

    return "CleanupIdeToolEdition[" + this.editionName + "]";
  }
}
