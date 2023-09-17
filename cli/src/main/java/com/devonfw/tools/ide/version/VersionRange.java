package com.devonfw.tools.ide.version;

/**
 * Container for a range of versions.
 */
public final class VersionRange implements Comparable<VersionRange> {

  private final VersionIdentifier min;

  private final VersionIdentifier max;

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
   * @param version the {@link VersionIdentifier} to check.
   * @return {@code true} if the given {@link VersionIdentifier} is contained in this {@link VersionRange},
   *         {@code false} otherwise.
   */
  public boolean contains(VersionIdentifier version) {

    if (this.min != null) {
      if (version.isLess(this.min)) {
        return false;
      }
    }
    if (this.max != null) {
      if (version.isGreater(this.max)) {
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
    return this.min.compareTo(o.min);
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();
    if (this.min != null) {
      sb.append(this.min);
    }
    sb.append('>');
    if (this.max != null) {
      sb.append(this.max);
    }
    return sb.toString();
  }

  /**
   * @param value the {@link #toString() string representation} of a {@link VersionRange} to parse.
   * @return the parsed {@link VersionRange}.
   */
  public static VersionRange of(String value) {

    int index = value.indexOf('>');
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
    return new VersionRange(min, max);
  }

}
