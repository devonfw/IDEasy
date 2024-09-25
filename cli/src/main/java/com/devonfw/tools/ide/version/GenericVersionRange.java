package com.devonfw.tools.ide.version;

public interface GenericVersionRange {

  /**
   * @return the minimum {@link VersionIdentifier} or {@code null} for no lower bound.
   */
  VersionIdentifier getMin();

  /**
   * @return the maximum {@link VersionIdentifier} or {@code null} for no upper bound.
   */
  VersionIdentifier getMax();

  /**
   * @return the {@link BoundaryType} defining whether the boundaries of the range are inclusive or exclusive.
   */
  default BoundaryType getBoundaryType() {

    return BoundaryType.CLOSED;
  }

  /**
   * @return {@code true} if this a version pattern (e.g. "17*" or "17.*") or {@link VersionRange}, {@code false} otherwise (this is a {@link VersionIdentifier}
   *     representing a concrete version number).
   */
  boolean isPattern();

  /**
   * @param version the {@link VersionIdentifier} to check.
   * @return {@code true} if the given {@link VersionIdentifier} is contained in this {@link VersionRange}, {@code false} otherwise.
   */
  boolean contains(VersionIdentifier version);

}
