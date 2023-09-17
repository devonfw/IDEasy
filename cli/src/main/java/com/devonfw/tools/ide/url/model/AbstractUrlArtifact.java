package com.devonfw.tools.ide.url.model;

import java.nio.file.Path;

/**
 * Abstract base implementation of {@link UrlArtifact}.
 */
public abstract class AbstractUrlArtifact implements UrlArtifact {

  private final Path path;

  private final String name;

  /** {@code true} if already {@link #load(boolean) loaded}, {@code false} otherwise. */
  protected boolean loaded;

  /**
   * The constructor.
   *
   * @param path the {@link #getPath() path}.
   * @param name the {@link #getName() filename}.
   */
  public AbstractUrlArtifact(Path path, String name) {

    super();
    this.path = path;
    this.name = name;
  }

  @Override
  public Path getPath() {

    return this.path;
  }

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  public String toString() {

    return this.name + "[" + getClass().getSimpleName() + "@" + this.path + "]";
  }

}
