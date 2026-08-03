package com.devonfw.tools.ide.variable;

import java.util.regex.Matcher;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link VariableSyntax}.
 */
class VariableSyntaxTest extends Assertions {

  /**
   * Test of {@link VariableSyntax#CURLY}.
   */
  @Test
  void testCurly() {

    // arrange
    VariableSyntax syntax = VariableSyntax.CURLY;
    // act
    String result = replaceAll("start${ENVIRONMENT_VARIABLE}:?foo$[REPLACEMENT_VARIABLE].${invalid-syntax$PATH;bar${PATH}$[PATH]end", syntax);
    String var = syntax.create("JAVA_HOME");
    // assert
    assertThat(result).isEqualTo("start%ENVIRONMENT_VARIABLE%:?foo$[REPLACEMENT_VARIABLE].${invalid-syntax$PATH;bar%PATH%$[PATH]end");
    assertThat(var).isEqualTo("${JAVA_HOME}");
  }

  /**
   * Test of {@link VariableSyntax#CURLY}.
   */
  @Test
  void testSquare() {

    // arrange
    VariableSyntax syntax = VariableSyntax.SQUARE;
    // act
    String result = replaceAll("${ENVIRONMENT_VARIABLE}:?foo$[REPLACEMENT_VARIABLE].${invalid-syntax$PATH;bar${PATH}$[PATH]end", syntax);
    String var = syntax.create("JAVA_HOME");
    // assert
    assertThat(result).isEqualTo("${ENVIRONMENT_VARIABLE}:?foo%REPLACEMENT_VARIABLE%.${invalid-syntax$PATH;bar${PATH}%PATH%end");
    assertThat(var).isEqualTo("$[JAVA_HOME]");
  }

  private String replaceAll(String string, VariableSyntax syntax) {

    StringBuilder sb = new StringBuilder(string.length());
    Matcher matcher = syntax.getPattern().matcher(string);
    while (matcher.find()) {
      String variableName = syntax.getVariable(matcher);
      matcher.appendReplacement(sb, "%" + variableName + "%");
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}
