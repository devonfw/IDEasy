package com.devonfw.tools.ide.version;

/**
 * Abstract interface for the development phase from a {@link VersionLetters letter-sequence} of {@link VersionSegment}.
 */
public interface AbstractVersionPhase {

  /**
   * @return {@code true} if this {@link VersionPhase} is a real development phase that says something about the quality
   *         of the release, {@code false} otherwise.
   */
  boolean isDevelopmentPhase();

  /**
   * @return {@code true} if this is an unstable {@link #isDevelopmentPhase() development phase} (a pre-release).
   */
  boolean isUnstable();

  /**
   * @return {@code true} if this is a stable {@link #isDevelopmentPhase() development phase} (an official release).
   */
  boolean isStable();

}
