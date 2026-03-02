package com.devonfw.tools.ide.cli;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link CliArguments}.
 */
class CliArgumentsTest extends Assertions {

  private CliArguments of(String... args) {

    CliArguments arguments = new CliArguments(args);
    assertThat(arguments.current().isStart()).isTrue();
    return arguments;
  }

  /**
   * Test of {@link CliArgument}s toString method.
   */
  @Test
  void testToString() {

    // arrange
    String[] args = { "one", "-two", "--three" };
    // act
    CliArguments arguments = of(args);
    String argString = arguments.toString();
    // assert
    assertThat(argString).contains("one");
    assertThat(argString).contains("-two");
    assertThat(argString).contains("--three");
  }

  /**
   * Test of {@link CliArgument} with simple usage.
   */
  @Test
  void testSimple() {

    // arrange
    String[] args = { "one", "two", "three" };
    // act
    CliArguments arguments = of(args);
    // assert
    assertThat(arguments.hasNext()).isTrue();
    CliArgument arg = arguments.next();
    assertThat(arg.get()).isEqualTo("one");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.get()).isEqualTo("two");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.get()).isEqualTo("three");
    assertThat(arguments.hasNext()).isFalse();
    arg = arguments.next();
    assertThat(arg.isEnd()).isTrue();
    assertThat(arguments.hasNext()).isFalse();
  }

  /**
   * Test of {@link CliArgument} with combined options.
   */
  @Test
  void testCombinedOptions() {

    // arrange
    String[] args = { "-abc", "-xyz", "--abc", "abc" };
    // act
    CliArguments arguments = of(args);
    // assert
    assertThat(arguments.hasNext()).isTrue();
    CliArgument arg = arguments.next();
    assertThat(arg.get()).isEqualTo("-a");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.get()).isEqualTo("-b");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.get()).isEqualTo("-c");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.get()).isEqualTo("-x");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.get()).isEqualTo("-y");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.get()).isEqualTo("-z");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.get()).isEqualTo("--abc");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.get()).isEqualTo("abc");
    assertThat(arguments.hasNext()).isFalse();
    arg = arguments.next();
    assertThat(arg.isEnd()).isTrue();
    assertThat(arguments.hasNext()).isFalse();
  }

  /**
   * Test of {@link CliArgument} with combined options.
   */
  @Test
  void testCombinedOptionsNoSplit() {

    // arrange
    String[] args = { "-abc", "-xyz", "--abc", "abc" };
    // act
    CliArguments arguments = of(args);
    arguments.endOptions();
    // assert
    assertThat(arguments.hasNext()).isTrue();
    CliArgument arg = arguments.next();
    assertThat(arg.get()).isEqualTo("-abc");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.get()).isEqualTo("-xyz");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.get()).isEqualTo("--abc");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.get()).isEqualTo("abc");
    assertThat(arguments.hasNext()).isFalse();
    arg = arguments.next(); // actually violating contract of Iterator
    assertThat(arg.isEnd()).isTrue();
    assertThat(arguments.hasNext()).isFalse();
  }

  /**
   * Test of {@link CliArgument} with key-value arguments.
   */
  @Test
  void testKeyValue() {

    // arrange
    String[] args = { "--locale=de", "time=23:59:59", "key=", "=value", "key==", "==value" };
    // act
    CliArguments arguments = of(args);
    // assert
    assertThat(arguments.hasNext()).isTrue();
    CliArgument arg = arguments.next();
    assertThat(arg.getKey()).isEqualTo("--locale");
    assertThat(arg.getValue()).isEqualTo("de");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.getKey()).isEqualTo("time");
    assertThat(arg.getValue()).isEqualTo("23:59:59");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    // edge-cases
    assertThat(arg.getKey()).isEqualTo("key");
    assertThat(arg.getValue()).isEqualTo("");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.getKey()).isEqualTo("");
    assertThat(arg.getValue()).isEqualTo("value");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.getKey()).isEqualTo("key");
    assertThat(arg.getValue()).isEqualTo("=");
    assertThat(arguments.hasNext()).isTrue();
    arg = arguments.next();
    assertThat(arg.getKey()).isEqualTo("");
    assertThat(arg.getValue()).isEqualTo("=value");
    assertThat(arguments.hasNext()).isFalse();
    arg = arguments.next(); // actually violating contract of Iterator
    assertThat(arg.isEnd()).isTrue();
    assertThat(arguments.hasNext()).isFalse();
  }
}
