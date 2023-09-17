package com.devonfw.tools.ide.environment;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.environment.VariableLine.Comment;
import com.devonfw.tools.ide.environment.VariableLine.Empty;
import com.devonfw.tools.ide.environment.VariableLine.Garbage;
import com.devonfw.tools.ide.environment.VariableLine.Variable;
import com.devonfw.tools.ide.log.IdeSlf4jRootLogger;

/**
 * Test of {@link VariableLine}.
 */
public class VariableLineTest extends Assertions {

  private static final IdeSlf4jRootLogger LOGGER = IdeSlf4jRootLogger.of();

  private VariableLine line(String line) {

    return VariableLine.of(line, LOGGER, "junit-source");
  }

  /**
   * Test that comments are parsed properly.
   */
  @Test
  public void testComment() {

    checkComment("# this is a comment ");
    checkComment(" #this is a comment");
  }

  private void checkComment(String line) {

    VariableLine variableLine = line(line);
    assertThat(variableLine).hasToString(line);
    assertThat(variableLine).isInstanceOf(Comment.class);
    assertThat(variableLine.getComment()).isEqualTo(line);
    assertThat(variableLine.getName()).isNull();
    assertThat(variableLine.getValue()).isNull();
    assertThat(variableLine.isExport()).isFalse();
  }

  /**
   * Test empty lines.
   */
  @Test
  public void testEmpty() {

    checkEmtpy("");
    checkEmtpy(" ");
  }

  private void checkEmtpy(String line) {

    VariableLine variableLine = line(line);
    assertThat(variableLine).hasToString(line);
    assertThat(variableLine).isInstanceOf(Empty.class);
    assertThat(variableLine.getName()).isNull();
    assertThat(variableLine.getValue()).isNull();
    assertThat(variableLine.getComment()).isNull();
    assertThat(variableLine.isExport()).isFalse();
  }

  /**
   * Test regular variable lines.
   */
  @Test
  public void testVariable() {

    checkVariable("TOOL_VERSION=47.11", false, "TOOL_VERSION", "47.11");
    checkVariable("TOOL_VERSION = 47.11", false, "TOOL_VERSION", "47.11");
    checkVariable("TOOL_VERSION=\"47.11\"", false, "TOOL_VERSION", "47.11");
    checkVariable("TOOL_VERSION = \"47.11\" ", false, "TOOL_VERSION", "47.11");
    checkVariable("export MAVEN_OPTS=-Xmx2g -Pdev", true, "MAVEN_OPTS", "-Xmx2g -Pdev");
    checkVariable("vaR_namE=", false, "vaR_namE", null);
    // edge-cases
    checkVariable("  export  MAVEN_OPTS = -Xmx2g -Pdev", true, "MAVEN_OPTS", "-Xmx2g -Pdev");
    checkVariable("export=value", false, "export", "value");
    checkVariable("export  =value", false, "export", "value");
    checkVariable("export Var_Name = value ", true, "Var_Name", "value");
    checkVariable("Var_Name=\" value \"", false, "Var_Name", " value ");
  }

  private void checkVariable(String line, boolean export, String name, String value) {

    VariableLine variableLine = line(line);
    assertThat(variableLine).hasToString(line);
    assertThat(variableLine).isInstanceOf(Variable.class);
    assertThat(variableLine.getName()).isEqualTo(name);
    assertThat(variableLine.getValue()).isEqualTo(value);
    assertThat(variableLine.getComment()).isNull();
    assertThat(variableLine.isExport()).isEqualTo(export);
  }

  /**
   * Test garbage lines.
   */
  @Test
  public void testGarbage() {

    checkGarbage("TOOL_VERSION 47.11");
  }

  private void checkGarbage(String line) {

    VariableLine variableLine = line(line);
    assertThat(variableLine).hasToString(line);
    assertThat(variableLine).isInstanceOf(Garbage.class);
    assertThat(variableLine.getName()).isNull();
    assertThat(variableLine.getValue()).isNull();
    assertThat(variableLine.getComment()).isNull();
    assertThat(variableLine.isExport()).isFalse();
  }

}
