package com.devonfw.tools.ide.commandlet.cleanupUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an installed IDE tool in the global software folder as discovered by the {@code cleanup} commandlet.
 * <p>
 * Contains the tool name, installation path, a list of projects that use this tool,
 * and a flag indicating whether the tool is marked for deletion. This class also
 * holds a list of {@link CleanupIdeToolEdition editions} belonging to this tool.
 */
public class CleanupIdeTool {

  /** The name of this tool. */
  public final String toolName;

  private final Path path;
  private final List<String> usedBy;
  private boolean delete;

  private final List<CleanupIdeToolEdition> editions;

  /**
   * Constructor.
   *
   * @param toolName the name of the tool.
   * @param path the installation {@link Path} of this tool.
   */
  public CleanupIdeTool(String toolName, Path path) {

    this.toolName = toolName;
    this.path = path;
    this.usedBy = new ArrayList<>();
    this.delete = false;
    this.editions = new ArrayList<>();
  }

  /**
   * @return the installation {@link Path} of this tool.
   */
  public Path getPath() {

    return this.path;
  }

  /**
   * @return the list of project names that currently use this tool.
   */
  public List<String> getUsedBy() {

    return this.usedBy;
  }

  /**
   * Marks this tool as used by the given project.
   *
   * @param projectName the name of the project that uses this tool.
   */
  public void markUsedBy(String projectName) {

    this.usedBy.add(projectName);
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
   * @return {@code true} if no project currently uses this tool.
   */
  public boolean isUnused() {

    return this.usedBy.isEmpty();
  }

  /**
   * @return the list of {@link CleanupIdeToolEdition editions} belonging to this tool.
   */
  public List<CleanupIdeToolEdition> getEditions() {

    return this.editions;
  }

  @Override
  public String toString() {

    return "CleanupIdeTool[" + this.toolName + "]";
  }
}
