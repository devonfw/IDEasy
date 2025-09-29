package com.devonfw.tools.ide.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test of {@link StringUtil}.
 */
public class StringUtilTest {

  /**
   * Tests appending of a single extra string will return the expected result.
   */
  @Test
  void testAppendSingleExtra() {
    // given
    String[] args = { "install" };

    // when
    String[] actual = StringUtil.extendArray(args, false, "--silent");

    // then
    assertThat(actual).containsExactly("install", "--silent");
    assertThat(actual).isNotSameAs(args); // returns a new array
    assertThat(args).containsExactly("install"); // original unchanged
  }

  /**
   * Tests prepending of a single extra string will return the expected result.
   */
  @Test
  void testPrependSingleExtra() {
    // given
    String[] args = { "install" };

    // when
    String[] actual = StringUtil.extendArray(args, true, "--silent");

    // then
    assertThat(actual).containsExactly("--silent", "install");
    assertThat(args).containsExactly("install"); // original unchanged
  }

  /**
   * Tests appending multiple extras preserves order and contents.
   */
  @Test
  void testAppendMultipleExtrasPreservesOrder() {
    // given
    String[] args = { "run", "build" };

    // when
    String[] actual = StringUtil.extendArray(args, false, "--silent", "--no-fund");

    // then
    assertThat(actual).containsExactly("run", "build", "--silent", "--no-fund");
  }

  /**
   * Tests prepending multiple extras preserves order and contents.
   */
  @Test
  void testPrependMultipleExtrasPreservesOrder() {
    // given
    String[] args = { "run", "test" };

    // when
    String[] actual = StringUtil.extendArray(args, true, "ci", "--offline");

    // then
    assertThat(actual).containsExactly("ci", "--offline", "run", "test");
  }

  /**
   * Tests appending when args is null results in only extra args returned.
   */
  @Test
  void testArgsNullAppend() {
    // given
    String[] args = null;

    // when
    String[] actual = StringUtil.extendArray(args, false, "--silent");

    // then
    assertThat(actual).containsExactly("--silent");
  }

  /**
   * Tests prepending when args is null results in only extra args returned.
   */
  @Test
  void testArgsNullPrepend() {
    // given
    String[] args = null;

    // when
    String[] actual = StringUtil.extendArray(args, true, "--silent", "--no-audit");

    // then
    assertThat(actual).containsExactly("--silent", "--no-audit");
  }

  /**
   * Tests appending with extraArgs = null returns a copy of args (not the same reference).
   */
  @Test
  void testExtraNullAppendReturnsCopyOfArgs() {
    // given
    String[] args = { "install", "--legacy-peer-deps" };

    // when
    String[] actual = StringUtil.extendArray(args, false, (String[]) null);

    // then
    assertThat(actual).containsExactly("install", "--legacy-peer-deps");
  }

  /**
   * Tests prepending with extraArgs = null returns a copy of args (not the same reference).
   */
  @Test
  void testExtraNullPrependReturnsCopyOfArgs() {
    // given
    String[] args = { "run", "test" };

    // when
    String[] actual = StringUtil.extendArray(args, true, (String[]) null);

    // then
    assertThat(actual).containsExactly("run", "test");
    assertThat(actual).isNotSameAs(args);
  }

  /**
   * Tests appending with an empty extraArgs returns a copy of args.
   */
  @Test
  void testExtraEmptyAppendReturnsCopyOfArgs() {
    // given
    String[] args = { "run", "lint" };

    // when
    String[] actual = StringUtil.extendArray(args, false, new String[0]);

    // then
    assertThat(actual).containsExactly("run", "lint");
  }

  /**
   * Tests prepending when args is empty returns only extra args.
   */
  @Test
  void testArgsEmptyPrependOnlyExtra() {
    // given
    String[] args = new String[0];

    // when
    String[] actual = StringUtil.extendArray(args, true, "ci");

    // then
    assertThat(actual).containsExactly("ci");
    assertThat(args).isEmpty();
  }

  /**
   * Tests appending when args is empty returns only extra args.
   */
  @Test
  void testArgsEmptyAppendOnlyExtra() {
    // given
    String[] args = new String[0];

    // when
    String[] actual = StringUtil.extendArray(args, false, "ci");

    // then
    assertThat(actual).containsExactly("ci");
    assertThat(args).isEmpty();
  }

  /**
   * Tests both args and extraArgs null produces an empty array (not null).
   */
  @Test
  void testBothNullReturnsEmptyArray() {
    // given
    String[] args = null;

    // when
    String[] actual = StringUtil.extendArray(args, false, (String[]) null);

    // then
    assertThat(actual).isNull();
  }

  /**
   * Tests both args and extraArgs empty produces an empty array.
   */
  @Test
  void testBothEmptyReturnsEmptyArray() {
    // given
    String[] args = new String[0];

    // when
    String[] actual = StringUtil.extendArray(args, false, new String[0]);

    // then
    assertThat(actual).isEmpty();
  }

  /**
   * Tests that inputs are not mutated by the method.
   */
  @Test
  void testDoesNotMutateInputs() {
    // given
    String[] args = { "a", "b" };
    String[] extra = { "x", "y" };
    String[] copyArgs = args.clone();
    String[] copyExtra = extra.clone();

    // when
    String[] actual = StringUtil.extendArray(args, true, extra);

    // then
    assertThat(args).containsExactly(copyArgs);
    assertThat(extra).containsExactly(copyExtra);
    assertThat(actual).containsExactly("x", "y", "a", "b");
  }

  /**
   * Tests that the result length equals the sum of inputs' lengths.
   */
  @Test
  void testResultLengthIsSum() {
    // given
    String[] args = { "a", "b", "c" };
    String[] extra = { "x" };

    // when
    String[] appended = StringUtil.extendArray(args, false, extra);
    String[] prepended = StringUtil.extendArray(args, true, extra);

    // then
    assertThat(appended.length).isEqualTo(args.length + extra.length);
    assertThat(prepended.length).isEqualTo(args.length + extra.length);
  }

  /**
   * Tests duplicates and empty strings are preserved in order.
   */
  @Test
  void testSupportsDuplicatesAndEmptyStrings() {
    // given
    String[] args = { "", "a", "a" };
    String[] extra = { "a", "" };

    // when
    String[] appended = StringUtil.extendArray(args, false, extra);
    String[] prepended = StringUtil.extendArray(args, true, extra);

    // then
    assertThat(appended).containsExactly("", "a", "a", "a", "");
    assertThat(prepended).containsExactly("a", "", "", "a", "a");
  }

  /**
   * Tests calling with no extra varargs (omitted) returns a copy when appending.
   */
  @Test
  void testNoExtraAppendReturnsCopyOfArgs() {
    // given
    String[] args = { "x", "y" };

    // when
    String[] actual = StringUtil.extendArray(args, false);

    // then
    assertThat(actual).containsExactly("x", "y");
  }

  /**
   * Tests calling with no extra varargs (omitted) returns a copy when prepending.
   */
  @Test
  void testNoExtraPrependReturnsCopyOfArgs() {
    // given
    String[] args = { "x", "y" };

    // when
    String[] actual = StringUtil.extendArray(args, true);

    // then
    assertThat(actual).containsExactly("x", "y");
  }

  /**
   * Tests args null with no extra varargs returns an empty array.
   */
  @Test
  void testArgsNullAndNoExtraReturnsEmptyArray() {
    // given
    String[] args = null;

    // when
    String[] actual = StringUtil.extendArray(args, false);

    // then
    assertThat(actual).isEmpty();
  }
}
