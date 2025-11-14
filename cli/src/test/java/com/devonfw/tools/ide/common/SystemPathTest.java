package com.devonfw.tools.ide.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Unit tests of {@link SystemPath}.
 */
public class SystemPathTest extends AbstractIdeContextTest {

  @ParameterizedTest
  // arrange
  @ValueSource(strings = { "C:\\Users\\User\\Documents\\My Pictures\\photo.jpg",
      "C:\\Windows\\System32\\drivers\\etc.sys", "D:\\Projects\\ProjectA\\source\\main.py" })
  public void systemPathShouldRecognizeWindowsPaths(String pathStringToTest) {

    // act
    boolean testResult = SystemPath.isValidWindowsPath(pathStringToTest);
    // assert
    assertThat(testResult).isTrue();
  }

  @ParameterizedTest
  // arrange
  @ValueSource(strings = { "-kill", "none", "--help", "/usr/local/bin/firefox.exe" })
  public void systemPathShouldRecognizeNonWindowsPaths(String pathStringToTest) {

    // act
    boolean testResult = SystemPath.isValidWindowsPath(pathStringToTest);
    // assert
    assertThat(testResult).isFalse();
  }

  @Test
  public void testfindBinaryFindsBashBinaryOnWindowsUsingFilterExcludingWindowsAppsInPath() {
    // arrange
    String path = "project/workspaces";
    IdeTestContext context = newContext("find-binary", path, false);
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    context.setSystemInfo(systemInfo);
    Predicate<Path> ignoreWindowsApps = p -> checkPathToIgnoreLowercase(p, "appdata\\local\\microsoft");
    List<Path> paths = new ArrayList<>();
    paths.add(context.getUserHome().resolve("AppData\\Local\\Microsoft\\WindowsApps"));
    paths.add(context.getUserHome().resolve("PortableGit\\bin"));
    SystemPath systemPath = new SystemPath(context, context.getSoftwarePath(), ';', paths);
    Path bash = Path.of("bash");

    // act
    Path result = systemPath.findBinary(bash, ignoreWindowsApps);

    // assert
    assertThat(result).isEqualTo(context.getUserHome().resolve("PortableGit").resolve("bin").resolve("bash.exe"));
  }

  @Test
  public void testfindBinaryFindsGraalvmBinaryOnWindowsUsingFilterExcludingJava() {
    // arrange
    String path = "project/workspaces";
    IdeTestContext context = newContext("find-binary", path, false);
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    context.setSystemInfo(systemInfo);
    Predicate<Path> ignoreJava = p -> checkPathToIgnoreLowercase(p, "java\\bin");
    SystemPath systemPath = new SystemPath(context, "", context.getIdeRoot(), context.getSoftwarePath(), ';', new ArrayList<>());
    Path java = Path.of("java");

    // act
    Path result = systemPath.findBinary(java, ignoreJava);

    // assert
    assertThat(result).isEqualTo(context.getSoftwarePath().resolve("graalvm").resolve("bin").resolve("java.exe"));
  }

  @Test
  public void testfindBinaryFindsNothingUsingFilterIgnoringEverything() {
    // arrange
    String path = "project/workspaces";
    IdeTestContext context = newContext("find-binary", path, false);
    Predicate<Path> ignoreEverything = p -> false;
    SystemPath systemPath = new SystemPath(context, "", context.getIdeRoot(), context.getSoftwarePath(), ';', new ArrayList<>());
    Path java = Path.of("java");

    // act
    Path result = systemPath.findBinary(java, ignoreEverything);

    // assert
    assertThat(result).isEqualTo(java);
  }

  @Test
  public void testfindBinaryFindsJavaBinaryOnWindowsUsingFilterExcludingGraalvm() {
    // arrange
    String path = "project/workspaces";
    IdeTestContext context = newContext("find-binary", path, false);
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    context.setSystemInfo(systemInfo);
    Predicate<Path> ignoreGraalvm = p -> checkPathToIgnoreLowercase(p, "graalvm\\bin");
    SystemPath systemPath = new SystemPath(context, "", context.getIdeRoot(), context.getSoftwarePath(), ';', new ArrayList<>());
    Path java = Path.of("java");

    // act
    Path result = systemPath.findBinary(java, ignoreGraalvm);

    // assert
    assertThat(result).isEqualTo(context.getSoftwarePath().resolve("java").resolve("bin").resolve("java.exe"));
  }

  @Test
  public void testfindBinaryFindsJavaBinaryOnWindowsWithoutFilter() {
    // arrange
    String path = "project/workspaces";
    IdeTestContext context = newContext("find-binary", path, false);
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    context.setSystemInfo(systemInfo);
    SystemPath systemPath = new SystemPath(context, "", context.getIdeRoot(), context.getSoftwarePath(), ';', new ArrayList<>());
    Path java = Path.of("java");

    // act
    Path result = systemPath.findBinary(java);

    // assert
    assertThat(result).isEqualTo(context.getSoftwarePath().resolve("java").resolve("bin").resolve("java.exe"));
  }

  @Test
  public void testfindBinaryFindsNothingWithoutFilter() {
    // arrange
    String path = "project/workspaces";
    IdeTestContext context = newContext("find-binary", path, false);
    SystemPath systemPath = new SystemPath(context, "", context.getIdeRoot(), context.getSoftwarePath(), ';', new ArrayList<>());
    Path test = Path.of("test");

    // act
    Path result = systemPath.findBinary(test);

    // assert
    assertThat(result).isEqualTo(test);
  }

  private static boolean checkPathToIgnoreLowercase(Path p, String toIgnore) {
    String s = p.toAbsolutePath().toString().replace('/', '\\').toLowerCase(Locale.ROOT);
    return !s.contains(toIgnore);
  }
}
