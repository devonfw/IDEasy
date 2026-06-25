package com.devonfw.tools.ide.tool.shim;

import static org.assertj.core.api.Assertions.assertThat;

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
  @EnabledOnOs(OS.WINDOWS)
  @EnabledIfSystemProperty(named = "ideasy.shim.integration", matches = "true")
  void shouldExecuteNodeVersionThroughGeneratedShimAndRealIdeasy() throws Exception {

    // arrange
    ShimLauncherGenerator generator = new ShimLauncherGenerator();

    Path shimsDirectory = this.tempDir.resolve("shims-real-ideasy");
    Path ideBinDirectory = this.tempDir.resolve("ide-bin");

    Files.createDirectories(ideBinDirectory);

    generator.generateWindowsShim(shimsDirectory, "node");

    Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe");
    String classPath = System.getProperty("java.class.path");

    Path ideCmd = ideBinDirectory.resolve("ide.cmd");
    Files.writeString(ideCmd, """
        @echo off
        "%s" -cp "%s" com.devonfw.tools.ide.cli.Ideasy %%*
        exit /b %%ERRORLEVEL%%
        """.formatted(javaExecutable, classPath));

    ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "node -v");

    String originalPath = processBuilder.environment().get("PATH");
    processBuilder.environment().put("PATH", shimsDirectory + ";" + ideBinDirectory + ";" + originalPath);

    processBuilder.redirectErrorStream(true);

    // act
    Process process = processBuilder.start();

    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();

    // assert
    assertThat(output).containsPattern("v\\d+\\.\\d+\\.\\d+");
    assertThat(exitCode).isEqualTo(0);
  }
}
