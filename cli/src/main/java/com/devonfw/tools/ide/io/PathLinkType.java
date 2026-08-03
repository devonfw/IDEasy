package com.devonfw.tools.ide.io;

/**
 * Enum with the available types of a FileLink.
 */
public enum PathLinkType {

  /**
   * A symbolic link stores the path to the linked file or folder. If that link target is renamed or deleted, the link gets broken. On the other hand, the
   * target can be replaced by something else and the link will point to that.
   */
  SYMBOLIC_LINK("symbolic link"),

  /**
   * A hard link stores the reference to the physical target entry in the filesystem. It is therefore a duplicated entry in the filesystem metadata pointing to
   * the same target. If that original target gets deleted, the hard link is still a valid reference. On the other hand, if the target gets replaced by
   * something else, the hard link will still point to the target that was originally linked on its creation.
   */
  HARD_LINK("hard link");

  private final String title;

  private PathLinkType(String title) {
    this.title = title;
  }

  @Override
  public String toString() {

    return this.title;
  }
}
