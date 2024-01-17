package com.devonfw.tools.ide.version;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Container for a range of versions. The lower and upper bounds can be exclusive or inclusive. If a bound is null, it
 * means that this direction is unbounded. The boolean defining whether this bound is inclusive or exclusive is ignored
 * in this case.
 */
public final class VersionRange implements Comparable<VersionRange> {

  private final VersionIdentifier min;

  private final VersionIdentifier max;

  private final boolean leftIsExclusive;

  private final boolean rightIsExclusive;

  private static final String VERSION_SEPARATOR = ">";

  private static final String START_EXCLUDING_PREFIX = "(";

  private static final String START_INCLUDING_PREFIX = "[";

  private static final String END_EXCLUDING_SUFFIX = ")";

  private static final String END_INCLUDING_SUFFIX = "]";

  public static String getVersionSeparator() {

    return VERSION_SEPARATOR;
  }

  public static String getStartExcludingPrefix() {

    return START_EXCLUDING_PREFIX;
  }

  public static String getStartIncludingPrefix() {

    return START_INCLUDING_PREFIX;
  }

  public static String getEndExcludingSuffix() {

    return END_EXCLUDING_SUFFIX;
  }

  public static String getEndIncludingSuffix() {

    return END_INCLUDING_SUFFIX;
  }

  /**
   * The constructor.
   *
   * @param min the {@link #getMin() minimum}.
   * @param max the {@link #getMax() maximum}.
   */
  public VersionRange(VersionIdentifier min, VersionIdentifier max) {

    super();
    this.min = min;
    this.max = max;
    this.leftIsExclusive = false;
    this.rightIsExclusive = false;
  }

  /**
   * The constructor.
   *
   * @param min the {@link #getMin() minimum}.
   * @param max the {@link #getMax() maximum}.
   * @param boundaryType the {@link BoundaryType} defining whether the boundaries of the range are inclusive or
   *        exclusive.
   */
  public VersionRange(VersionIdentifier min, VersionIdentifier max, BoundaryType boundaryType) {

    super();
    this.min = min;
    this.max = max;
    this.leftIsExclusive = BoundaryType.LEFT_OPEN.equals(boundaryType) || BoundaryType.OPEN.equals(boundaryType);
    this.rightIsExclusive = BoundaryType.RIGHT_OPEN.equals(boundaryType) || BoundaryType.OPEN.equals(boundaryType);
  }

  /**
   * The constructor.
   *
   * @param min the {@link #getMin() minimum}.
   * @param max the {@link #getMax() maximum}.
   * @param leftIsExclusive - {@code true} if the {@link #getMin() minimum} is exclusive, {@code false} otherwise.
   * @param rightIsExclusive - {@code true} if the {@link #getMax() maximum} is exclusive, {@code false} otherwise.
   */
  public VersionRange(VersionIdentifier min, VersionIdentifier max, boolean leftIsExclusive, boolean rightIsExclusive) {

    super();
    this.min = min;
    this.max = max;
    this.leftIsExclusive = leftIsExclusive;
    this.rightIsExclusive = rightIsExclusive;
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
   * @return {@code true} if the {@link #getMin() minimum} is exclusive, {@code false} otherwise.
   */
  public boolean isLeftExclusive() {

    return this.leftIsExclusive;
  }

  /**
   * @return {@code true} if the {@link #getMax() maximum} is exclusive, {@code false} otherwise.
   */
  public boolean isRightExclusive() {

    return this.rightIsExclusive;
  }

  /**
   * @return the {@link BoundaryType} defining whether the boundaries of the range are inclusive or exclusive.
   */
  public BoundaryType getBoundaryType() {

    if (this.leftIsExclusive && this.rightIsExclusive) {
      return BoundaryType.OPEN;
    } else if (this.leftIsExclusive) {
      return BoundaryType.LEFT_OPEN;
    } else if (this.rightIsExclusive) {
      return BoundaryType.RIGHT_OPEN;
    } else {
      return BoundaryType.CLOSED;
    }
  }

  /**
   * @param version the {@link VersionIdentifier} to check.
   * @return {@code true} if the given {@link VersionIdentifier} is contained in this {@link VersionRange},
   *         {@code false} otherwise.
   */
  public boolean contains(VersionIdentifier version) {

    if (this.min != null) {
      VersionComparisonResult compareMin = version.compareVersion(this.min);
      if (compareMin.isLess()) {
        return false;
      } else if (compareMin.isEqual() && this.leftIsExclusive) {
        return false;
      }
    }
    if (this.max != null) {
      VersionComparisonResult compareMax = version.compareVersion(this.max);
      if (compareMax.isGreater()) {
        return false;
      } else if (compareMax.isEqual() && this.rightIsExclusive) {
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
      return this.leftIsExclusive == o.leftIsExclusive ? 0 : this.leftIsExclusive ? 1 : -1;
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
    if (this.min == null && this.max == null) {
      return o.min == null && o.max == null;
    } else if (this.min == null) {
      return o.min == null && this.max.equals(o.max) && this.rightIsExclusive == o.rightIsExclusive;
    } else if (this.max == null) {
      return this.min.equals(o.min) && o.max == null && this.leftIsExclusive == o.leftIsExclusive;
    }
    return this.min.equals(o.min) && this.leftIsExclusive == o.leftIsExclusive && this.max.equals(o.max)
        && this.rightIsExclusive == o.rightIsExclusive;
  }

  @Override
  @JsonValue
  public String toString() {

    StringBuilder sb = new StringBuilder();
    sb.append(this.leftIsExclusive ? START_EXCLUDING_PREFIX : START_INCLUDING_PREFIX);
    if (this.min != null) {
      sb.append(this.min);
    }
    sb.append(VERSION_SEPARATOR);
    if (this.max != null) {
      sb.append(this.max);
    }
    sb.append(this.rightIsExclusive ? END_EXCLUDING_SUFFIX : END_INCLUDING_SUFFIX);
    return sb.toString();
  }

  /**
   * @param value the {@link #toString() string representation} of a {@link VersionRange} to parse.
   * @return the parsed {@link VersionRange}.
   */
  @JsonCreator
  public static VersionRange of(String value) {

    boolean leftIsExclusive = value.startsWith(START_EXCLUDING_PREFIX);
    boolean rightIsExclusive = value.endsWith(END_EXCLUDING_SUFFIX);
    value = removeAffixes(value);

    int index = value.indexOf(VERSION_SEPARATOR);
    if (index == -1) {
      return null; // log warning?
    }

    VersionIdentifier min = null;
    if (index > 0) {
      min = VersionIdentifier.of(value.substring(0, index));
    }
    VersionIdentifier max = null;
    String maxString = value.substring(index + 1);
    if (!maxString.isEmpty()) {
      max = VersionIdentifier.of(maxString);
    }
    return new VersionRange(min, max, leftIsExclusive, rightIsExclusive);
  }

  private static String removeAffixes(String value) {

    if (value.startsWith(START_EXCLUDING_PREFIX)) {
      value = value.substring(START_EXCLUDING_PREFIX.length());
    } else if (value.startsWith(START_INCLUDING_PREFIX)) {
      value = value.substring(START_INCLUDING_PREFIX.length());
    }
    if (value.endsWith(END_EXCLUDING_SUFFIX)) {
      value = value.substring(0, value.length() - END_EXCLUDING_SUFFIX.length());
    } else if (value.endsWith(END_INCLUDING_SUFFIX)) {
      value = value.substring(0, value.length() - END_EXCLUDING_SUFFIX.length());
    }
    return value;
  }

}
