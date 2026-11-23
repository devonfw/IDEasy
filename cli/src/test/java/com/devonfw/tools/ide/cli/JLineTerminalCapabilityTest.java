package com.devonfw.tools.ide.cli;

import org.assertj.core.api.Assertions;
import org.jline.utils.Curses;
import org.junit.jupiter.api.Test;

/**
 * Test that the used JLine version decodes terminfo capabilities correctly, which is required for {@code ide shell} (see
 * <a href="https://github.com/devonfw/IDEasy/issues/1746">#1746</a>).
 */
class JLineTerminalCapabilityTest extends Assertions {

  /**
   * Git Bash (MSYS2) reports the backspace key as {@code kbs=^?}. JLine versions before 4.4.7 decoded this as {@code U+FFFF} instead of the DEL character,
   * so pressing backspace in {@code ide shell} typed a broken character.
   */
  @Test
  void testBackspaceCapabilityIsDecodedAsDel() {

    // arrange
    String kbs = "^?";

    // act
    String decoded = Curses.tputs(kbs);

    // assert
    assertThat(decoded).isEqualTo("\u007F");
  }

}
