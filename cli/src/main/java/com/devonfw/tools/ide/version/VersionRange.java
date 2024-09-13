package com.devonfw.tools.ide.version;

import java.util.Objects;

/**
 * Container for a range of versions. The lower and upper bounds can be exclusive or inclusive. If a bound is null, it means that this direction is unbounded.
 * The boolean defining whether this bound is inclusive or exclusive is ignored in this case.
 */
public final class VersionRange implements Comparable<VersionRange> {

  /** Property-name of {@link #getMin()}. */
  public static final String PROPERTY_MIN = "min";

  /** Property-name of {@link #getMax()}. */
  public static final String PROPERTY_MAX = "max";

  /** Property-name of {@link #getBoundaryType()}. */
  public static final String PROPERTY_TYPE = "type";

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
   */
  public VersionRange(VersionIdentifier min, VersionIdentifier max) {

    this(min, max, BoundaryType.CLOSED);
  }

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

  /**
   * @return the minimum {@link VersionIdentifier} or {@code null} for no lower bound.
   */
  public VersionIdentifier getMin() {

    return this.min;
  }

  /**
   * @return the maximum {@link VersionIdentifier} or {@code null} for no upper bound.
   */
  public VersionIdentifier getMax() {

    return this.max;
  }

  /**
   * @return the {@link BoundaryType} defining whether the boundaries of the range are inclusive or exclusive.
   */
  public BoundaryType getBoundaryType() {

    return this.boundaryType;
  }

  /**
   * @param version the {@link VersionIdentifier} to check.
   * @return {@code true} if the given {@link VersionIdentifier} is contained in this {@link VersionRange}, {@code false} otherwise.
   */
  public boolean contains(VersionIdentifier version) {

    if (this.min != null) {
      VersionComparisonResult compareMin = version.compareVersion(this.min);
      if (compareMin.isLess()) {
        return false;
      } else if (compareMin.isEqual() && this.boundaryType.isLeftExclusive()) {
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
      if (!minString.isEmpty()) {
        min = VersionIdentifier.of(minString);
      }
      String maxString = value.substring(index + 1);
      if (!maxString.isEmpty()) {
        max = VersionIdentifier.of(maxString);
      }
    }
    if (isleftExclusive == null) {
      isleftExclusive = Boolean.valueOf(min == null);
    }
    if (isRightExclusive == null) {
      isRightExclusive = Boolean.valueOf(max == null);
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
