package com.devonfw.tools.ide.version;

/**
 * Enum with the available categories of a {@link Character} for a {@link VersionSegment}.
 */
enum CharCategory {

  /** Category for {@link VersionSegment#getSeparator() separator} characters. */
  SEPARATOR,

  /** Category for {@link VersionSegment#getLetters() letter} characters. */
  LETTER,

  /** Category for {@link VersionSegment#getDigits() digit} characters. */
  DIGIT;

  public static CharCategory of(char c) {

    if (isLetter(c)) {
      return LETTER;
    } else if (isDigit(c)) {
      return DIGIT;
    } else {
      return SEPARATOR;
    }
  }

  static boolean isDigit(char c) {

    return (c >= '0') && (c <= '9');
  }

  static boolean isLetter(char c) {

    return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'));
  }

  static boolean isValidSeparator(char c) {

    return (c == '.') || (c == '-') || (c == '_');
  }

  static boolean isLetterSeparator(char c) {

    return (c == '-') || (c == '_');
  }

}
