package com.devonfw.tools.ide.version;

/**
 * Abstract base interface for a version object such as {@link VersionIdentifier} and {@link VersionSegment}.
 *
 *
 * {@link Comparable} for versions with an extended contract. If two versions are not strictly comparable (e.g.
 * "1.apple" and "1.banana") we fall back to some heuristics (e.g. lexicographical comparison for
 * {@link VersionSegment#getLettersString() letters} that we do not understand (e.g. "apple" < "banana"). Therefore you can
 * use {@link #compareVersion(Object)} to get a {@link VersionComparisonResult} that contains the additional information
 * as {@link VersionComparisonResult#isUnsafe() unsafe} flag.
 *
 * @param <T> type of the object to compare (this class itself).
 */
public interface VersionObject<T> extends Comparable<T> {

  @Override
  default int compareTo(T other) {

    return compareVersion(other).asValue();
  }

  /**
   * @param other the other version to compare to.
   * @return the {@link VersionComparisonResult}.
   */
  VersionComparisonResult compareVersion(T other);

  /**
   * @param other the other version to compare to.
   * @return true if this version is greater than the given one.
   */
  default boolean isGreater(T other) {

    return compareVersion(other).isGreater();
  }

  /**
   * @param other the other version to compare to.
   * @return true if this version is less than the given one.
   */
  default boolean isLess(T other) {

    return compareVersion(other).isLess();
  }

  /**
   * @param other the other version to compare to.
   * @return true if this version is greater than or equal to the given one.
   */
  default boolean isGreaterOrEqual(T other) {

    return !compareVersion(other).isLess();
  }

  /**
   * @param other the other version to compare to.
   * @return true if this version is less than or equal to the given one.
   */
  default boolean isLessOrEqual(T other) {

    return !compareVersion(other).isGreater();
  }

  /**
   * @return {@code true} if this {@link VersionObject} itself is valid according to version scheme best-practices,
   *         {@code false} otherwise. Invalid {@link VersionObject}s can still be parsed and compared in a deterministic
   *         way but results may not always be perfect.
   */
  boolean isValid();

}
