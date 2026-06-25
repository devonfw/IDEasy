package com.devonfw.tools.ide.tool.shim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class ShimLauncherExecutionTest {

  @TempDir
  Path tempDir;

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void shouldExecuteGeneratedWindowsShimAndPreserveCommandBehavior() throws Exception {

    // arrange
    ShimLauncherGenerator generator = new ShimLauncherGenerator();

    Path shimsDirectory = this.tempDir.resolve("shims");
    Path fakeIdeDirectory = this.tempDir.resolve("fake-ide-bin");

    Files.createDirectories(fakeIdeDirectory);

    generator.generateWindowsShim(shimsDirectory, "node");

    Path fakeIde = fakeIdeDirectory.resolve("ide.cmd");
    Files.writeString(fakeIde, """
        @echo off
        echo tool=%1
        shift
        echo args=%*
        echo stderr-marker 1>&2
        exit /b 23
        """);

    ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "node -v -- \"hello world\"");

    String originalPath = processBuilder.environment().get("PATH");
    processBuilder.environment().put("PATH", shimsDirectory + ";" + fakeIdeDirectory + ";" + originalPath);

    // act
    Process process = processBuilder.start();

    String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();

    // assert
    assertThat(exitCode).isEqualTo(23);
    assertThat(stdout).contains("tool=node");
    assertThat(stdout).contains("-v");
    assertThat(stdout).contains("--");
    assertThat(stdout).contains("hello world");
    assertThat(stderr).contains("stderr-marker");
  }

  @Test
  void shouldExecuteGeneratedShellShimAndPreserveCommandBehavior() throws Exception {

    // arrange
    Path bash = findBash();
    assumeTrue(bash != null, "bash is required for shell shim execution test");
    assumeTrue(canRunBash(bash), "usable bash is required for shell shim execution test");

    ShimLauncherGenerator generator = new ShimLauncherGenerator();

    Path shimsDirectory = this.tempDir.resolve("shims");
    Path fakeIdeDirectory = this.tempDir.resolve("fake-ide-bin");

    generator.generateShellShim(shimsDirectory, "node");

    Files.createDirectories(fakeIdeDirectory);
    Path fakeIde = fakeIdeDirectory.resolve("ide");
    Files.writeString(fakeIde, """
        #!/usr/bin/env bash
        printf 'tool=%s\\n' "$1"
        shift
        index=0
        for arg in "$@"; do
          printf 'arg[%s]=%s\\n' "$index" "$arg"
          index=$((index + 1))
        done
        printf 'stderr-marker\\n' >&2
        exit 23
        """);
    fakeIde.toFile().setExecutable(true, false);

    String shimsPath = toBashPath(shimsDirectory);
    String fakeIdePath = toBashPath(fakeIdeDirectory);
    Path runner = this.tempDir.resolve("run-node-through-shim.sh");
    Files.writeString(runner, """
        #!/usr/bin/env bash
        export PATH="$1:$2:$PATH"
        shift 2
        node "$@"
        """);
    runner.toFile().setExecutable(true, false);

    ProcessBuilder processBuilder = new ProcessBuilder(bash.toString(), "--noprofile", "--norc", runner.toString(), shimsPath, fakeIdePath, "alpha",
        "two words", "--", "path with spaces", "tail");
    processBuilder.environment().remove("BASH_ENV");
    processBuilder.environment().remove("ENV");

    // act
    Process process = processBuilder.start();

    String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();

    // assert
    assertThat(exitCode)
        .as("shell shim stdout:%n%s%nshell shim stderr:%n%s", stdout, stderr)
        .isEqualTo(23);
    assertThat(stdout).contains("tool=node");
    assertThat(stdout).contains("arg[0]=alpha");
    assertThat(stdout).contains("arg[1]=two words");
    assertThat(stdout).contains("arg[2]=--");
    assertThat(stdout).contains("arg[3]=path with spaces");
    assertThat(stdout).contains("arg[4]=tail");
    assertThat(stderr).contains("stderr-marker");
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  @EnabledIfSystemProperty(named = "ideasy.shim.integration", matches = "true")
  void shouldExecuteNodeVersionThroughGeneratedShimAndRealIdeasy() throws Exception {

    // arrange
    Path shimsDirectory = this.tempDir.resolve("shims-node");
    Path ideBinDirectory = this.tempDir.resolve("ide-bin-node");

    createWindowsShim("node", shimsDirectory);
    createIdeCmdBridge(ideBinDirectory);

    ProcessBuilder processBuilder = createCmdProcessWithShimPath(
        shimsDirectory,
        ideBinDirectory,
        "node -v"
    );

    processBuilder.redirectErrorStream(true);

    // act
    Process process = processBuilder.start();

    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();

    // assert
    assertThat(exitCode)
        .as("node shim output:%n%s", output)
        .isEqualTo(0);
    assertThat(output).containsPattern("v\\d+\\.\\d+\\.\\d+");
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  @EnabledIfSystemProperty(named = "ideasy.shim.integration", matches = "true")
  void shouldExecuteNpmVersionThroughGeneratedShimAndRealIdeasy() throws Exception {

    // arrange
    Path shimsDirectory = this.tempDir.resolve("shims-npm");
    Path ideBinDirectory = this.tempDir.resolve("ide-bin-npm");

    createWindowsShim("npm", shimsDirectory);
    createIdeCmdBridge(ideBinDirectory);

    ProcessBuilder processBuilder = createCmdProcessWithShimPath(
        shimsDirectory,
        ideBinDirectory,
        "npm -v"
    );

    processBuilder.redirectErrorStream(true);

    // act
    Process process = processBuilder.start();

    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();

    // assert
    assertThat(exitCode)
        .as("npm shim output:%n%s", output)
        .isEqualTo(0);
    assertThat(output).containsPattern("\\d+\\.\\d+\\.\\d+");
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  @EnabledIfSystemProperty(named = "ideasy.shim.integration", matches = "true")
  void shouldDocumentThatNpxShimIsCurrentlyBlockedBecauseIdeasyHasNoNpxCommand() throws Exception {

    // arrange
    Path shimsDirectory = this.tempDir.resolve("shims-npx");
    Path ideBinDirectory = this.tempDir.resolve("ide-bin-npx");

    createWindowsShim("npx", shimsDirectory);
    createIdeCmdBridge(ideBinDirectory);

    ProcessBuilder processBuilder = createCmdProcessWithShimPath(
        shimsDirectory,
        ideBinDirectory,
        "npx -v"
    );

    processBuilder.redirectErrorStream(true);

    // act
    Process process = processBuilder.start();

    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();

    // assert
    assertThat(exitCode)
        .as("npx shim output:%n%s", output)
        .isEqualTo(1);

    assertThat(output).contains("Unknown command \"npx\"");
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  @EnabledIfSystemProperty(named = "ideasy.shim.integration", matches = "true")
  void shouldHandleNodeScriptWithSpaces() throws Exception {

    // arrange
    Path shimsDirectory = this.tempDir.resolve("shims-spaces");
    Path ideBinDirectory = this.tempDir.resolve("ide-bin-spaces");

    createWindowsShim("node", shimsDirectory);
    createIdeCmdBridge(ideBinDirectory);

    Path script = this.tempDir.resolve("test script.js");
    Files.writeString(script, "console.log('hello from script');");

    ProcessBuilder processBuilder = createCmdProcessWithShimPath(
        shimsDirectory,
        ideBinDirectory,
        "node \"" + script + "\""
    );

    processBuilder.redirectErrorStream(true);

    // act
    Process process = processBuilder.start();

    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();

    // assert
    assertThat(exitCode)
        .as("node script with spaces output:%n%s", output)
        .isEqualTo(0);
    assertThat(output).contains("hello from script");
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  @EnabledIfSystemProperty(named = "ideasy.shim.integration", matches = "true")
  void shouldForwardNodeEvalArgumentsThroughGeneratedShimAndRealIdeasy() throws Exception {

    // arrange
    Path shimsDirectory = this.tempDir.resolve("shims-eval");
    Path ideBinDirectory = this.tempDir.resolve("ide-bin-eval");

    createWindowsShim("node", shimsDirectory);
    createIdeCmdBridge(ideBinDirectory);

    ProcessBuilder processBuilder = createCmdProcessWithShimPath(
        shimsDirectory,
        ideBinDirectory,
        "node -e \"console.log('arg test')\" -- test"
    );

    processBuilder.redirectErrorStream(true);

    // act
    Process process = processBuilder.start();

    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();

    // assert
    assertThat(exitCode)
        .as("node eval shim output:%n%s", output)
        .isEqualTo(0);
    assertThat(output).contains("arg test");
  }

  private void createWindowsShim(String toolName, Path shimsDirectory) throws Exception {

    ShimLauncherGenerator generator = new ShimLauncherGenerator();
    generator.generateWindowsShim(shimsDirectory, toolName);
  }

  private void createIdeCmdBridge(Path ideBinDirectory) throws Exception {

    Files.createDirectories(ideBinDirectory);

    Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe");
    String classPath = System.getProperty("java.class.path");

    Path ideCmd = ideBinDirectory.resolve("ide.cmd");
    Files.writeString(ideCmd, """
        @echo off
        "%s" -cp "%s" com.devonfw.tools.ide.cli.Ideasy %%*
        exit /b %%ERRORLEVEL%%
        """.formatted(javaExecutable, classPath));
  }

  private ProcessBuilder createCmdProcessWithShimPath(Path shimsDirectory, Path ideBinDirectory, String command) {

    ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);

    String originalPath = processBuilder.environment().get("PATH");
    processBuilder.environment().put("PATH", shimsDirectory + ";" + ideBinDirectory + ";" + originalPath);

    return processBuilder;
  }

  private Path findBash() {

    String path = System.getenv("PATH");
    if (path == null) {
      return null;
    }
    String[] segments = path.split(File.pathSeparator);
    for (String segment : segments) {
      Path directory = Path.of(segment);
      Path bash = directory.resolve("bash");
      if (Files.exists(bash)) {
        return bash;
      }
      Path bashExe = directory.resolve("bash.exe");
      if (Files.exists(bashExe)) {
        return bashExe;
      }
    }
    return null;
  }

  private boolean canRunBash(Path bash) throws Exception {

    Process process = new ProcessBuilder(bash.toString(), "-lc", "true").start();
    return process.waitFor() == 0;
  }

  private String toBashPath(Path path) {

    String normalized = path.toAbsolutePath().normalize().toString().replace('\\', '/');
    if ((normalized.length() >= 2) && (normalized.charAt(1) == ':')) {
      return "/" + Character.toLowerCase(normalized.charAt(0)) + normalized.substring(2);
    }
    return normalized;
  }
}
