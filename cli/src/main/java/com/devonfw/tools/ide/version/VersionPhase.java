package com.devonfw.tools.ide.version;

/**
 * Enum with the available development phases of a {@link VersionSegment#getLettersString() letter-sequence} from a
 * {@link VersionSegment}.
 */
public enum VersionPhase implements AbstractVersionPhase {

  /** An undefined {@link VersionPhase} such as "apple" or "banana". */
  UNDEFINED,

  /** A revision number (actually not a development phase like {@link #UNDEFINED}). */
  REVISION(Boolean.TRUE, "revision", "rev"),

  /** A snapshot version from development (e.g. "-SNAPSHOT" suffix in maven). */
  SNAPSHOT(Boolean.FALSE, "snapshot", "dev"),

  /** A nightly build version from continuous-integration (CI) process. */
  NIGHTLY("nightly", "nb", "ci"),

  /** A version for alpha testing (unstable). */
  ALPHA(Boolean.TRUE, "alpha", "alph", "alp", "a", "unstable"),

  /**
   * A version for beta testing (should not have fundamental bugs like maybe in {@link #ALPHA} but still needs more
   * testing).
   */
  BETA(Boolean.TRUE, "beta", "bet", "test"),

  /**
   * A "b" can be {@link #BETA} or {@link #BUILD} depending on the version scheme of the product. Please avoid such
   * ambiguous {@link VersionSegment#getLetters() letter-sequence}.
   */
  BETA_OR_BUILD(Boolean.TRUE, "b"),

  /** A build number (actually not a development phase like {@link #UNDEFINED}). */
  BUILD(Boolean.TRUE, "build"),

  /** A version that has passed quality gates but is in a phase for further end-user testing to collect feedback. */
  MILESTONE(Boolean.TRUE, "m", "milestone"),

  /**
   * A version close to the {@link #RELEASE} with expected good quality for final end-user testing and only relevant
   * bugs will be fixed if found to come to the final release (typically within a predefined time-frame).
   */
  RELEASE_CANDIDATE(Boolean.TRUE, "rc", "release-candidate", "candidate"),

  /** No phase specified. */
  NONE(Boolean.TRUE, ""),

  /** An official release that should have good quality. */
  RELEASE("release", "ga", "rel"),

  /** A bug-fix version from the previous release including important fix(es). */
  BUG_FIX(Boolean.TRUE, "bugfix", "fix", "quickfix"),

  /** An fix release, similar to {@link #BUG_FIX} but more urgent. */
  HOT_FIX(Boolean.TRUE, "hotfix", "hf");

  private final Boolean hasNumber;

  private final String[] ids;

  private VersionPhase(String... ids) {

    this.hasNumber = null;
    this.ids = ids;
  }

  private VersionPhase(Boolean hasNumber, String... ids) {

    this.hasNumber = hasNumber;
    this.ids = ids;
  }

  @Override
  public boolean isDevelopmentPhase() {

    return (this != UNDEFINED) && (this != NONE) && (this != REVISION) && (this != BUILD);
  }

  /**
   * A valid combination of {@link VersionPhase} and {@link VersionSegment#getNumber() segment number} has to meet the
   * following requirements:
   * <ul>
   * <li>{@link VersionPhase} is not {@link #UNDEFINED}.</li>
   * <li>{@link VersionPhase} is not {@link #BETA_OR_BUILD} ("b" is ambiguous and therefore discouraged).</li>
   * <li>The {@link VersionPhase}s {@link #REVISION}, {@link #ALPHA}, {@link #BETA}, {@link #BUILD}, {@link #MILESTONE},
   * {@link #RELEASE_CANDIDATE}, {@link #NONE}, {@link #BUG_FIX}, or {@link #HOT_FIX} have to be followed by
   * {@link VersionSegment#getDigits() digits} ({@link VersionSegment#getNumber() segment number} >= 0).</li>
   * <li>The {@link VersionPhase} {@link #SNAPSHOT} must not be followed by {@link VersionSegment#getDigits() digits}
   * ({@link VersionSegment#getNumber() segment number} == -1).</li>
   * </ul>
   *
   * @param number the {@link VersionSegment#getNumber() segment number}.
   * @return {@code true} if the combination of this {@link VersionPhase} with the given {@code number} is a valid
   *         according to version scheme best-practices.
   */
  public boolean isValid(int number) {

    if ((this == UNDEFINED) || (this == BETA_OR_BUILD)) {
      return false;
    }
    if (this.hasNumber != null) {
      if (this.hasNumber.booleanValue()) {
        return (number >= 0);
      } else {
        return (number == -1);
      }
    }
    return true;
  }

  @Override
  public boolean isUnstable() {

    if (isDevelopmentPhase()) {
      if (ordinal() < NONE.ordinal()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isStable() {

    if (ordinal() >= NONE.ordinal()) {
      return true;
    }
    return false;

  }

  /**
   * @param letters the {@link VersionSegment#getLettersString() letter-sequence} of a {@link VersionSegment} in
   *        {@link String#toLowerCase(java.util.Locale) lower-case}.
   * @return the corresponding {@link VersionPhase}. Will be {@code #UNDEFINED} if undefined (e.g. "apple" or "banana").
   */
  public static VersionPhase of(String letters) {

    for (VersionPhase phase : values()) {
      for (String id : phase.ids) {
        if (id.equals(letters)) {
          return phase;
        }
      }
    }
    return UNDEFINED;
  }

}
