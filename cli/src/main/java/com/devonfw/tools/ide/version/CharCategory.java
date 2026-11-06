package com.devonfw.tools.ide.version;

import com.devonfw.tools.ide.common.CharReader;

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

    if (CharReader.isLetter(c)) {
      return LETTER;
    } else if (CharReader.isDigit(c)) {
      return DIGIT;
    } else {
      return SEPARATOR;
    }
  }

}
