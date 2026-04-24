package com.devonfw.tools.ide.commandlet;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link LnCommandlet}.
 */
class LnCommandletTest extends AbstractIdeContextTest {

  /**
   * Tests a link creation on Windows.
   */
  @Test
  @EnabledOnOs(OS.WINDOWS)
  void testLnCreatesRealLinkAndReflectsChanges_Windows() throws Exception {
    IdeTestContext context = newContext(PROJECT_BASIC);

    Path testDir = context.getWorkspacePath().resolve("ln-test");
    context.getFileAccess().mkdirs(testDir);
    context.setCwd(testDir, context.getWorkspaceName(), context.getIdeHome());

    Path source = testDir.resolve("source.txt");
    Path link = testDir.resolve("link.txt");
    Files.writeString(source, "A", StandardCharsets.UTF_8);

    LnCommandlet cmd = new LnCommandlet(context);
    cmd.symbolic.setValueAsString("-s", context);
    cmd.source.setValueAsString("source.txt", context);
    cmd.target.setValueAsString("link.txt", context);

    cmd.run();

    assertThat(link).exists();

    Files.writeString(source, "B", StandardCharsets.UTF_8);
    assertThat(Files.readString(link, StandardCharsets.UTF_8)).isEqualTo("B");
  }

  /**
   * Tests a link creation on Unix.
   */
  @Test
  @EnabledOnOs({ OS.LINUX, OS.MAC })
  void testLnCreatesRealLinkAndReflectsChanges_Unix() throws Exception {
    IdeTestContext context = newContext(PROJECT_BASIC);

    Path testDir = context.getWorkspacePath().resolve("ln-test");
    context.getFileAccess().mkdirs(testDir);
    context.setCwd(testDir, context.getWorkspaceName(), context.getIdeHome());

    Path source = testDir.resolve("source.txt");
    Path link = testDir.resolve("link.txt");
    Files.writeString(source, "A", StandardCharsets.UTF_8);

    LnCommandlet cmd = new LnCommandlet(context);
    cmd.symbolic.setValueAsString("-s", context);
    cmd.source.setValueAsString("source.txt", context);
    cmd.target.setValueAsString("link.txt", context);

    cmd.run();

    assertThat(link).exists();

    Files.writeString(source, "B", StandardCharsets.UTF_8);
    assertThat(Files.readString(link, StandardCharsets.UTF_8)).isEqualTo("B");
  }
}
