package com.devonfw.tools.ide.url.model.folder;

import com.devonfw.tools.ide.url.model.AbstractUrlFolderWithParent;

/**
 * An {@link UrlFolder} representing the actual software tool like "docker" or "vscode".
 */
public class UrlTool extends AbstractUrlFolderWithParent<UrlRepository, UrlEdition> {

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   * @param name the {@link #getName() filename}.
   */
  public UrlTool(UrlRepository parent, String name) {

    super(parent, name);
  }

  /**
   * This method is used to add new children to the children collection of an instance from this class.
   *
   * @param name The name of the {@link UrlEdition} object that should be created.
   */
  @Override
  protected UrlEdition newChild(String name) {

    return new UrlEdition(this, name);
  }

}
