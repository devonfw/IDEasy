package com.devonfw.tools.ide.version;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link CharReader}.
 */
class CharReaderTest extends Assertions {

  /** Test of {@link CharReader#hasNext()}, {@link CharReader#peek()} and {@link CharReader#next()}. */
  @Test
  void testPeekAndNext() {

    // given
    CharReader reader = new CharReader("ab");
    // then
    assertThat(reader.hasNext()).isTrue();
    assertThat(reader.peek()).isEqualTo('a');
    assertThat(reader.peek()).isEqualTo('a');
    assertThat(reader.next()).isEqualTo('a');
    assertThat(reader.hasNext()).isTrue();
    assertThat(reader.next()).isEqualTo('b');
    assertThat(reader.hasNext()).isFalse();
    assertThat(reader.peek()).isEqualTo('\0');
    assertThat(reader.next()).isEqualTo('\0');
  }

  /** Test of {@link CharReader#hasNext()} with an empty input. */
  @Test
  void testEmpty() {

    // given
    CharReader reader = new CharReader("");
    // then
    assertThat(reader.hasNext()).isFalse();
    assertThat(reader.peek()).isEqualTo('\0');
    assertThat(reader.next()).isEqualTo('\0');
  }

  /** Test of {@link CharReader#readSeparator()}. */
  @Test
  void testReadSeparator() {

    // given
    CharReader reader = new CharReader(".-_abc");
    // when
    String separator = reader.readSeparator();
    // then
    assertThat(separator).isEqualTo(".-_");
    assertThat(reader.hasNext()).isTrue();
    assertThat(reader.next()).isEqualTo('a');
  }

  /** Test of {@link CharReader#readSeparator()} stopping before a pattern or digit/letter. */
  @Test
  void testReadSeparatorStopsAtStar() {

    // given
    CharReader reader = new CharReader("*1");
    // when
    String separator = reader.readSeparator();
    // then
    assertThat(separator).isEmpty();
    assertThat(reader.next()).isEqualTo('*');
  }

  /** Test of {@link CharReader#readDigits()}. */
  @Test
  void testReadDigits() {

    // given
    CharReader reader = new CharReader("123abc");
    // when
    String digits = reader.readDigits();
    // then
    assertThat(digits).isEqualTo("123");
    assertThat(reader.readLetters()).isEqualTo("abc");
  }

  /** Test of {@link CharReader#readDigits()} when no digit is present. */
  @Test
  void testReadDigitsNone() {

    // given
    CharReader reader = new CharReader("abc");
    // when
    String digits = reader.readDigits();
    // then
    assertThat(digits).isEmpty();
    assertThat(reader.peek()).isEqualTo('a');
  }

  /** Test of {@link CharReader#readLetters()} keeping internal letter separators. */
  @Test
  void testReadLetters() {

    // given
    CharReader reader = new CharReader("a-b_c1");
    // when
    String letters = reader.readLetters();
    // then
    assertThat(letters).isEqualTo("a-b_c");
    assertThat(reader.next()).isEqualTo('1');
  }

  /** Test of {@link CharReader#readLetters()} with a trailing letter separator. */
  @Test
  void testReadLettersTrailingSeparator() {

    // given
    CharReader reader = new CharReader("ab-");
    // when
    String letters = reader.readLetters();
    // then
    assertThat(letters).isEqualTo("ab");
    assertThat(reader.readSeparator()).isEqualTo("-");
  }

  /** Test of {@link CharReader#readLetters()} with a leading separator. */
  @Test
  void testReadLettersLeadingSeparator() {

    // given
    CharReader reader = new CharReader("-ab");
    // when
    String letters = reader.readLetters();
    // then
    assertThat(letters).isEmpty();
    assertThat(reader.readSeparator()).isEqualTo("-");
    assertThat(reader.readLetters()).isEqualTo("ab");
  }

  /** Test of {@link CharReader#readPattern()} for the "any version" pattern. */
  @Test
  void testReadPatternAnyVersion() {

    // given
    CharReader reader = new CharReader("*!rest");
    // when
    String pattern = reader.readPattern();
    // then
    assertThat(pattern).isEqualTo(VersionSegment.PATTERN_MATCH_ANY_VERSION);
    assertThat(reader.hasNext()).isTrue();
    assertThat(reader.next()).isEqualTo('r');
  }

  /** Test of {@link CharReader#readPattern()} for the "any stable version" pattern. */
  @Test
  void testReadPatternAnyStableVersion() {

    // given
    CharReader reader = new CharReader("*");
    // when
    String pattern = reader.readPattern();
    // then
    assertThat(pattern).isEqualTo(VersionSegment.PATTERN_MATCH_ANY_STABLE_VERSION);
    assertThat(reader.hasNext()).isFalse();
  }

  /** Test of {@link CharReader#readPattern()} when no pattern is present. */
  @Test
  void testReadPatternNone() {

    // given
    CharReader reader = new CharReader("1.2.3");
    // when
    String pattern = reader.readPattern();
    // then
    assertThat(pattern).isEmpty();
    assertThat(reader.peek()).isEqualTo('1');
  }

  /** Test of {@link CharReader#readPattern()} with a star that is not followed by "!". */
  @Test
  void testReadPatternStarThenOther() {

    // given
    CharReader reader = new CharReader("*x");
    // when
    String pattern = reader.readPattern();
    // then
    assertThat(pattern).isEqualTo(VersionSegment.PATTERN_MATCH_ANY_STABLE_VERSION);
    assertThat(reader.next()).isEqualTo('x');
  }
}
