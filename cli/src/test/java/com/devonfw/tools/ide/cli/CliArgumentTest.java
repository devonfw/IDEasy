package com.devonfw.tools.ide.cli;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link CliArgument}.
 */
public class CliArgumentTest extends Assertions {

  /**
   * Test of {@link CliArgument} with simple usage.
   */
  @Test
  public void testSimple() {

    String[] args = { "one", "two", "three" };
    CliArgument arg = CliArgument.of(args);
    assertThat(arg.get()).isEqualTo("one");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(true);
    assertThat(arg.get()).isEqualTo("two");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(true);
    assertThat(arg.get()).isEqualTo("three");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(true);
    assertThat(arg.isEnd()).isTrue();
  }

  /**
   * Test of {@link CliArgument} with combined options.
   */
  @Test
  public void testCombinedOptions() {

    // arrange
    boolean split = true;
    String[] args = { "-abc", "-xyz", "--abc", "abc" };
    // act
    CliArgument arg = CliArgument.of(split, args);
    // assert
    assertThat(arg.get()).isEqualTo("-a");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.get()).isEqualTo("-b");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.get()).isEqualTo("-c");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.get()).isEqualTo("-x");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.get()).isEqualTo("-y");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.get()).isEqualTo("-z");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.get()).isEqualTo("--abc");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.get()).isEqualTo("abc");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.isEnd()).isTrue();
  }

  /**
   * Test of {@link CliArgument} with combined options.
   */
  @Test
  public void testCombinedOptionsNoSplit() {

    // arrange
    boolean split = false;
    String[] args = { "-abc", "-xyz", "--abc", "abc" };
    // act
    CliArgument arg = CliArgument.of(split, args);
    // assert
    assertThat(arg.get()).isEqualTo("-abc");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.get()).isEqualTo("-xyz");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.get()).isEqualTo("--abc");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.get()).isEqualTo("abc");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.isEnd()).isTrue();
  }

  /**
   * Test of {@link CliArgument} with key-value arguments.
   */
  @Test
  public void testKeyValue() {

    // arrange
    boolean split = true;
    String[] args = { "--locale=de", "time=23:59:59", "key=", "=value", "key==", "==value" };
    // act
    CliArgument arg = CliArgument.of(split, args);
    // assert
    assertThat(arg.getKey()).isEqualTo("--locale");
    assertThat(arg.getValue()).isEqualTo("de");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.getKey()).isEqualTo("time");
    assertThat(arg.getValue()).isEqualTo("23:59:59");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    // edge-cases
    assertThat(arg.getKey()).isEqualTo("key");
    assertThat(arg.getValue()).isEqualTo("");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.getKey()).isEqualTo("");
    assertThat(arg.getValue()).isEqualTo("value");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.getKey()).isEqualTo("key");
    assertThat(arg.getValue()).isEqualTo("=");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.getKey()).isEqualTo("");
    assertThat(arg.getValue()).isEqualTo("=value");
    assertThat(arg.isEnd()).isFalse();
    arg = arg.getNext(split);
    assertThat(arg.isEnd()).isTrue();
  }
}
