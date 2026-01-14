package com.devonfw.tools.ide.version;

import java.util.Objects;

/**
 * Container for a range of versions. The lower and upper bounds can be exclusive or inclusive. If a bound is null, it means that this direction is unbounded.
 * The boolean defining whether this bound is inclusive or exclusive is ignored in this case.
 */
public final class VersionRange implements Comparable<VersionRange>, GenericVersionRange {

  /** The unbounded {@link VersionRange} instance. */
  public static final VersionRange UNBOUNDED = new VersionRange(null, null, BoundaryType.OPEN);

  private static final String VERSION_SEPARATOR = ",";

  final VersionIdentifier min;

  final VersionIdentifier max;

  final BoundaryType boundaryType;

  /**
   * The constructor.
   *
   * @param min the {@link #getMin() minimum}.
   * @param max the {@link #getMax() maximum}.
   * @param boundaryType the {@link BoundaryType} defining whether the boundaries of the range are inclusive or exclusive.
   */
  private VersionRange(VersionIdentifier min, VersionIdentifier max, BoundaryType boundaryType) {

    super();
    Objects.requireNonNull(boundaryType);
    this.min = min;
    this.max = max;
    this.boundaryType = boundaryType;
    if ((min != null) && (max != null) && min.isGreater(max)) {
      throw new IllegalArgumentException(toString());
    } else if ((min == null) && !boundaryType.isLeftExclusive()) {
      throw new IllegalArgumentException(toString());
    } else if ((max == null) && !boundaryType.isRightExclusive()) {
      throw new IllegalArgumentException(toString());
    }

  }

  @Override
  public VersionIdentifier getMin() {

    return this.min;
  }

  @Override
  public VersionIdentifier getMax() {

    return this.max;
  }

  @Override
  public BoundaryType getBoundaryType() {

    return this.boundaryType;
  }

  @Override
  public boolean isPattern() {

    return true;
  }

  @Override
  public boolean contains(VersionIdentifier version) {

    VersionSegment start = version.getStart();
    if ((start.getNumber() == -1) && start.isPattern()) {
      return true; // * and *! are always contained
    }
    if (this.min != null) {
      VersionComparisonResult compareMin = version.compareVersion(this.min);
      if (compareMin.isLess()) {
        return false;
      } else if (compareMin.isEqual() && this.boundaryType.isLeftExclusive() && !version.isPattern()) {
        return false;
      }
    }
    if (this.max != null) {
      VersionComparisonResult compareMax = version.compareVersion(this.max);
      if (compareMax.isGreater()) {
        return false;
      } else if (compareMax.isEqual() && this.boundaryType.isRightExclusive()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int compareTo(VersionRange o) {

    if (this.min == null) {
      if (o == null) {
        return 1; // should never happen
      } else if (o.min == null) {
        return 0;
      }
      return -1;
    }
    int compareMins = this.min.compareTo(o.min);
    if (compareMins == 0) {
      return this.boundaryType.isLeftExclusive() == o.boundaryType.isLeftExclusive() ? 0
          : this.boundaryType.isLeftExclusive() ? 1 : -1;
    } else {
      return compareMins;
    }
  }

  /**
   * @param other the {@link VersionRange} to unite with.
   * @return the union of this with the given {@link VersionRange} or {@code null} if not {@link VersionRangeRelation#CONNECTED connected} or
   *     {@link VersionRangeRelation#OVERLAPPING overlapping}.
   */
  public VersionRange union(VersionRange other) {

    return union(other, VersionRangeRelation.CONNECTED);
  }

  /**
   * @param other the {@link VersionRange} to unite with.
   * @param minRelation the minimum {@link VersionRangeRelation} required to allow building the union instead of returning {@code null}. So if you want to
   *     build a strict union, you can pass {@link VersionRangeRelation#OVERLAPPING} so you only get a union that contains exactly what is contained in at least
   *     one of the two {@link VersionRange}s. However, you can pass {@link VersionRangeRelation#CONNECTED_LOOSELY} in order to get "[2.0,5.0]" as the union of
   *     "[2.0,2.2]" and "[2.3,5.0]".
   * @return the union of this with the given {@link VersionRange} or {@code null} if the actual {@link VersionRangeRelation} of the {@link VersionRange}s is
   *     lower than the given {@code minRelation}.
   */
  public VersionRange union(VersionRange other, VersionRangeRelation minRelation) {

    if (other == null) {
      return this;
    }
    return new VersionRangeCombination(this, other).union(minRelation);
  }

  /**
   * @param other the {@link VersionRange} to intersect with.
   * @return the intersection of this with the given {@link VersionRange} or {@code null} for empty intersection.
   */
  public VersionRange intersect(VersionRange other) {

    if (other == null) {
      return this;
    }
    return new VersionRangeCombination(this, other).intersection();
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    } else if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }
    VersionRange o = (VersionRange) obj;
    if (this.boundaryType != o.boundaryType) {
      return false;
    } else if (!Objects.equals(this.min, o.min)) {
      return false;
    } else if (!Objects.equals(this.max, o.max)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();
    sb.append(this.boundaryType.getPrefix());
    if (this.min != null) {
      sb.append(this.min);
    }
    if (this.max == null || !this.max.equals(this.min)) { // [1.0] instead of [1.0,1.0]
      sb.append(VERSION_SEPARATOR);
      if (this.max != null) {
        sb.append(this.max);
      }
    }
    sb.append(this.boundaryType.getSuffix());
    return sb.toString();
  }

  /**
   * @param value the {@link #toString() string representation} of a {@link VersionRange} to parse.
   * @return the parsed {@link VersionRange}.
   */
  public static VersionRange of(String value) {

    return of(value, false);
  }

  /**
   * @param value the {@link #toString() string representation} of a {@link VersionRange} to parse.
   * @param tolerance {@code true} to enable tolerant parsing so we can read garbage (e.g. form JSON) without failing.
   * @return the parsed {@link VersionRange}.
   */
  public static VersionRange of(String value, boolean tolerance) {

    Boolean isleftExclusive = null;
    Boolean isRightExclusive = null;
    if (value.startsWith(BoundaryType.START_EXCLUDING_PREFIX)) {
      isleftExclusive = Boolean.TRUE;
      value = value.substring(BoundaryType.START_EXCLUDING_PREFIX.length());
    } else if (value.startsWith(BoundaryType.START_INCLUDING_PREFIX)) {
      isleftExclusive = Boolean.FALSE;
      value = value.substring(BoundaryType.START_INCLUDING_PREFIX.length());
    }
    if (value.endsWith(BoundaryType.END_EXCLUDING_SUFFIX)) {
      isRightExclusive = Boolean.TRUE;
      value = value.substring(0, value.length() - BoundaryType.END_EXCLUDING_SUFFIX.length());
    } else if (value.endsWith(BoundaryType.END_INCLUDING_SUFFIX)) {
      isRightExclusive = Boolean.FALSE;
      value = value.substring(0, value.length() - BoundaryType.END_INCLUDING_SUFFIX.length());
    }
    VersionIdentifier min = null;
    VersionIdentifier max = null;
    int index = value.indexOf(VERSION_SEPARATOR);
    if (index < 0) {
      min = VersionIdentifier.of(value);
      max = min;
    } else {
      String minString = value.substring(0, index).trim();
      if (!minString.isBlank()) {
        min = VersionIdentifier.of(minString);
        if (min == VersionIdentifier.LATEST) {
          min = null;
        }
      }
      String maxString = value.substring(index + 1).trim();
      if (!maxString.isBlank()) {
        max = VersionIdentifier.of(maxString);
        if (max == VersionIdentifier.LATEST) {
          max = null;
        }
      }
    }
    if ((isleftExclusive == null) || (tolerance && (min == null))) {
      isleftExclusive = min == null;
    }
    if ((isRightExclusive == null) || (tolerance && (max == null))) {
      isRightExclusive = max == null;
    }

    if ((min == null) && (max == null) && isleftExclusive && isRightExclusive) {
      return UNBOUNDED;
    }
    return new VersionRange(min, max, BoundaryType.of(isleftExclusive, isRightExclusive));
  }

  /**
   * @param min the {@link #getMin() minimum}.
   * @param max the {@link #getMax() maximum}.
   * @param type the {@link BoundaryType} defining whether the boundaries of the range are inclusive or exclusive.
   * @return the {@link VersionRange} created from the given values.
   */
  public static VersionRange of(VersionIdentifier min, VersionIdentifier max, BoundaryType type) {

    if (type == null) {
      type = BoundaryType.of(min == null, max == null);
    }
    if ((min == null) && (max == null)) {
      assert (type == BoundaryType.OPEN);
      return UNBOUNDED;
    }
    return new VersionRange(min, max, type);
  }

}
