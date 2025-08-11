package com.devonfw.tools.ide.io.ini;

abstract class IniElement {

  private String content;

  protected String getContent() {
    return content;
  }

  protected void setContent(String content) {
    this.content = content;
  }

  public String toString() {
    return content;
  }

  void write(StringBuilder stringBuilder) {
    stringBuilder.append(this);
  }

  protected String getIndentation() {
    int nonWhitespaceStart = content.length() - content.stripLeading().length();
    return content.substring(0, nonWhitespaceStart);
  }
}
