package com.devonfw.tools.ide.io.ini;

import java.util.Arrays;
import java.util.List;

/**
 * Implementation of {@link IniElement}
 */
public class IniCommentImpl extends IniElement {

  /**
   * List of Characters that may mark a comment
   */
  public static List<Character> commentSymbols = Arrays.asList(';', '#');

  /**
   * @param content the content of the comment, including comment marker like ";"
   */
  public IniCommentImpl(String content) throws IllegalArgumentException {
    String trimmedContent = content.trim();
    if (!trimmedContent.isEmpty() && !commentSymbols.contains(trimmedContent.charAt(0))) {
      throw new IllegalArgumentException("Comments must begin with comment symbol or contain only whitespace");
    }
    this.content = content;
  }

}
