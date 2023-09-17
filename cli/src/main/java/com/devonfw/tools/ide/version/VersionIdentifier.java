package com.devonfw.tools.ide.version;

import java.util.Objects;

/**
 * Data-type to represent a {@link VersionIdentifier} in a structured way and allowing
 * {@link #compareVersion(VersionIdentifier) comparison} of {@link VersionIdentifier}s.
 */
public final class VersionIdentifier implements VersionObject<VersionIdentifier> {

  /** {@link VersionIdentifier} "*" that will resolve to the latest stable version. */
  public static final VersionIdentifier LATEST = VersionIdentifier.of("*");

  private final VersionSegment start;

  private final VersionLetters developmentPhase;

  private final boolean valid;

  private VersionIdentifier(VersionSegment start) {

    super();
    Objects.requireNonNull(start);
    this.start = start;
    boolean isValid = this.start.getSeparator().isEmpty() && this.start.getLettersString().isEmpty();
    boolean hasPositiveNumber = false;
    VersionLetters dev = VersionLetters.EMPTY;
    VersionSegment segment = this.start;
    while (segment != null) {
      if (!segment.isValid()) {
        isValid = false;
      } else if (segment.getNumber() > 0) {
        hasPositiveNumber = true;
      }
      VersionLetters segmentLetters = segment.getLetters();
      if (segmentLetters.isDevelopmentPhase()) {
        if (dev.isEmpty()) {
          dev = segmentLetters;
        } else {
          dev = VersionLetters.UNDEFINED;
          isValid = false;
        }
      }
      segment = segment.getNextOrNull();
    }
    this.developmentPhase = dev;
    this.valid = isValid && hasPositiveNumber;
  }

  /**
   * @return the first {@link VersionSegment} of this {@link VersionIdentifier}. To get other segments use
   *         {@link VersionSegment#getNextOrEmpty()} or {@link VersionSegment#getNextOrNull()}.
   */
  public VersionSegment getStart() {

    return this.start;
  }

  /**
   * A valid {@link VersionIdentifier} has to meet the following requirements:
   * <ul>
   * <li>All {@link VersionSegment segments} themselves are {@link VersionSegment#isValid() valid}.</li>
   * <li>The {@link #getStart() start} {@link VersionSegment segment} shall have an {@link String#isEmpty() empty}
   * {@link VersionSegment#getSeparator() separator} (e.g. ".1.0" or "-1-2" are not considered valid).</li>
   * <li>The {@link #getStart() start} {@link VersionSegment segment} shall have an {@link String#isEmpty() empty}
   * {@link VersionSegment#getLettersString() letter-sequence} (e.g. "RC1" or "beta" are not considered valid).</li>
   * <li>Have at least one {@link VersionSegment segment} with a positive {@link VersionSegment#getNumber() number}
   * (e.g. "0.0.0" or "0.alpha" are not considered valid).</li>
   * <li>Have at max one {@link VersionSegment segment} with a {@link VersionSegment#getPhase() phase} that is a real
   * {@link VersionPhase#isDevelopmentPhase() development phase} (e.g. "1.alpha1.beta2" or "1.0.rc1-milestone2" are not
   * considered valid).</li>
   * <li>It is NOT a {@link #isPattern() pattern}.</li>
   * </ul>
   */
  @Override
  public boolean isValid() {

    return this.valid;
  }

  /**
   * Determines if this {@link VersionIdentifier} is a pattern (e.g. "17*" or "17.*").
   *
   * @return {@code true} if this {@link VersionIdentifier} is a pattern and not a normal version or in other words if
   *         it {@link #getStart() has} a {@link VersionSegment segment} that {@link VersionSegment#isPattern() is a
   *         pattern}, {@code false} otherwise.
   */
  public boolean isPattern() {

    VersionSegment segment = this.start;
    while (segment != null) {
      if (segment.isPattern()) {
        return true;
      }
      segment = segment.getNextOrNull();
    }
    return false;
  }

  /**
   * @return the {@link VersionLetters#isDevelopmentPhase() development phase} of this {@link VersionIdentifier}. Will
   *         be {@link VersionLetters#EMPTY} if no development phase is specified in any {@link VersionSegment} and will
   *         be {@link VersionLetters#UNDEFINED} if more than one {@link VersionLetters#isDevelopmentPhase() development
   *         phase} is specified (e.g. "1.0-alpha1.rc2").
   */
  public VersionLetters getDevelopmentPhase() {

    return this.developmentPhase;
  }

  @Override
  public VersionComparisonResult compareVersion(VersionIdentifier other) {

    if (other == null) {
      return VersionComparisonResult.GREATER_UNSAFE;
    }
    VersionSegment thisSegment = this.start;
    VersionSegment otherSegment = other.start;
    VersionComparisonResult result = null;
    boolean unsafe = false;
    boolean todo = true;
    do {
      result = thisSegment.compareVersion(otherSegment);
      if (result.isEqual()) {
        if (thisSegment.isEmpty() && otherSegment.isEmpty()) {
          todo = false;
        } else if (result.isUnsafe()) {
          unsafe = true;
        }
      } else {
        todo = false;
      }
      thisSegment = thisSegment.getNextOrEmpty();
      otherSegment = otherSegment.getNextOrEmpty();
    } while (todo);
    if (unsafe) {
      return result.withUnsafe();
    }
    return result;
  }

  /**
   * @param other the {@link VersionIdentifier} to be matched.
   * @return {@code true} if this {@link VersionIdentifier} is equal to the given {@link VersionIdentifier} or this
   *         {@link VersionIdentifier} is a pattern version (e.g. "17*" or "17.*") and the given
   *         {@link VersionIdentifier} matches to that pattern.
   */
  public boolean matches(VersionIdentifier other) {

    if (other == null) {
      return false;
    }
    VersionSegment thisSegment = this.start;
    VersionSegment otherSegment = other.start;
    boolean todo = true;
    do {
      VersionMatchResult matchResult = thisSegment.matches(otherSegment);
      if (matchResult == VersionMatchResult.MATCH) {
        return true;
      } else if (matchResult == VersionMatchResult.MISMATCH) {
        return false;
      }
      thisSegment = thisSegment.getNextOrEmpty();
      otherSegment = otherSegment.getNextOrEmpty();
    } while (todo);
    return true;
  }

  @Override
  public int hashCode() {

    VersionSegment segment = this.start;
    int hash = 1;
    while (segment != null) {
      hash = hash * 31 + segment.hashCode();
      segment = segment.getNextOrNull();
    }
    return hash;
  }

  @Override
  public boolean equals(Object obj) {

    if (obj == this) {
      return true;
    } else if (!(obj instanceof VersionIdentifier)) {
      return false;
    }
    VersionIdentifier other = (VersionIdentifier) obj;
    return Objects.equals(this.start, other.start);
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();
    VersionSegment segment = this.start;
    while (segment != null) {
      sb.append(segment.toString());
      segment = segment.getNextOrNull();
    }
    return sb.toString();
  }

  /**
   * @param version the {@link #toString() string representation} of the {@link VersionIdentifier} to parse.
   * @return the parsed {@link VersionIdentifier}.
   */
  public static VersionIdentifier of(String version) {

    if (version.equals("latest")) {
      return VersionIdentifier.LATEST;
    }
    return new VersionIdentifier(VersionSegment.of(version));
  }

}
