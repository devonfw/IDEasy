package com.devonfw.tools.ide.url.model;

import com.devonfw.tools.ide.url.model.folder.UrlFolder;

/**
 * Interface for an {@link UrlArtifact} that has a {@link #getParent() parent}.
 *
 * @param <P> type of the {@link #getParent() parent} {@link UrlFolder folder}.
 */
public interface UrlArtifactWithParent<P extends UrlFolder<?>> extends UrlArtifact {

  /**
   * @return the parent {@link UrlFolder} owning this artifact as child.
   */
  P getParent();


  /**
   * Physically deletes this artifact with all its potential children from the disc. Will also remove it from its {@link #getParent() parent}.
   */
  default void delete() {
    getParent().deleteChild(getName());
  }
}
