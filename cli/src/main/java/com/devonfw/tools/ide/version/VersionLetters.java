package com.devonfw.tools.ide.version;

import java.util.Locale;
import java.util.Objects;

/**
 * Value type for the letters of a {@link VersionSegment}.
 *
 * @see VersionSegment#getLettersString()
 */
public final class VersionLetters implements AbstractVersionPhase, VersionObject<VersionLetters> {

  /** The empty {@link VersionLetters} instance. */
  public static final VersionLetters EMPTY = new VersionLetters("");

  /** The undefined {@link VersionLetters} instance. */
  public static final VersionLetters UNDEFINED = new VersionLetters("undefined");

  private final String letters;

  private final String lettersLowerCase;

  private final VersionPhase phase;

  private final boolean prePhase;

  /**
   * The constructor.
   *
   * @param letters the {@link #getLetters() letters}.
   */
  private VersionLetters(String letters) {

    super();
    this.letters = letters;
    this.lettersLowerCase = letters.toLowerCase(Locale.ROOT);
    String phaseLetters = this.lettersLowerCase.replace('_', '-');
    if (phaseLetters.startsWith("pre")) {
      this.prePhase = true;
      int preLength = 3;
      if (phaseLetters.startsWith("pre-")) {
        preLength = 4;
      }
      phaseLetters = phaseLetters.substring(preLength);
    } else {
      this.prePhase = false;
    }
    this.phase = VersionPhase.of(phaseLetters);
  }

  /**
   * @return the letters or the empty {@link String} ("") for none. In canonical {@link VersionIdentifier}s letters
   *         indicate the development phase (e.g. "pre", "rc", "alpha", "beta", "milestone", "test", "dev", "SNAPSHOT",
   *         etc.). However, letters are technically any {@link Character#isLetter(char) letter characters} and may also
   *         be something like a code-name (e.g. "Cupcake", "Donut", "Eclair", "Froyo", "Gingerbread", "Honeycomb", "Ice
   *         Cream Sandwich", "Jelly Bean" in case of Android internals). Please note that in such case it is impossible
   *         to properly decide which version is greater than another versions. To avoid mistakes, the comparison
   *         supports a strict mode that will let the comparison fail in such case. However, by default (e.g. for
   *         {@link Comparable#compareTo(Object)}) the default {@link String#compareTo(String) string comparison}
   *         (lexicographical) is used to ensure a natural order.
   * @see #getPhase()
   */
  public String getLetters() {

    return this.letters;
  }

  /**
   * @return the {@link VersionPhase} for the {@link #getLetters() letters}. Will be {@link VersionPhase#UNDEFINED} if
   *         unknown and hence never {@code null}.
   * @see #isPrePhase()
   * @see #getLetters()
   */
  public VersionPhase getPhase() {

    return this.phase;
  }

  /**
   * @return {@code true} if the {@link #getLetters() letters} and a potential {@link #getPhase() phase} are prefixed
   *         with "pre" (e.g. in "pre-alpha"), {@code false} otherwise.
   */
  public boolean isPrePhase() {

    return this.prePhase;
  }

  @Override
  public boolean isDevelopmentPhase() {

    return this.phase.isDevelopmentPhase() || isPrePhase();
  }

  @Override
  public boolean isUnstable() {

    return this.prePhase || this.phase.isUnstable();
  }

  @Override
  public boolean isStable() {

    return !this.prePhase && this.phase.isStable();
  }

  /**
   * @return {@code true} if empty, {@code false} otherwise.
   */
  public boolean isEmpty() {

    return this.letters.isEmpty();
  }

  @Override
  public boolean isValid() {

    return true;
  }

  @Override
  public VersionComparisonResult compareVersion(VersionLetters other) {

    if (!this.lettersLowerCase.equals(other.lettersLowerCase)) {
      if ((this.phase == VersionPhase.UNDEFINED) || (other.phase == VersionPhase.UNDEFINED)) {
        if (this.lettersLowerCase.compareTo(other.lettersLowerCase) < 0) {
          return VersionComparisonResult.LESS_UNSAFE;
        } else {
          return VersionComparisonResult.GREATER_UNSAFE;
        }
      }
      if (this.phase != other.phase) {
        if (this.phase.ordinal() < other.phase.ordinal()) {
          return VersionComparisonResult.LESS;
        } else {
          return VersionComparisonResult.GREATER;
        }
      } else if (this.prePhase != other.prePhase) {
        if (this.prePhase) {
          return VersionComparisonResult.LESS;
        } else {
          return VersionComparisonResult.GREATER;
        }
      }
    }
    return VersionComparisonResult.EQUAL;
  }

  /**
   * @param other the other {@link VersionLetters} to match against.
   * @param pattern - {@code true} if the owning {@link VersionSegment} {@link VersionSegment#isPattern() is a pattern},
   *        {@code false} otherwise.
   * @return the {@link VersionMatchResult}.
   * @see VersionSegment#matches(VersionSegment)
   */
  public VersionMatchResult matches(VersionLetters other, boolean pattern) {

    if (other == null) {
      return VersionMatchResult.MISMATCH;
    }
    if (isEmpty() && other.isEmpty()) {
      return VersionMatchResult.EQUAL;
    }
    if (pattern) {
      if (this.phase != VersionPhase.NONE) {
        if (this.phase != other.phase) {
          return VersionMatchResult.MISMATCH;
        }
      }
      if (!other.lettersLowerCase.startsWith(this.lettersLowerCase)) {
        return VersionMatchResult.MISMATCH;
      }
      return VersionMatchResult.MATCH;
    } else {
      if (this.phase != other.phase) {
        return VersionMatchResult.MISMATCH;
      }
      if (!this.lettersLowerCase.equals(other.lettersLowerCase)) {
        return VersionMatchResult.MISMATCH;
      }
      return VersionMatchResult.EQUAL;
    }
  }

  @Override
  public int hashCode() {

    return this.letters.hashCode();
  }

  @Override
  public boolean equals(Object obj) {

    if (obj == this) {
      return true;
    } else if (!(obj instanceof VersionLetters)) {
      return false;
    }
    VersionLetters other = (VersionLetters) obj;
    return Objects.equals(this.letters, other.letters);
  }

  @Override
  public String toString() {

    return this.letters;
  }

  /**
   * @param letters the letters as {@link String}.
   * @return the parsed {@link VersionLetters}.
   */
  public static VersionLetters of(String letters) {

    if ((letters == null) || letters.isEmpty()) {
      return EMPTY;
    }
    return new VersionLetters(letters);
  }

}
