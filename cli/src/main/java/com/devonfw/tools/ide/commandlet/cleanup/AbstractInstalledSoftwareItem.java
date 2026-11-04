package com.devonfw.tools.ide.commandlet.cleanup;

import java.nio.file.Path;

/**
 * Abstract base class for an item in the installed software hierarchy.
 */
public abstract class AbstractInstalledSoftwareItem {

  /** The name of this item. */
  private final String name;

  /** The installation path of this item. */
  private final Path path;

  /**
   * Constructor.
   *
   * @param name the name of this item.
   * @param path the installation path of this item.
   */
  protected AbstractInstalledSoftwareItem(String name, Path path) {

    this.name = name;
    this.path = path;
  }

  /**
   * @return the name of this item.
   */
  public String getName() {

    return this.name;
  }

  /**
   * @return the installation path of this item.
   */
  public Path getPath() {

    return this.path;
  }
}
