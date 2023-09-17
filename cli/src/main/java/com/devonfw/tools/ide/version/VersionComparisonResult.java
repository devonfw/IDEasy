package com.devonfw.tools.ide.version;

/**
 * Result of {@link VersionObject#compareVersion(Object)}.
 */
public enum VersionComparisonResult {

  /** The first version is smaller than the second one. */
  LESS,

  /** {@link #LESS Less} but {@link #isUnsafe() unsafe}. */
  LESS_UNSAFE,

  /** Both versions are equal. */
  EQUAL,

  /** {@link #EQUAL} but {@link #isUnsafe() unsafe}. */
  EQUAL_UNSAFE,

  /** The second version is larger than the second one. */
  GREATER,

  /** {@link #GREATER Greater} but {@link #isUnsafe() unsafe}. */
  GREATER_UNSAFE;

  /**
   * @return {@code true} if the versions are not strictly comparable (e.g. "apple" and "banana", also for "1.0" and
   *         "1-0"), {@code false} otherwise.
   *
   * @see #LESS_UNSAFE
   * @see #GREATER_UNSAFE
   */
  public boolean isUnsafe() {

    return (this == LESS_UNSAFE) || (this == GREATER_UNSAFE);
  }

  /**
   * @return {@code true} if the first version was smaller than the second one, {@code false} otherwise.
   */
  public boolean isLess() {

    return (this == LESS) || (this == LESS_UNSAFE);
  }

  /**
   * @return {@code true} if both versions were equal, {@code false} otherwise.
   */
  public boolean isEqual() {

    return (this == EQUAL) || (this == EQUAL_UNSAFE);
  }

  /**
   * @return {@code true} if the first version was larger than the second one, {@code false} otherwise.
   */
  public boolean isGreater() {

    return (this == GREATER) || (this == GREATER_UNSAFE);
  }

  /**
   * @return an integer value equivalent with the {@link VersionComparisonResult} and compliant with
   *         {@link VersionObject#compareTo(Object)}.
   */
  public int asValue() {

    switch (this) {
      case LESS:
      case LESS_UNSAFE:
        return -1;
      case EQUAL:
      case EQUAL_UNSAFE:
        return 0;
      case GREATER:
      case GREATER_UNSAFE:
        return 1;
      default:
        throw new IllegalStateException(toString());
    }
  }

  /**
   * @param unsafe the new value of {@link #isUnsafe()}.
   * @return an equivalent {@link VersionComparisonResult} with the given {@link #isUnsafe() unsafe} value.
   */
  public VersionComparisonResult withUnsafe(boolean unsafe) {

    if (unsafe) {
      return withUnsafe();
    } else {
      return withSafe();
    }
  }

  /**
   * @return an equivalent {@link VersionComparisonResult} with {@link #isUnsafe() unsafe} being {@code true}.
   */
  public VersionComparisonResult withUnsafe() {

    if (isGreater()) {
      return GREATER_UNSAFE;
    } else if (isLess()) {
      return LESS_UNSAFE;
    } else {
      return EQUAL_UNSAFE;
    }
  }

  /**
   * @return an equivalent {@link VersionComparisonResult} with {@link #isUnsafe() unsafe} being {@code false}.
   */
  public VersionComparisonResult withSafe() {

    if (isGreater()) {
      return GREATER;
    } else if (isLess()) {
      return LESS;
    } else {
      return EQUAL;
    }
  }

}
