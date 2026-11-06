package com.devonfw.tools.ide.common;

import com.devonfw.tools.ide.version.VersionSegment;

/**
 * Stateful object to read a {@link String} as stream of characters.
 */
public final class CharReader {

  private final String string;

  private final int start;

  private final int len;

  private int i;

  /**
   * The constructor.
   *
   * @param string the {@link String} to read.
   */
  public CharReader(String string) {

    this(string, 0, string.length());
  }

  /**
   * The constructor.
   *
   * @param string the {@link String} to read.
   * @param start the first character of the {@link String} to read (inclusive).
   */
  public CharReader(String string, int start) {

    this(string, start, string.length());
  }

  /**
   * The constructor.
   *
   * @param string the {@link String} to read.
   * @param start the first character of the {@link String} to read (inclusive).
   * @param limit the exclusive limit where to stop reading from {@code String}.
   */
  public CharReader(String string, int start, int limit) {

    super();
    this.string = string;
    this.start = start;
    this.len = limit;
    this.i = start;
  }

  /**
   * @return {@code true} if a next character is available, {@code false} otherwise (the end of the readers stream has been reached).
   */
  public boolean hasNext() {

    return (this.i < this.len);
  }

  /**
   * @return the current character or NUL character if no {@link #hasNext() next character is available}. This method will not advance the readers position.
   */
  public char peek() {

    if (this.i < this.len) {
      return this.string.charAt(this.i);
    }
    return '\0';
  }

  /**
   * @return the current character or NUL character if no {@link #hasNext() next character is available}. This method will advance the readers position unless
   *     the end of the stream has already been reached.
   */
  public char next() {

    if (this.i < this.len) {
      return this.string.charAt(this.i++);
    }
    return '\0';
  }

  /**
   * @return reads and consumes a version separator from the stream.
   */
  public String readSeparator() {

    int start = this.i;
    while (this.i < this.len) {
      char c = this.string.charAt(this.i);
      if ((c != '*') && !isDigit(c) && !isLetter(c)) {
        this.i++;
      } else {
        break;
      }
    }
    return this.string.substring(start, this.i);
  }

  /**
   * @return reads and consumes as all next digits from the stream.
   */
  public String readDigits() {

    int start = this.i;
    while (this.i < this.len) {
      char c = this.string.charAt(this.i);
      if (isDigit(c)) {
        this.i++;
      } else {
        break;
      }
    }
    return this.string.substring(start, this.i);
  }

  /**
   * @return reads and consumes as all next letters from the stream.
   */
  public String readLetters() {

    int start = this.i;
    while (this.i < this.len) {
      int step = 0;
      char c = this.string.charAt(this.i);
      if (isLetter(c)) {
        step = 1;
      } else if ((this.i > start) && isLetterSeparator(c)) {
        int j = this.i + 1;
        if (j < this.len) {
          char next = this.string.charAt(j);
          if (isLetter(next)) {
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

  /**
   * @return reads and consumes a version pattern from the stream.
   */
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

  /**
   * @param expected the expected character.
   * @return {@code true} if the expected character was found and consumed from this reader, {@code false} otherwise (state of reader remains unchanged).
   */
  public boolean expect(char expected) {

    if (this.i < this.len) {
      char c = this.string.charAt(this.i);
      if (c == expected) {
        this.i++;
        return true;
      }
    }
    return false;
  }

  /**
   * @param expected the {@link String} expected at the given position.
   * @return {@code true} if the expected {@link String} was found and consumed from this reader, {@code false} otherwise (state of reader remains unchanged).
   */
  public boolean expect(String expected) {
    return expect(expected, false);
  }

  /**
   * @param expected the {@link String} expected at the given position.
   * @param ignoreCase flag telling to ignore the case of the expected {@link String}.
   * @return {@code true} if the expected {@link String} was found and consumed from this reader, {@code false} otherwise (state of reader remains unchanged).
   */
  public boolean expect(String expected, boolean ignoreCase) {

    int length = expected.length();
    int end = this.i + length;
    if (end > this.len) {
      return false;
    }
    String substring = this.string.substring(this.i, end);
    boolean found;
    if (ignoreCase) {
      found = substring.equalsIgnoreCase(expected);
    } else {
      found = substring.equals(expected);
    }
    if (found) {
      this.i = end;
    }
    return found;
  }

  /**
   * @param filter the {@link CharFilter} defining where to stop reading.
   * @return a {@link String} with all {@link #next() next characters} consumed from the stream until a character was {@link CharFilter#accept(char) accepted}
   *     by the given {@link CharFilter} or the end of the stream was reached.
   */
  public String readUntil(CharFilter filter) {

    int start = this.i;
    while (this.i < this.len) {
      char c = this.string.charAt(this.i);
      if (filter.accept(c)) {
        break;
      }
      this.i++;
    }
    return this.string.substring(start, this.i);
  }

  /**
   * @param filter the {@link CharFilter} defining where to stop reading.
   * @return a {@link String} with all {@link #next() next characters} consumed from the stream until a character was {@link CharFilter#accept(char) accepted}
   *     by the given {@link CharFilter} or the end of the stream was reached.
   */
  public String readWhile(CharFilter filter) {

    return readUntil(c -> !filter.accept(c));
  }

  /**
   * @return the parsed quoted {@link String} with quotes removed or {@code null} if no quote was found at the current position.
   */
  public String readQuotedString() {

    char quote = peek();
    if ((quote != '"') && (quote != '\'')) {
      return null;
    }
    next();
    StringBuilder sb = new StringBuilder();
    // boolean escape = false;
    while (this.i < this.len) {
      int step = 0;
      char c = this.string.charAt(this.i++);
      // if (c == '\\') {
      //   if (!escape) {
      //     escape = true;
      //     continue;
      //   }
      // } else
      // if ((c == quote) && !escape) {
      if (c == quote) {
        break;
      }
      sb.append(c);
    }
    // if we reached the end of the reader, we return even if closing quote was missing.
    return sb.toString();
  }

  /**
   * Skips all whitespaces.
   *
   * @return the number of space characters that have been found and skipped.
   */
  public int skipSpaces() {

    int startIndex = this.i;
    while (this.i < this.len) {
      char c = this.string.charAt(this.i);
      if (Character.isWhitespace(c)) {
        this.i++;
      } else {
        break;
      }
    }
    return this.i - startIndex;
  }

  /**
   * @return the start index as the minimum allowed value for {@link #getIndex() index}.
   */
  public int getStart() {

    return this.start;
  }

  /**
   * @return the current index position.
   */
  public int getIndex() {

    return this.i;
  }

  /**
   * @param index the new {@link #getIndex() index}. Has to be in the range from {@link #getStart() start} to {@link #getLimit() limit}.
   */
  public void setIndex(int index) {

    if ((index < this.start) || (index > this.len)) {
      throw new IndexOutOfBoundsException(index);
    }
    this.i = index;
  }

  /**
   * @return the limit of the {@link #getIndex() index} when the end of the stream has been reached.
   */
  public int getLimit() {

    return this.len;
  }

  /**
   * @return the number of remaining {@link #next() next characters} that can be consumed from this reader. Will be {@code 0} if the end of the stream has been
   *     reached.
   */
  public int getRemaining() {
    return this.len - this.i;
  }

  /**
   * @return the entire rest of the stream.
   */
  public String getRest() {

    if (this.i < this.len) {
      return this.string.substring(this.i, this.len);
    }
    return "";
  }

  /**
   * @return the full {@link String} this {@link CharReader} was created from.
   */
  public String getString() {

    if ((this.start == 0) && (this.len == this.string.length())) {
      return this.string;
    }
    return this.string.substring(this.start, this.len);
  }

  /**
   * @param newStart the new {@link #getStart() start}.
   * @param newLimit the new {@link #getLimit() limit}.
   * @return the new {@link CharReader}.
   */
  public CharReader subSequence(int newStart, int newLimit) {
    if ((newStart < this.start) || (newLimit > this.len) || (newStart > newLimit)) {
      throw new IllegalArgumentException("[" + newStart + "," + newLimit + ") !<= [" + this.start + "," + this.len + ")");
    }
    return new CharReader(this.string, newStart, newLimit);
  }

  @Override
  public String toString() {

    return this.string.substring(this.start, this.i) + "<|>" + getRest();
  }

  /**
   * @param c the character to test.
   * @return {@code true} in case of Arabic digit, {@code false} otherwise.
   */
  public static boolean isDigit(char c) {

    return (c >= '0') && (c <= '9');
  }

  /**
   * @param c the character to test.
   * @return {@code true} in case of Latin ASCII letter, {@code false} otherwise.
   */
  public static boolean isLetter(char c) {

    return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'));
  }

  /**
   * @param c the character to test.
   * @return {@code true} in case of a lower Latin ASCII letter, {@code false} otherwise.
   */
  public static boolean isLetterLowercase(char c) {

    return ((c >= 'a') && (c <= 'z'));
  }

  /**
   * @param c the character to test.
   * @return {@code true} if dot, hyphen, or underscore, {@code false} otherwise.
   */
  public static boolean isValidSeparator(char c) {

    return (c == '.') || (c == '-') || (c == '_');
  }

  /**
   * @param c the character to test.
   * @return {@code true} if hyphen or underscore, {@code false} otherwise.
   */
  public static boolean isLetterSeparator(char c) {

    return (c == '-') || (c == '_');
  }

}
