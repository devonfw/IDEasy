package com.devonfw.tools.ide.url.model;

import com.devonfw.tools.ide.url.model.folder.AbstractUrlFolder;
import com.devonfw.tools.ide.url.model.folder.UrlFolder;

/**
 * Abstract base implementation of {@link UrlFolder} and {@link UrlArtifactWithParent}.
 *
 * @param <P> type of the {@link #getParent() parent} {@link UrlFolder folder}.
 */
public abstract class AbstractUrlArtifactWithParent<P extends AbstractUrlFolder<?>> extends AbstractUrlArtifact
    implements UrlArtifactWithParent<P> {

  private final P parent;

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   * @param name the {@link #getName() filename}.
   */
  public AbstractUrlArtifactWithParent(P parent, String name) {

    super(parent.getPath().resolve(name), name);
    this.parent = parent;
  }

  @Override
  public P getParent() {

    return this.parent;
  }

}
