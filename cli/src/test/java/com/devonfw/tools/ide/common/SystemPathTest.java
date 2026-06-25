package com.devonfw.tools.ide.common;

import java.nio.file.Files;
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
 * Tests of {@link SystemPath}.
 */
class SystemPathTest extends AbstractIdeContextTest {

  @ParameterizedTest
  // arrange
  @ValueSource(strings = { "C:\\Users\\User\\Documents\\My Pictures\\photo.jpg",
      "C:\\Windows\\System32\\drivers\\etc.sys", "D:\\Projects\\ProjectA\\source\\main.py" })
  void systemPathShouldRecognizeWindowsPaths(String pathStringToTest) {

    // act
    boolean testResult = SystemPath.isValidWindowsPath(pathStringToTest);
    // assert
    assertThat(testResult).isTrue();
  }

  @ParameterizedTest
  // arrange
  @ValueSource(strings = { "-kill", "none", "--help", "/usr/local/bin/firefox.exe" })
  void systemPathShouldRecognizeNonWindowsPaths(String pathStringToTest) {

    // act
    boolean testResult = SystemPath.isValidWindowsPath(pathStringToTest);
    // assert
    assertThat(testResult).isFalse();
  }

  @Test
  void testFindBinaryFindsBashBinaryOnWindowsUsingFilterExcludingWindowsAppsInPath() {
    // arrange
    String path = "project/workspaces";
    IdeTestContext context = newContext("find-binary", path, false);
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    context.setSystemInfo(systemInfo);
    Predicate<Path> ignoreWindowsApps = p -> checkPathToIgnoreLowercase(p, "/appdata/local/microsoft");
    List<Path> paths = new ArrayList<>();
    paths.add(context.getUserHome().resolve("AppData/Local/Microsoft/WindowsApps"));
    paths.add(context.getUserHome().resolve("PortableGit/bin"));
    SystemPath systemPath = new SystemPath(context, context.getSoftwarePath(), ';', paths);
    Path bash = Path.of("bash");

    // act
    Path result = systemPath.findBinary(bash, ignoreWindowsApps);

    // assert
    assertThat(result).isEqualTo(context.getUserHome().resolve("PortableGit").resolve("bin").resolve("bash"));
  }

  @Test
  void testFindBinaryFindsGraalvmBinaryOnWindowsUsingFilterExcludingJava() {
    // arrange
    String path = "project/workspaces";
    IdeTestContext context = newContext("find-binary", path, false);
    Predicate<Path> ignoreJava = p -> checkPathToIgnoreLowercase(p, "java/bin");
    SystemPath systemPath = new SystemPath(context, "", context.getIdeRoot(), context.getSoftwarePath(), ';', new ArrayList<>());
    Path java = Path.of("java");

    // act
    Path result = systemPath.findBinary(java, ignoreJava);

    // assert
    assertThat(result).isEqualTo(context.getSoftwarePath().resolve("graalvm").resolve("bin").resolve("java"));
  }

  @Test
  void testFindBinaryFindsNothingUsingFilterIgnoringEverything() {
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
  void testFindBinaryFindsJavaBinaryOnWindowsUsingFilterExcludingGraalvm() {
    // arrange
    String path = "project/workspaces";
    IdeTestContext context = newContext("find-binary", path, false);
    Predicate<Path> ignoreGraalvm = p -> checkPathToIgnoreLowercase(p, "graalvm/bin");
    SystemPath systemPath = new SystemPath(context, "", context.getIdeRoot(), context.getSoftwarePath(), ';', new ArrayList<>());
    Path java = Path.of("java");

    // act
    Path result = systemPath.findBinary(java, ignoreGraalvm);

    // assert
    assertThat(result).isEqualTo(context.getSoftwarePath().resolve("java").resolve("bin").resolve("java"));
  }

  @Test
  void testFindBinaryFindsJavaBinaryWithoutFilter() {
    // arrange
    String path = "project/workspaces";
    IdeTestContext context = newContext("find-binary", path, false);
    SystemPath systemPath = new SystemPath(context, "", context.getIdeRoot(), context.getSoftwarePath(), ';', new ArrayList<>());
    Path java = Path.of("java");

    // act
    Path result = systemPath.findBinary(java);

    // assert
    assertThat(result).isEqualTo(context.getSoftwarePath().resolve("java").resolve("bin").resolve("java"));
  }

  @Test
  void testFindBinaryFindsNothingWithoutFilter() {
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
    String s = p.toAbsolutePath().toString().replace('\\', '/').toLowerCase(Locale.ROOT);
    return !s.contains(toIgnore);
  }

  @Test
  void testFindBinaryCanResolveRealNodeBinaryWhileExcludingShimDirectory() throws Exception {

    // arrange
    String path = "project/workspaces";
    IdeTestContext context = newContext("find-binary-shims", path, false);
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    context.setSystemInfo(systemInfo);

    Path shims = context.getIdeRoot().resolve("shims");
    Path realNodeBin = context.getIdeRoot().resolve("real-node-bin");

    Files.createDirectories(shims);
    Files.createDirectories(realNodeBin);

    Path shimNode = shims.resolve("node.cmd");
    Path realNode = realNodeBin.resolve("node.exe");

    Files.deleteIfExists(shimNode);
    Files.deleteIfExists(realNode);

    Files.createFile(shimNode);
    Files.createFile(realNode);

    List<Path> paths = new ArrayList<>();
    paths.add(shims);
    paths.add(realNodeBin);

    SystemPath systemPath = new SystemPath(context, context.getSoftwarePath(), ';', paths);
    Path node = Path.of("node");

    Predicate<Path> excludeShims = p -> !p.toAbsolutePath().normalize().startsWith(shims.toAbsolutePath().normalize());

    // act
    Path resultWithoutFilter = systemPath.findBinary(node);
    Path resultWithFilter = systemPath.findBinary(node, excludeShims);

    // assert
    assertThat(resultWithoutFilter).isEqualTo(shimNode);
    assertThat(resultWithFilter).isEqualTo(realNode);
  }

  @Test
  void testCanCreateMinimalExperimentalWindowsShims() throws Exception {

    // arrange
    String path = "project/workspaces";
    IdeTestContext context = newContext("find-binary-shims", path, false);

    Path shims = context.getIdeRoot().resolve("shims");
    Files.createDirectories(shims);

    // act
    Path nodeShim = shims.resolve("node.cmd");
    Path npmShim = shims.resolve("npm.cmd");
    Path npxShim = shims.resolve("npx.cmd");

    Files.writeString(nodeShim, "@echo off\r\nide node %*\r\n");
    Files.writeString(npmShim, "@echo off\r\nide npm %*\r\n");
    Files.writeString(npxShim, "@echo off\r\nide npx %*\r\n");

    // assert
    assertThat(nodeShim).exists();
    assertThat(npmShim).exists();
    assertThat(npxShim).exists();

    assertThat(Files.readString(nodeShim)).contains("ide node %*");
    assertThat(Files.readString(npmShim)).contains("ide npm %*");
    assertThat(Files.readString(npxShim)).contains("ide npx %*");
  }
}
