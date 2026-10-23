package com.devonfw.tools.ide.tool;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link AbstractToolCommandlet}.
 */
class AbstractToolCommandletTest extends AbstractIdeContextTest {

  private static final String TOOL_NAME = "docker";

  static class TestToolCommandlet extends GlobalToolCommandlet {

    TestToolCommandlet(IdeContext context) {

      super(context, TOOL_NAME, Set.of(Tag.DOCKER));
    }

    @Override
    protected void completeRequest(ToolInstallRequest request) {

      // Not required for these unit tests.
    }

    @Override
    protected ToolInstallation doInstall(ToolInstallRequest request) {

      throw new UnsupportedOperationException("Installation is not required for these unit tests.");
    }
  }

  @Test
  void testCreateStartScriptCreatesScriptInBinDirectory(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    TestToolCommandlet commandlet = new TestToolCommandlet(context);

    Path binDir = Files.createDirectory(tempDir.resolve("bin"));

    // act
    commandlet.createStartScript(tempDir, "dockerBinary", false);

    // assert
    Path script = binDir.resolve(TOOL_NAME);

    assertThat(script)
        .hasContent("#!/usr/bin/env bash\n\"$(dirname \"$0\")/dockerBinary\" $@")
        .isExecutable();
  }

  @Test
  void testCreateStartScriptBackground(@TempDir Path tempDir) {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    TestToolCommandlet commandlet = new TestToolCommandlet(context);

    // act
    commandlet.createStartScript(tempDir, "dockerBinary", true);

    // assert
    Path script = tempDir.resolve(TOOL_NAME);

    assertThat(script)
        .hasContent("#!/usr/bin/env bash\n\"$(dirname \"$0\")/dockerBinary\" $@ &")
        .isExecutable();
  }

  @Test
  void testResolveVersionWithPatternMatch() {

    IdeTestContext context = newContext(PROJECT_BASIC);
    TestToolCommandlet commandlet = new TestToolCommandlet(context);

    Pattern pattern = Pattern.compile("v(\\d+\\.\\d+)");

    VersionIdentifier result =
        commandlet.resolveVersionWithPattern("release v2.5", pattern);

    assertThat(result).isEqualTo(VersionIdentifier.of("2.5"));
  }

  @Test
  void testResolveVersionWithPatternNoMatch() {

    IdeTestContext context = newContext(PROJECT_BASIC);
    TestToolCommandlet commandlet = new TestToolCommandlet(context);

    Pattern pattern = Pattern.compile("v(\\d+\\.\\d+)");

    VersionIdentifier result =
        commandlet.resolveVersionWithPattern("release latest", pattern);

    assertThat(result).isNull();
  }

  @Test
  void testResolveVersionWithPatternMultipleMatches() {

    IdeTestContext context = newContext(PROJECT_BASIC);
    TestToolCommandlet commandlet = new TestToolCommandlet(context);

    Pattern pattern = Pattern.compile("v(\\d+\\.\\d+)");

    VersionIdentifier result =
        commandlet.resolveVersionWithPattern("v1.0 v2.0", pattern);

    assertThat(result).isEqualTo(VersionIdentifier.of("1.0"));
  }

  @Test
  void testCreateStartScriptCreatesScriptForMacApplication(@TempDir Path tempDir) {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    TestToolCommandlet commandlet = new TestToolCommandlet(context);

    Path appDir = tempDir.resolve("Docker.app");
    Path linkDir = appDir
        .resolve(IdeContext.FOLDER_CONTENTS)
        .resolve(IdeContext.FOLDER_RESOURCES)
        .resolve(IdeContext.FOLDER_APP);

    context.getFileAccess().mkdirs(linkDir.resolve(IdeContext.FOLDER_BIN));

    // act
    commandlet.createStartScript(tempDir, "dockerBinary", false);

    // assert
    Path script = linkDir.resolve(TOOL_NAME);

    assertThat(script)
        .hasContent("#!/usr/bin/env bash\n\"$(dirname \"$0\")/dockerBinary\" $@")
        .isExecutable();
  }
}
