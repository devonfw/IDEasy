package com.devonfw.tools.ide.tool.shim;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShimLauncherGeneratorTest {

  @TempDir
  Path tempDir;

  @Test
  void shouldGenerateWindowsShimForNode() throws Exception {

    // arrange
    ShimLauncherGenerator generator = new ShimLauncherGenerator();
    Path shimsDirectory = this.tempDir.resolve("shims");

    // act
    Path shim = generator.generateWindowsShim(shimsDirectory, "node");

    // assert
    assertThat(shim).exists();
    assertThat(shim.getFileName().toString()).isEqualTo("node.cmd");
    assertThat(Files.readString(shim)).isEqualTo("@echo off\r\nide node %*\r\nexit /b %ERRORLEVEL%\r\n");
  }

  @Test
  void shouldGenerateWindowsShimsForNpmAndNpx() throws Exception {

    // arrange
    ShimLauncherGenerator generator = new ShimLauncherGenerator();
    Path shimsDirectory = this.tempDir.resolve("shims");

    // act
    Path npmShim = generator.generateWindowsShim(shimsDirectory, "npm");
    Path npxShim = generator.generateWindowsShim(shimsDirectory, "npx");

    // assert
    assertThat(npmShim).exists();
    assertThat(npxShim).exists();
    assertThat(Files.readString(npmShim)).isEqualTo("@echo off\r\nide npm %*\r\nexit /b %ERRORLEVEL%\r\n");
    assertThat(Files.readString(npxShim)).isEqualTo("@echo off\r\nide npx %*\r\nexit /b %ERRORLEVEL%\r\n");
  }

  @Test
  void shouldGenerateShellShimForNode() throws Exception {

    // arrange
    ShimLauncherGenerator generator = new ShimLauncherGenerator();
    Path shimsDirectory = this.tempDir.resolve("shims");

    // act
    Path shim = generator.generateShellShim(shimsDirectory, "node");

    // assert
    assertThat(shim).exists();
    assertThat(shim.getFileName().toString()).isEqualTo("node");
    assertThat(Files.readString(shim)).isEqualTo("#!/usr/bin/env bash\nexec ide node \"$@\"\n");
    assertThat(shim.toFile().canExecute()).isTrue();
  }
}
