package com.devonfw.tools.ide.common;

/**
 * Interface for a filter deciding which characters are {@link #accept(char) accepted}.
 */
public interface CharFilter {

  /**
   * @param c the character to check.
   * @return {@code true} if the character will be accepted, {@code false} otherwise.
   */
  boolean accept(char c);

}
