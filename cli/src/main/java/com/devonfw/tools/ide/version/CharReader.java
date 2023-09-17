package com.devonfw.tools.ide.version;

/**
 * Stateful object to read a {@link String} as stream of characters.
 */
final class CharReader {

  private final String string;

  private final int len;

  private int i;

  /**
   * The constructor.
   *
   * @param string the {@link String} to read.
   */
  public CharReader(String string) {

    super();
    this.string = string;
    this.len = string.length();
    this.i = 0;
  }

  public boolean hasNext() {

    return (this.i < this.len);
  }

  public char peek() {

    if (this.i < this.len) {
      return this.string.charAt(this.i);
    }
    return '\0';
  }

  public char next() {

    if (this.i < this.len) {
      return this.string.charAt(this.i++);
    }
    return '\0';
  }

  public String readSeparator() {

    int start = this.i;
    while (this.i < this.len) {
      char c = this.string.charAt(this.i);
      if ((c != '*') && !CharCategory.isDigit(c) && !CharCategory.isLetter(c)) {
        this.i++;
      } else {
        break;
      }
    }
    return this.string.substring(start, this.i);
  }

  public String readDigits() {

    int start = this.i;
    while (this.i < this.len) {
      char c = this.string.charAt(this.i);
      if (CharCategory.isDigit(c)) {
        this.i++;
      } else {
        break;
      }
    }
    return this.string.substring(start, this.i);
  }

  public String readLetters() {

    int start = this.i;
    while (this.i < this.len) {
      int step = 0;
      char c = this.string.charAt(this.i);
      if (CharCategory.isLetter(c)) {
        step = 1;
      } else if ((this.i > start) && CharCategory.isLetterSeparator(c)) {
        int j = this.i + 1;
        if (j < this.len) {
          char next = this.string.charAt(j);
          if (CharCategory.isLetter(next)) {
            step = 2;
          }
        }
      }
      if (step == 0) {
        break;
      }
      this.i = this.i + step;
    }
    return this.string.substring(start, this.i);
  }

  public String readPattern() {

    char c = peek();
    if (c == '*') {
      this.i++;
      c = peek();
      if (c == '!') {
        this.i++;
        return VersionSegment.PATTERN_MATCH_ANY_VERSION;
      }
      return VersionSegment.PATTERN_MATCH_ANY_STABLE_VERSION;
    }
    return "";
  }

}
