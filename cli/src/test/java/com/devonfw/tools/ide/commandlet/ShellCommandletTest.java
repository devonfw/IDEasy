package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;

/**
 * Test of {@link ShellCommandlet}.
 */
class ShellCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link ShellCommandlet#normalizeLine(String)} when the pre-filled {@code ide } prefix is left untouched, so the command behaves the same as
   * before this prefix was made part of the editable input line.
   */
  @Test
  void testNormalizeLineKeepsPrefixedCommand() {

    // act & assert
    assertThat(ShellCommandlet.normalizeLine("ide status")).isEqualTo("status");
    assertThat(ShellCommandlet.normalizeLine("ide install java")).isEqualTo("install java");
  }

  /**
   * Test of {@link ShellCommandlet#normalizeLine(String)} when the user removed the pre-filled {@code ide } prefix via backspace to enter a non-IDEasy
   * command, e.g. {@code cd}, see <a href="https://github.com/devonfw/IDEasy/issues/821">#821</a> for reference.
   */
  @Test
  void testNormalizeLineWithRemovedPrefix() {

    // act & assert
    assertThat(ShellCommandlet.normalizeLine("cd ..")).isEqualTo("cd ..");
    assertThat(ShellCommandlet.normalizeLine("exit")).isEqualTo("exit");
  }

  /**
   * Test of {@link ShellCommandlet#normalizeLine(String)} when the user leaves an empty or unmodified prompt.
   */
  @Test
  void testNormalizeLineWithEmptyInput() {

    // act & assert
    assertThat(ShellCommandlet.normalizeLine("ide")).isEmpty();
    assertThat(ShellCommandlet.normalizeLine("ide ")).isEmpty();
    assertThat(ShellCommandlet.normalizeLine("")).isEmpty();
  }
}
