package com.devonfw.tools.ide.version;

import java.util.Objects;

/**
 * Container for a range of versions. The lower and upper bounds can be exclusive or inclusive. If a bound is null, it means that this direction is unbounded.
 * The boolean defining whether this bound is inclusive or exclusive is ignored in this case.
 */
public final class VersionRange implements Comparable<VersionRange>, GenericVersionRange {

  /** The unbounded {@link VersionRange} instance. */
  public static final VersionRange UNBOUNDED = new VersionRange(null, null, BoundaryType.OPEN);

  private final VersionIdentifier min;

  private final VersionIdentifier max;

  private final BoundaryType boundaryType;

  private static final String VERSION_SEPARATOR = ",";

  /**
   * The constructor.
   *
   * @param min the {@link #getMin() minimum}.
   * @param max the {@link #getMax() maximum}.
   * @param boundaryType the {@link BoundaryType} defining whether the boundaries of the range are inclusive or exclusive.
   */
  public VersionRange(VersionIdentifier min, VersionIdentifier max, BoundaryType boundaryType) {

    super();
    Objects.requireNonNull(boundaryType);
    this.min = min;
    this.max = max;
    this.boundaryType = boundaryType;

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
      if (this.boundaryType.isLeftExclusive() == o.boundaryType.isLeftExclusive()) {
        return 0;
      } else {
        if (this.boundaryType.isLeftExclusive()) {
          return 1;
        } else {
          return -1;
        }
      }
    } else {
      return compareMins;
    }
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
    sb.append(VERSION_SEPARATOR);
    if (this.max != null) {
      sb.append(this.max);
    }
    sb.append(this.boundaryType.getSuffix());
    return sb.toString();
  }

  /**
   * @param value the {@link #toString() string representation} of a {@link VersionRange} to parse.
   * @return the parsed {@link VersionRange}.
   */
  public static VersionRange of(String value) {

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
      String minString = value.substring(0, index);
      if (!minString.isBlank()) {
        min = VersionIdentifier.of(minString);
      }
      String maxString = value.substring(index + 1);
      if (!maxString.isBlank()) {
        max = VersionIdentifier.of(maxString);
      }
    }
    if (isleftExclusive == null) {
      isleftExclusive = Boolean.valueOf(min == null);
    }
    if (isRightExclusive == null) {
      isRightExclusive = Boolean.valueOf(max == null);
    }
    if ((min == null) && (max == null) && isleftExclusive && isRightExclusive) {
      return UNBOUNDED;
    }
    return new VersionRange(min, max, BoundaryType.of(isleftExclusive.booleanValue(), isRightExclusive.booleanValue()));
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
