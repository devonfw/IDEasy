package com.devonfw.tools.ide.version;

/**
 * Enum representing the type of interval regarding its boundaries.
 */
public enum BoundaryType {

  /** Closed interval - includes the specified values at the boundaries. */
  CLOSED,

  /** Open interval - excludes the specified values at the boundaries. */
  OPEN,

  /** Left open interval - excludes the lower bound but includes the upper bound. */
  LEFT_OPEN,

  /** Right open interval - includes the lower bound but excludes the upper bound. */
  RIGHT_OPEN;

  static final String START_EXCLUDING_PREFIX = "(";

  static final String START_INCLUDING_PREFIX = "[";

  static final String END_EXCLUDING_SUFFIX = ")";

  static final String END_INCLUDING_SUFFIX = "]";

  /**
   * @return {@code true} if left exclusive, {@code false} otherwise (left inclusive).
   */
  public boolean isLeftExclusive() {

    return (this == LEFT_OPEN) || (this == OPEN);
  }

  /**
   * @return {@code true} if right exclusive, {@code false} otherwise (right inclusive).
   */
  public boolean isRightExclusive() {

    return (this == RIGHT_OPEN) || (this == OPEN);
  }

  /**
   * @return the prefix (left parenthesis or bracket for {@link #isLeftExclusive()}).
   */
  public String getPrefix() {

    return isLeftExclusive() ? START_EXCLUDING_PREFIX : START_INCLUDING_PREFIX;
  }

  /**
   * @return the suffix (right parenthesis or bracket for {@link #isRightExclusive()}).
   */
  public String getSuffix() {

    return isRightExclusive() ? END_EXCLUDING_SUFFIX : END_INCLUDING_SUFFIX;
  }

  /**
   * @param leftExclusive the {@link #isLeftExclusive() left exclusive flag}.
   * @param rightExclusive the {@link #isRightExclusive() right exclusive flag}.
   * @return the {@link BoundaryType} with the specified values.
   */
  public static BoundaryType of(boolean leftExclusive, boolean rightExclusive) {

    if (leftExclusive) {
      if (rightExclusive) {
        return OPEN;
      } else {
        return LEFT_OPEN;
      }
    } else {
      if (rightExclusive) {
        return RIGHT_OPEN;
      } else {
        return CLOSED;
      }
    }
  }
}