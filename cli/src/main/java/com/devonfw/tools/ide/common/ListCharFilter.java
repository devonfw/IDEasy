package com.devonfw.tools.ide.common;

/**
 * Implementation of {@link CharFilter} for a given list of characters.
 */
public class ListCharFilter implements CharFilter {

  private final char[] chars;

  /**
   * @param chars the characters to {@link #accept(char) accept}.
   */
  public ListCharFilter(char... chars) {
    super();
    this.chars = chars;
  }

  @Override
  public boolean accept(char c) {

    for (char ch : chars) {
      if (ch == c) {
        return true;
      }
    }
    return false;
  }
}
