package com.devonfw.tools.ide.io;

/**
 * Implementation of {@link IniComment}
 */
public class IniCommentImpl implements IniComment {

  String comment;
  int indentLevel;

  /**
   * @param comment the content of the comment, including comment marker like ";"
   * @param indentLevel the indentation level
   */
  public IniCommentImpl(String comment, int indentLevel) {
    this.comment = comment;
    this.indentLevel = indentLevel;
  }

  /**
   * @param comment the content of the comment, including comment marker like ";"
   */
  public IniCommentImpl(String comment) {
    this(comment, 0);
  }

  @Override
  public int getIndentLevel() {
    return indentLevel;
  }

  @Override
  public String toString() {
    return comment;
  }
}
