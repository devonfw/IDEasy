package com.devonfw.tools.ide.url.model;

import com.devonfw.tools.ide.url.model.folder.AbstractUrlFolder;

/**
 * Class from which UrlTool, UrlEdition and UrlVersion inherit, as their objects have both parent and eventually child
 * objects. It mainly gives methods to read a parent object or to create or read child objects.
 *
 * @param <P> Type of the parent object
 * @param <C> Type of the child objects
 */
public abstract class AbstractUrlFolderWithParent<P extends AbstractUrlFolder<?>, C extends UrlArtifactWithParent<?>>
    extends AbstractUrlFolder<C> implements UrlArtifactWithParent<P> {

  private final P parent;

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   * @param name the {@link #getName() filename}.
   */
  public AbstractUrlFolderWithParent(P parent, String name) {

    super(parent.getPath().resolve(name), name);
    this.parent = parent;
  }

  @Override
  public P getParent() {

    return this.parent;
  }

}
