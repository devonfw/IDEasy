package com.devonfw.tools.ide.url.model.folder;

import com.devonfw.tools.ide.url.model.AbstractUrlFolderWithParent;
import com.devonfw.tools.ide.url.model.file.UrlSecurityFile;

/**
 * An {@link UrlFolder} representing the actual edition of a {@link UrlTool}. The default edition may have the same
 * {@link #getName() name} as the {@link UrlTool} itself. However, tools like "intellij" may have editions like
 * "community" or "ultimate".
 */
public class UrlEdition extends AbstractUrlFolderWithParent<UrlTool, UrlVersion> {

  private UrlSecurityFile securityFile;

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   * @param name the {@link #getName() filename}.
   */
  public UrlEdition(UrlTool parent, String name) {

    super(parent, name);
  }

  /**
   * This method is used to add new children to the children collection of an instance from this class.
   *
   * @param name The name of the {@link UrlVersion} object that should be created.
   */
  @Override
  protected UrlVersion newChild(String name) {

    return new UrlVersion(this, name);
  }

  /**
   * @return the {@link UrlSecurityFile} of this {@link UrlEdition}. Will be lazily initialized on the first call of
   *         this method. If the file exists, it will be loaded, otherwise it will be empty and only created on save if
   *         data was added.
   */
  public UrlSecurityFile getSecurityFile() {

    if (this.securityFile == null) {
      this.securityFile = new UrlSecurityFile(this);
      this.securityFile.load(false);
    }
    return this.securityFile;
  }

  @Override
  public void save() {

    super.save();
    if (this.securityFile != null) {
      this.securityFile.save();
    }
  }

}
