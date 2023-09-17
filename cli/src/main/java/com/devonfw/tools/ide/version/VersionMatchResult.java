package com.devonfw.tools.ide.version;

/**
 * Result of {@link VersionSegment#matches(VersionSegment)}.
 */
public enum VersionMatchResult {

  /** {@link VersionSegment}s are equal and {@link VersionIdentifier#matches(VersionIdentifier)} shall continue. */
  EQUAL,

  /** {@link VersionSegment}s match (final result). */
  MATCH,

  /** {@link VersionSegment}s do not match (final result). */
  MISMATCH

}
