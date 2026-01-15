package com.devonfw.tools.ide.os;

import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link WindowsPathSyntax}.
 */
class WindowsPathSyntaxTest extends Assertions {

  public static final String WINDOWS_HOSTS = "C:\\Windows\\system32\\drivers\\etc\\hosts";
  public static final String MSYS_HOSTS = "/c/Windows/system32/drivers/etc/hosts";

  /**
   * Test of {@link WindowsPathSyntax#WINDOWS}.
   */
  @Test
  void testWindows() {

    // arrange
    WindowsPathSyntax syntax = WindowsPathSyntax.WINDOWS;
    // act && assert
    assertThat(syntax.getDrive(WINDOWS_HOSTS)).isEqualTo("C");
    assertThat(syntax.getDrive("D:\\projects\\ide\\settings\\ide.properties")).isEqualTo("D");
    assertThat(syntax.getDrive("https://host.com:8443/context/path/collection/id?key=value")).isNull();
    assertThat(syntax.getDrive("$:\\{garbage}§")).isNull();
    assertThat(syntax.getDrive(null)).isNull();
    assertThat(syntax.getRootPath("C")).isEqualTo("C:\\");
    assertThat(syntax.getRootPath("c")).isEqualTo("C:\\");
    assertThat(syntax.getRootPath("Z")).isEqualTo("Z:\\");
    assertThat(syntax.format(Path.of(MSYS_HOSTS))).isEqualTo(WINDOWS_HOSTS);
    if (SystemInfoImpl.INSTANCE.isWindows()) {
      assertThat(syntax.format(Path.of(WINDOWS_HOSTS))).isEqualTo(WINDOWS_HOSTS);
    }
    assertThat(syntax.format(Path.of("/foo/bar/some.txt"))).isEqualTo("\\foo\\bar\\some.txt");
    assertThat(syntax.format(Path.of("foo/bar/some.txt"))).isEqualTo("foo\\bar\\some.txt");
    assertThat(syntax.format(Path.of("./foo/bar/some.txt"))).isEqualTo(".\\foo\\bar\\some.txt");
  }

  /**
   * Test of {@link WindowsPathSyntax#MSYS}.
   */
  @Test
  void testMsys() {

    // arrange
    WindowsPathSyntax syntax = WindowsPathSyntax.MSYS;
    // act && assert
    assertThat(syntax.getDrive(MSYS_HOSTS)).isEqualTo("c");
    assertThat(syntax.getDrive("/d/projects/ide/settings/ide.properties")).isEqualTo("d");
    assertThat(syntax.getDrive("https://host.com:8443/context/path/collection/id?key=value")).isNull();
    assertThat(syntax.getDrive("$:\\{garbage}§")).isNull();
    assertThat(syntax.getDrive(null)).isNull();
    assertThat(syntax.getRootPath("c")).isEqualTo("/c/");
    assertThat(syntax.getRootPath("C")).isEqualTo("/c/");
    assertThat(syntax.getRootPath("z")).isEqualTo("/z/");
    assertThat(syntax.format(Path.of(MSYS_HOSTS))).isEqualTo(MSYS_HOSTS);
    if (SystemInfoImpl.INSTANCE.isWindows()) {
      assertThat(syntax.format(Path.of(WINDOWS_HOSTS))).isEqualTo(MSYS_HOSTS);
      assertThat(syntax.format(Path.of("\\foo\\bar\\some.txt"))).isEqualTo("/foo/bar/some.txt");
      assertThat(syntax.format(Path.of("foo\\bar\\some.txt"))).isEqualTo("foo/bar/some.txt");
      assertThat(syntax.format(Path.of(".\\foo\\bar\\some.txt"))).isEqualTo("./foo/bar/some.txt");
    }
  }

  /**
   * Test of {@link WindowsPathSyntax#normalize(String, boolean)} for Windows.
   */
  @Test
  void testNormalizeWindowsValue2Windows() {

    boolean bash = false;
    assertThat(WindowsPathSyntax.normalize("", bash)).isEqualTo("");
    assertThat(WindowsPathSyntax.normalize("*", bash)).isEqualTo("*");
    assertThat(WindowsPathSyntax.normalize("$:\\\\{garbage}§", bash)).isEqualTo("$:\\\\{garbage}§");
    assertThat(WindowsPathSyntax.normalize("/c/Windows/system32/drivers/etc/hosts", bash))
        .isEqualTo("C:\\Windows\\system32\\drivers\\etc\\hosts");
    assertThat(WindowsPathSyntax.normalize("C:\\Windows\\system32\\drivers\\etc\\hosts", bash))
        .isEqualTo("C:\\Windows\\system32\\drivers\\etc\\hosts");
    assertThat(WindowsPathSyntax.normalize("C:\\Users\\login/.ide/scripts/ide", bash))
        .isEqualTo("C:\\Users\\login\\.ide\\scripts\\ide");
    assertThat(WindowsPathSyntax.normalize("\\login/.ide/scripts/ide", bash)).isEqualTo("\\login/.ide/scripts/ide");
  }

  /**
   * Test of {@link WindowsPathSyntax#normalize(String, boolean)} for (Git-)Bash.
   */
  @Test
  void testNormalizeWindowsValue2Bash() {

    boolean bash = true;
    assertThat(WindowsPathSyntax.normalize("", bash)).isEqualTo("");
    assertThat(WindowsPathSyntax.normalize("*", bash)).isEqualTo("*");
    assertThat(WindowsPathSyntax.normalize("$:\\\\{garbage}§", bash)).isEqualTo("$:\\\\{garbage}§");
    assertThat(WindowsPathSyntax.normalize("C:\\Windows\\system32\\drivers\\etc\\hosts", bash))
        .isEqualTo("/c/Windows/system32/drivers/etc/hosts");
    assertThat(WindowsPathSyntax.normalize("/c/Windows/system32/drivers/etc/hosts", bash))
        .isEqualTo("/c/Windows/system32/drivers/etc/hosts");
  }

  /**
   * Test of {@link WindowsPathSyntax#normalize(String, boolean)} for Windows.
   */
  @Test
  void testNormalizeWindowsPath() {

    // arrange
    String path = "/c/Windows/system32/drivers/etc/hosts";
    // act
    String normalized = WindowsPathSyntax.normalize(path, false);
    // assert
    assertThat(normalized).isEqualTo("C:\\Windows\\system32\\drivers\\etc\\hosts");
  }

  @Test
  void systemPathShouldConvertWindowsPathToUnixPath() {

    // arrange
    String windowsPathString = "C:\\Users\\User\\test.exe";
    String expectedUnixPathString = "/c/Users/User/test.exe";

    // act
    String resultPath = WindowsPathSyntax.normalize(windowsPathString, true);

    // assert
    assertThat(resultPath).isEqualTo(expectedUnixPathString);
  }

}
