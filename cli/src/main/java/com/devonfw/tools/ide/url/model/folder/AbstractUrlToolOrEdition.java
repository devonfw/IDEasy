package com.devonfw.tools.ide.url.model.folder;

import com.devonfw.tools.ide.url.model.AbstractUrlFolderWithParent;
import com.devonfw.tools.ide.url.model.UrlArtifactWithParent;
import com.devonfw.tools.ide.url.model.file.UrlDependencyFile;
import com.devonfw.tools.ide.url.model.file.UrlSecurityFile;

public abstract class AbstractUrlToolOrEdition<P extends AbstractUrlFolder<?>, C extends UrlArtifactWithParent<?>> extends AbstractUrlFolderWithParent<P, C> {

  private UrlDependencyFile dependencyFile;
  private UrlSecurityFile securityFile;

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   * @param name the {@link #getName() filename}.
   */
  public AbstractUrlToolOrEdition(P parent, String name) {

    super(parent, name);
  }


  /**
   * @return the {@link UrlDependencyFile} of this {@link UrlEdition}. Will be lazily initialized on the first call of this method. If the file exists, it will
   *     be loaded, otherwise it will be empty and only created on save if data was added.
   */
  public UrlDependencyFile getDependencyFile() {

    if (this.dependencyFile == null) {
      this.dependencyFile = new UrlDependencyFile(this);
      this.dependencyFile.load(false);
    }
    return dependencyFile;
  }

  /**
   * @return the {@link UrlSecurityFile} of this {@link UrlEdition}. Will be lazily initialized on the first call of this method. If the file exists, it will be
   *     loaded, otherwise it will be empty and only created on save if data was added.
   */
  public UrlSecurityFile getSecurityFile() {

    if (this.securityFile == null) {
      this.securityFile = new UrlSecurityFile(this);
      this.securityFile.load(false);
    }
    return securityFile;
  }
}
