package com.devonfw.tools.ide.os;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link WindowsPathSyntax}.
 */
public class WindowsPathSyntaxTest extends Assertions {

  /** Test of {@link WindowsPathSyntax#WINDOWS}. */
  @Test
  public void testWindows() {

    // arrange
    WindowsPathSyntax syntax = WindowsPathSyntax.WINDOWS;
    // act && assert
    assertThat(syntax.getDrive("C:\\Windows\\system32\\drivers\\etc\\hosts")).isEqualTo("C");
    assertThat(syntax.getDrive("D:\\projects\\ide\\settings\\ide.properties")).isEqualTo("D");
    assertThat(syntax.getDrive("https://host.com:8443/context/path/collection/id?key=value")).isNull();
    assertThat(syntax.getDrive("$:\\{garbage}§")).isNull();
    assertThat(syntax.getDrive(null)).isNull();
    assertThat(syntax.getRootPath("C")).isEqualTo("C:\\");
    assertThat(syntax.getRootPath("c")).isEqualTo("C:\\");
    assertThat(syntax.getRootPath("Z")).isEqualTo("Z:\\");
  }

  /** Test of {@link WindowsPathSyntax#MSYS}. */
  @Test
  public void testMsys() {

    // arrange
    WindowsPathSyntax syntax = WindowsPathSyntax.MSYS;
    // act && assert
    assertThat(syntax.getDrive("/c/Windows/system32/drivers/etc/hosts")).isEqualTo("c");
    assertThat(syntax.getDrive("/d/projects/ide/settings/ide.properties")).isEqualTo("d");
    assertThat(syntax.getDrive("https://host.com:8443/context/path/collection/id?key=value")).isNull();
    assertThat(syntax.getDrive("$:\\{garbage}§")).isNull();
    assertThat(syntax.getDrive(null)).isNull();
    assertThat(syntax.getRootPath("c")).isEqualTo("/c/");
    assertThat(syntax.getRootPath("C")).isEqualTo("/c/");
    assertThat(syntax.getRootPath("z")).isEqualTo("/z/");
  }

}
