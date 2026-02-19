package com.devonfw.tools.ide.version;

import java.util.Objects;

/**
 * Represents a single segment of a {@link VersionIdentifier}.
 */
public class VersionSegment implements VersionObject<VersionSegment> {

  /** Pattern to match a any version that matches the prefix. Value is: {@value} */
  public static final String PATTERN_MATCH_ANY_VERSION = "*!";

  /** Pattern to match a {@link VersionPhase#isStable() stable} version that matches the prefix. Value is: {@value} */
  public static final String PATTERN_MATCH_ANY_STABLE_VERSION = "*";

  private static final VersionSegment EMPTY = new VersionSegment("", "", "", "");

  private final String separator;

  private final VersionLetters letters;

  private final String pattern;

  private final String digits;

  private final int number;

  private VersionSegment next;

  /**
   * The constructor.
   *
   * @param separator the {@link #getSeparator() separator}.
   * @param letters the {@link #getLettersString() letters}.
   * @param digits the {@link #getDigits() digits}.
   */
  VersionSegment(String separator, String letters, String digits) {

    this(separator, letters, digits, "");
  }

  /**
   * The constructor.
   *
   * @param separator the {@link #getSeparator() separator}.
   * @param letters the {@link #getLettersString() letters}.
   * @param digits the {@link #getDigits() digits}.
   * @param pattern the {@link #getPattern() pattern}.
   */
  VersionSegment(String separator, String letters, String digits, String pattern) {

    super();
    this.separator = separator;
    boolean isAnyPattern = PATTERN_MATCH_ANY_VERSION.equals(pattern);
    if (isAnyPattern && letters.isEmpty()) {
      this.letters = VersionLetters.UNSTABLE;
    } else {
      this.letters = VersionLetters.of(letters);
    }
    if (!pattern.isEmpty() && !isAnyPattern
        && !PATTERN_MATCH_ANY_STABLE_VERSION.equals(pattern)) {
      throw new IllegalArgumentException("Invalid pattern: " + pattern);
    }
    this.pattern = pattern;
    this.digits = digits;
    if (this.digits.isEmpty()) {
      this.number = -1;
    } else {
      this.number = Integer.parseInt(this.digits);
    }
    if (EMPTY != null) {
      assert (!this.letters.isEmpty() || !this.digits.isEmpty() || !this.separator.isEmpty()
          || !this.pattern.isEmpty());
    }
  }

  private VersionSegment(VersionSegment next, String separator, VersionLetters letters, String digits, int number, String pattern) {
    super();
    this.next = next;
    this.separator = separator;
    this.letters = letters;
    this.pattern = pattern;
    this.digits = digits;
    this.number = number;
  }

  /**
   * @return the separator {@link String} (e.g. "." or "-") or the empty {@link String} ("") for none.
   */
  public String getSeparator() {

    return this.separator;
  }

  /**
   * @return the letters or the empty {@link String} ("") for none. In canonical {@link VersionIdentifier}s letters indicate the development phase (e.g. "pre",
   *     "rc", "alpha", "beta", "milestone", "test", "dev", "SNAPSHOT", etc.). However, letters are technically any
   *     {@link Character#isLetter(char) letter characters} and may also be something like a code-name (e.g. "Cupcake", "Donut", "Eclair", "Froyo",
   *     "Gingerbread", "Honeycomb", "Ice Cream Sandwich", "Jelly Bean" in case of Android internals). Please note that in such case it is impossible to
   *     properly decide which version is greater than another versions. To avoid mistakes, the comparison supports a strict mode that will let the comparison
   *     fail in such case. However, by default (e.g. for {@link Comparable#compareTo(Object)}) the default {@link String#compareTo(String) string comparison}
   *     (lexicographical) is used to ensure a natural order.
   * @see #getPhase()
   */
  public String getLettersString() {

    return this.letters.getLetters();
  }

  /**
   * @return the {@link VersionLetters}.
   */
  public VersionLetters getLetters() {

    return this.letters;
  }

  /**
   * @return the {@link VersionPhase} for the {@link #getLettersString() letters}. Will be {@link VersionPhase#UNDEFINED} if unknown and hence never
   *     {@code null}.
   * @see #getLettersString()
   */
  public VersionPhase getPhase() {

    return this.letters.getPhase();
  }

  /**
   * @return the digits or the empty {@link String} ("") for none. This is the actual {@link #getNumber() number} part of this {@link VersionSegment}. So the
   *     {@link VersionIdentifier} "1.0.001" will have three segments: The first one with "1" as digits, the second with "0" as digits, and a third with "001"
   *     as digits. You can get the same value via {@link #getNumber()} but this {@link String} representation will preserve leading zeros.
   */
  public String getDigits() {

    return this.digits;
  }

  /**
   * @return the {@link #getDigits() digits} and integer number. Will be {@code -1} if no {@link #getDigits() digits} are present.
   */
  public int getNumber() {

    return this.number;
  }

  /**
   * @return the potential pattern that is {@link #PATTERN_MATCH_ANY_STABLE_VERSION}, {@link #PATTERN_MATCH_ANY_VERSION}, or for no pattern the empty
   *     {@link String}.
   */
  public String getPattern() {

    return this.pattern;
  }

  /**
   * @return {@code true} if {@link #getPattern() pattern} is NOT {@link String#isEmpty() empty}.
   */
  public boolean isPattern() {

    return !this.pattern.isEmpty();
  }

  /**
   * @return the next {@link VersionSegment} or {@code null} if this is the tail of the {@link VersionIdentifier}.
   */
  public VersionSegment getNextOrNull() {

    return this.next;
  }

  /**
   * @return the next {@link VersionSegment} or the {@link #ofEmpty() empty segment} if this is the tail of the {@link VersionIdentifier}.
   */
  public VersionSegment getNextOrEmpty() {

    if (this.next == null) {
      return EMPTY;
    }
    return this.next;
  }

  /**
   * @return {@code true} if this is the empty {@link VersionSegment}, {@code false} otherwise.
   */
  public boolean isEmpty() {

    return (this == EMPTY);
  }

  /**
   * A valid {@link VersionSegment} has to meet the following requirements:
   * <ul>
   * <li>The {@link #getSeparator() separator} may not be {@link String#length() longer} than a single character (e.g.
   * ".-_1" or "--1" are not considered valid).</li>
   * <li>The {@link #getSeparator() separator} may only contain the characters '.', '-', or '_' (e.g. " 1" or "ö1" are
   * not considered valid).</li>
   * <li>The combination of {@link #getPhase() phase} and {@link #getNumber() number} has to be
   * {@link VersionPhase#isValid(int) valid} (e.g. "pineapple-pen1" or "donut" are not considered valid).</li>
   * </ul>
   */
  @Override
  public boolean isValid() {

    if (!this.pattern.isEmpty()) {
      return false;
    }
    int separatorLen = this.separator.length();
    if (separatorLen > 1) {
      return false;
    } else if (separatorLen == 1) {
      if (!CharCategory.isValidSeparator(this.separator.charAt(0))) {
        return false;
      }
    }
    return this.letters.getPhase().isValid(this.number);
  }

  @Override
  public VersionComparisonResult compareVersion(VersionSegment other) {

    if (other == null) {
      return VersionComparisonResult.GREATER_UNSAFE;
    }
    VersionComparisonResult lettersResult = this.letters.compareVersion(other.letters);
    if (!lettersResult.isEqual()) {
      return lettersResult;
    }
    if (!"_".equals(this.separator) && "_".equals(other.separator)) {
      if ("".equals(this.separator)) {
        return VersionComparisonResult.LESS;
      } else {
        return VersionComparisonResult.GREATER;
      }
    } else if ("_".equals(this.separator) && !"_".equals(other.separator)) {
      if ("".equals(other.separator)) {
        return VersionComparisonResult.GREATER;
      } else {
        return VersionComparisonResult.LESS;
      }
    }

    if (this.number != other.number) {
      if ((this.number < 0) && isPattern()) {
        return VersionComparisonResult.LESS_UNSAFE;
      } else if ((other.number < 0) && other.isPattern()) {
        return VersionComparisonResult.GREATER_UNSAFE;
      } else if (this.number < other.number) {
        return VersionComparisonResult.LESS;
      } else {
        return VersionComparisonResult.GREATER;
      }
    } else if (this.separator.equals(other.separator)) {
      return VersionComparisonResult.EQUAL;
    } else {
      return VersionComparisonResult.EQUAL_UNSAFE;
    }
  }

  /**
   * Matches a {@link VersionSegment} with a potential {@link #getPattern() pattern} against another {@link VersionSegment}. This operation may not always be
   * symmetric.
   *
   * @param other the {@link VersionSegment} to match against.
   * @return the {@link VersionMatchResult} of the match.
   */
  public VersionMatchResult matches(VersionSegment other) {

    if (other == null) {
      return VersionMatchResult.MISMATCH;
    }
    if (isEmpty() && other.isEmpty()) {
      return VersionMatchResult.MATCH;
    }
    boolean isPattern = isPattern();
    if (isPattern) {
      if (!this.digits.isEmpty()) {
        if (this.number != other.number) {
          return VersionMatchResult.MISMATCH;
        }
      }
      if (!this.separator.isEmpty()) {
        if (!this.separator.equals(other.separator)) {
          return VersionMatchResult.MISMATCH;
        }
      }
    } else {
      if ((this.number != other.number) || !this.separator.equals(other.separator)) {
        return VersionMatchResult.MISMATCH;
      }
    }
    VersionMatchResult result = this.letters.matches(other.letters, isPattern);
    if (isPattern && (result == VersionMatchResult.EQUAL)) {
      if (this.pattern.equals(PATTERN_MATCH_ANY_STABLE_VERSION)) {
        VersionLetters developmentPhase = other.getDevelopmentPhase();
        if (developmentPhase.isUnstable()) {
          return VersionMatchResult.MISMATCH;
        }
        return VersionMatchResult.MATCH;
      } else if (this.pattern.equals(PATTERN_MATCH_ANY_VERSION)) {
        return VersionMatchResult.MATCH;
      } else {
        throw new IllegalStateException("Pattern=" + this.pattern);
      }
    }
    return result;
  }

  /**
   * @return the {@link VersionLetters} that represent a {@link VersionLetters#isDevelopmentPhase() development phase} searching from this
   *     {@link VersionSegment} to all {@link #getNextOrNull() next segments}. Will be {@link VersionPhase#NONE} if no
   *     {@link VersionPhase#isDevelopmentPhase() development phase} was found and {@link VersionPhase#UNDEFINED} if multiple
   *     {@link VersionPhase#isDevelopmentPhase() development phase}s have been found.
   * @see VersionIdentifier#getDevelopmentPhase()
   */
  protected VersionLetters getDevelopmentPhase() {

    VersionLetters result = VersionLetters.EMPTY;
    VersionSegment segment = this;
    while (segment != null) {
      if (segment.letters.isDevelopmentPhase()) {
        if (result == VersionLetters.EMPTY) {
          result = segment.letters;
        } else {
          result = VersionLetters.UNDEFINED;
        }
      }
      segment = segment.next;
    }
    return result;
  }

  /**
   * {@link VersionIdentifier#incrementSegment(int, boolean)}  Increments a version} recursively per {@link VersionSegment}.
   *
   * @param digitKeepCount the number of leading {@link VersionSegment}s with {@link VersionSegment#getDigits() digits} to keep untouched. Will be {@code 0}
   *     for the segment to increment and negative for the segments to set to zero.
   * @param keepLetters {@code true} to keep {@link VersionSegment#getLetters() letters} from modified segments, {@code false} to drop them.
   * @return the new {@link VersionSegment}.
   */
  VersionSegment increment(int digitKeepCount, boolean keepLetters) {

    String separator = this.separator;
    VersionLetters letters = this.letters;
    String digits = this.digits;
    int number = this.number;
    String pattern = this.pattern;
    int nextSegmentKeepCount = digitKeepCount;
    if (this.number >= 0) {
      nextSegmentKeepCount--;
    }
    if ((digitKeepCount < 0) || ((digitKeepCount == 0) && (this.number >= 0))) {
      if (!keepLetters) {
        letters = VersionLetters.EMPTY;
      }
      if (number >= 0) {
        if (digitKeepCount == 0) {
          number++;
        } else {
          number = 0;
        }
        int digitsLength = digits.length();
        digits = Integer.toString(number);
        int leadingZeros = digitsLength - digits.length();
        if (leadingZeros > 0) {
          StringBuilder newDigits = new StringBuilder(digits);
          while (leadingZeros > 0) {
            newDigits.insert(0, "0");
            leadingZeros--;
          }
          digits = newDigits.toString();
        }
      } else if (!keepLetters) {
        if (this.next == null) {
          return null;
        }
        return this.next.increment(nextSegmentKeepCount, false);
      }
    }
    VersionSegment nextSegment = null;
    if (this.next != null) {
      nextSegment = this.next.increment(nextSegmentKeepCount, keepLetters);
    }
    return new VersionSegment(nextSegment, separator, letters, digits, number, pattern);
  }

  /**
   * @return the number of {@link VersionSegment}s with {@link VersionSegment#getDigits() digits}.
   */
  int countDigits() {

    int count = 0;
    if (this.number >= 0) {
      count = 1;
    }
    if (this.next != null) {
      count = count + this.next.countDigits();
    }
    return count;
  }

  @Override
  public boolean equals(Object obj) {

    if (obj == this) {
      return true;
    } else if (!(obj instanceof VersionSegment)) {
      return false;
    }
    VersionSegment other = (VersionSegment) obj;
    if (!Objects.equals(this.digits, other.digits)) {
      return false;
    } else if (!Objects.equals(this.separator, other.separator)) {
      return false;
    } else if (!Objects.equals(this.letters, other.letters)) {
      return false;
    } else if (!Objects.equals(this.pattern, other.pattern)) {
      return false;
    } else if (!Objects.equals(this.next, other.next)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {

    return this.separator + this.letters + this.digits + this.pattern;
  }

  /**
   * @return the {@link #isEmpty() empty} {@link VersionSegment} instance.
   */
  public static VersionSegment ofEmpty() {

    return EMPTY;
  }

  static VersionSegment of(String version) {

    CharReader reader = new CharReader(version);
    VersionSegment start = null;
    VersionSegment current = null;
    while (reader.hasNext()) {
      VersionSegment segment = parseSegment(reader);
      if (current == null) {
        start = segment;
      } else {
        current.next = segment;
      }
      current = segment;
    }
    return start;
  }

  private static VersionSegment parseSegment(CharReader reader) {

    String separator = reader.readSeparator();
    String letters = reader.readLetters();
    String digits = reader.readDigits();
    String pattern = reader.readPattern();
    return new VersionSegment(separator, letters, digits, pattern);
  }

}
