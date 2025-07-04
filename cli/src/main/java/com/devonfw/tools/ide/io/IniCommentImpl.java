package com.devonfw.tools.ide.io;

/**
 * Implementation of {@link IniComment}
 */
public class IniCommentImpl implements IniComment {

  String comment;

  /**
   * @param comment the content of the comment, including comment marker like ";"
   */
  public IniCommentImpl(String comment) {
    this.comment = comment;
  }

  @Override
  public String toString() {
    return comment;
  }
}
