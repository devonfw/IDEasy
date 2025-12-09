package com.devonfw.tools.ide.version;

import java.util.List;
import java.util.Objects;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.log.IdeLogger;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Data-type to represent a {@link VersionIdentifier} in a structured way and allowing {@link #compareVersion(VersionIdentifier) comparison} of
 * {@link VersionIdentifier}s.
 */
public final class VersionIdentifier implements VersionObject<VersionIdentifier>, GenericVersionRange {

  /** {@link VersionIdentifier} "*" that will resolve to the latest stable version. */
  public static final VersionIdentifier LATEST = new VersionIdentifier(VersionSegment.of("*"));

  /** {@link VersionIdentifier} "*!" that will resolve to the latest snapshot. */
  public static final VersionIdentifier LATEST_UNSTABLE = VersionIdentifier.of("*!");

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
   * Resolves a version pattern against a list of available versions.
   *
   * @param version the version pattern to resolve
   * @param versions the
   *     {@link com.devonfw.tools.ide.tool.repository.ToolRepository#getSortedVersions(String, String, ToolCommandlet) available versions, sorted in descending
   *     order}.
   * @param logger the {@link IdeLogger}.
   * @return the resolved version
   */
  public static VersionIdentifier resolveVersionPattern(GenericVersionRange version, List<VersionIdentifier> versions, IdeLogger logger) {
    if (version == null) {
      version = LATEST;
    }
    if (!version.isPattern()) {
      for (VersionIdentifier vi : versions) {
        if (vi.equals(version)) {
          logger.debug("Resolved version {} to version {}", version, vi);
          return vi;
        }
      }
    }
    for (VersionIdentifier vi : versions) {
      if (version.contains(vi)) {
        logger.debug("Resolved version pattern {} to version {}", version, vi);
        return vi;
      }
    }
    throw new CliException(
        "Could not find any version matching '" + version + "' - there are " + versions.size() + " version(s) available but none matched!");
  }

  /**
   * @return the first {@link VersionSegment} of this {@link VersionIdentifier}. To get other segments use {@link VersionSegment#getNextOrEmpty()} or
   *     {@link VersionSegment#getNextOrNull()}.
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

  @Override
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
   * @return {@code true} if this is a stable version, {@code false} otherwise.
   * @see VersionLetters#isStable()
   */
  public boolean isStable() {

    return this.developmentPhase.isStable();
  }

  /**
   * @return the {@link VersionLetters#isDevelopmentPhase() development phase} of this {@link VersionIdentifier}. Will be {@link VersionLetters#EMPTY} if no
   *     development phase is specified in any {@link VersionSegment} and will be {@link VersionLetters#UNDEFINED} if more than one
   *     {@link VersionLetters#isDevelopmentPhase() development phase} is specified (e.g. "1.0-alpha1.rc2").
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
   * @return {@code true} if this {@link VersionIdentifier} is equal to the given {@link VersionIdentifier} or this {@link VersionIdentifier} is a pattern
   *     version (e.g. "17*" or "17.*") and the given {@link VersionIdentifier} matches to that pattern.
   */
  public boolean matches(VersionIdentifier other) {

    if (other == null) {
      return false;
    }
    VersionSegment thisSegment = this.start;
    VersionSegment otherSegment = other.start;
    while (true) {
      VersionMatchResult matchResult = thisSegment.matches(otherSegment);
      if (matchResult == VersionMatchResult.MATCH) {
        return true;
      } else if (matchResult == VersionMatchResult.MISMATCH) {
        return false;
      }
      thisSegment = thisSegment.getNextOrEmpty();
      otherSegment = otherSegment.getNextOrEmpty();
    }
  }

  /**
   * Increment the specified segment. For examples see {@code VersionIdentifierTest.testIncrement()}.
   *
   * @param digitNumber the index of the {@link VersionSegment} to increment. All segments before will remain untouched and all following segments will be
   *     set to zero.
   * @param keepLetters {@code true} to keep {@link VersionSegment#getLetters() letters} from modified segments, {@code false} to drop them.
   * @return the incremented {@link VersionIdentifier}.
   */
  public VersionIdentifier incrementSegment(int digitNumber, boolean keepLetters) {

    if (isPattern()) {
      throw new IllegalStateException("Cannot increment version pattern: " + toString());
    }
    VersionSegment newStart = this.start.increment(digitNumber, keepLetters);
    return new VersionIdentifier(newStart);
  }

  /**
   * Increment the first digit (major version).
   *
   * @param keepLetters {@code true} to keep {@link VersionSegment#getLetters() letters} from modified segments, {@code false} to drop them.
   * @return the incremented {@link VersionIdentifier}.
   * @see #incrementSegment(int, boolean)
   */
  public VersionIdentifier incrementMajor(boolean keepLetters) {
    return incrementSegment(0, keepLetters);
  }

  /**
   * Increment the second digit (minor version).
   *
   * @param keepLetters {@code true} to keep {@link VersionSegment#getLetters() letters} from modified segments, {@code false} to drop them.
   * @return the incremented {@link VersionIdentifier}.
   * @see #incrementSegment(int, boolean)
   */
  public VersionIdentifier incrementMinor(boolean keepLetters) {
    return incrementSegment(1, keepLetters);
  }

  /**
   * Increment the third digit (patch or micro version).
   *
   * @param keepLetters {@code true} to keep {@link VersionSegment#getLetters() letters} from modified segments, {@code false} to drop them.
   * @return the incremented {@link VersionIdentifier}.
   * @see #incrementSegment(int, boolean)
   */
  public VersionIdentifier incrementPatch(boolean keepLetters) {
    return incrementSegment(2, keepLetters);
  }

  /**
   * Increment the last segment.
   *
   * @param keepLetters {@code true} to keep {@link VersionSegment#getLetters() letters} from modified segments, {@code false} to drop them.
   * @return the incremented {@link VersionIdentifier}.
   * @see #incrementSegment(int, boolean)
   */
  public VersionIdentifier incrementLastDigit(boolean keepLetters) {

    return incrementSegment(this.start.countDigits() - 1, keepLetters);
  }

  @Override
  public VersionIdentifier getMin() {

    return this;
  }

  @Override
  public VersionIdentifier getMax() {

    return this;
  }

  @Override
  public boolean contains(VersionIdentifier version) {

    return matches(version);
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
  @JsonSerialize
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
  @JsonCreator
  public static VersionIdentifier of(String version) {

    if (version == null) {
      return null;
    }
    version = version.trim();
    if (version.equals("latest") || version.equals("*")) {
      return VersionIdentifier.LATEST;
    }
    assert !version.contains(" ") && !version.contains("\n") && !version.contains("\t") : version;
    VersionSegment startSegment = VersionSegment.of(version);
    if (startSegment == null) {
      return null;
    }
    return new VersionIdentifier(startSegment);
  }

  /**
   * @param v1 the first {@link VersionIdentifier}.
   * @param v2 the second {@link VersionIdentifier}.
   * @param treatNullAsNegativeInfinity {@code true} to treat {@code null} as negative infinity, {@code false} otherwise (positive infinity).
   * @return the null-safe {@link #compareVersion(VersionIdentifier) comparison} of the two {@link VersionIdentifier}s.
   */
  public static VersionComparisonResult compareVersion(VersionIdentifier v1, VersionIdentifier v2, boolean treatNullAsNegativeInfinity) {

    if (v1 == null) {
      if (v2 == null) {
        return VersionComparisonResult.EQUAL;
      } else if (treatNullAsNegativeInfinity) {
        return VersionComparisonResult.LESS;
      }
      return VersionComparisonResult.GREATER;
    } else if (v2 == null) {
      if (treatNullAsNegativeInfinity) {
        return VersionComparisonResult.GREATER;
      }
      return VersionComparisonResult.LESS;
    }
    return v1.compareVersion(v2);
  }

}
