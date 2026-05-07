package com.devonfw.tools.ide.commandlet;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link LnCommandlet}.
 */
class LnCommandletTest extends AbstractIdeContextTest {

  /**
   * Tests symbolic link creation on Windows with -s flag. On Windows without developer mode or admin privileges,
   * symbolic link creation may fail and fall back to hard links instead, but the link should still be created and
   * reflect source changes.
   */
  @Test
  @EnabledOnOs(OS.WINDOWS)
  void testLnCreatesRealLinkAndReflectsChanges_Windows() throws Exception {
    IdeTestContext context = newContext(PROJECT_BASIC);

    Path testDir = context.getWorkspacePath().resolve("ln-test-windows");
    context.getFileAccess().mkdirs(testDir);
    context.setCwd(testDir, context.getWorkspaceName(), context.getIdeHome());

    Path source = testDir.resolve("source.txt");
    Path link = testDir.resolve("link.txt");
    Files.writeString(source, "A", StandardCharsets.UTF_8);

    LnCommandlet cmd = new LnCommandlet(context);
    cmd.symbolic.setValue(Boolean.TRUE);
    cmd.source.setValue("source.txt");
    cmd.link.setValue("link.txt");

    cmd.run();

    assertThat(link).exists();

    // On Windows, if symbolic link creation fails due to missing privileges, it falls back to hard link
    // Either way, the link should reflect source changes
    Files.writeString(source, "B", StandardCharsets.UTF_8);
    assertThat(Files.readString(link, StandardCharsets.UTF_8)).isEqualTo("B");
  }

  /**
   * Tests symbolic link creation on Linux/Mac with -s flag. This should create a true symbolic link with relative
   * path (matching ln default behavior).
   */
  @Test
  @EnabledOnOs({ OS.LINUX, OS.MAC })
  void testLnCreatesSymbolicLinkAndReflectsChanges_Unix() throws Exception {
    IdeTestContext context = newContext(PROJECT_BASIC);

    Path testDir = context.getWorkspacePath().resolve("ln-test-unix");
    context.getFileAccess().mkdirs(testDir);
    context.setCwd(testDir, context.getWorkspaceName(), context.getIdeHome());

    Path source = testDir.resolve("source.txt");
    Path link = testDir.resolve("link.txt");
    Files.writeString(source, "A", StandardCharsets.UTF_8);

    LnCommandlet cmd = new LnCommandlet(context);
    cmd.symbolic.setValue(Boolean.TRUE);
    cmd.source.setValue("source.txt");
    cmd.link.setValue("link.txt");

    cmd.run();

    assertThat(link).exists().isSymbolicLink();

    // Verify link target is relative (PR requirement: relative by default like ln)
    Path linkTarget = Files.readSymbolicLink(link);
    assertThat(linkTarget.isAbsolute()).isFalse();
    assertThat(linkTarget.toString()).isEqualTo("source.txt");

    Files.writeString(source, "B", StandardCharsets.UTF_8);
    assertThat(Files.readString(link, StandardCharsets.UTF_8)).isEqualTo("B");
  }

  /**
   * Tests default link creation (without -s) which should create a hard link.
   */
  @Test
  void testLnCreatesHardLinkByDefault() throws Exception {
    IdeTestContext context = newContext(PROJECT_BASIC);

    Path testDir = context.getWorkspacePath().resolve("ln-test-hardlink");
    context.getFileAccess().mkdirs(testDir);
    context.setCwd(testDir, context.getWorkspaceName(), context.getIdeHome());

    Path source = testDir.resolve("source.txt");
    Path link = testDir.resolve("link.txt");
    Files.writeString(source, "A", StandardCharsets.UTF_8);

    LnCommandlet cmd = new LnCommandlet(context);
    cmd.source.setValue("source.txt");
    cmd.link.setValue("link.txt");

    cmd.run();

    assertThat(link).exists();

    Files.writeString(source, "B", StandardCharsets.UTF_8);
    assertThat(Files.readString(link, StandardCharsets.UTF_8)).isEqualTo("B");
  }

  /**
   * Tests that -f (force) flag allows overriding existing symbolic links on Linux/Mac. Without -f, attempting to
   * create a link to an existing link path should fail.
   */
  @Test
  @EnabledOnOs({ OS.LINUX, OS.MAC })
  void testForceOverridesExistingSymbolicLink() throws Exception {
    IdeTestContext context = newContext(PROJECT_BASIC);

    Path testDir = context.getWorkspacePath().resolve("ln-test-force-symlink");
    context.getFileAccess().mkdirs(testDir);
    context.setCwd(testDir, context.getWorkspaceName(), context.getIdeHome());

    Path source1 = testDir.resolve("source1.txt");
    Path source2 = testDir.resolve("source2.txt");
    Path link = testDir.resolve("link.txt");
    Files.writeString(source1, "A", StandardCharsets.UTF_8);
    Files.writeString(source2, "B", StandardCharsets.UTF_8);

    // Create initial symbolic link to source1
    LnCommandlet cmd1 = new LnCommandlet(context);
    cmd1.symbolic.setValue(Boolean.TRUE);
    cmd1.source.setValue("source1.txt");
    cmd1.link.setValue("link.txt");
    cmd1.run();

    assertThat(link).exists();
    assertThat(Files.readString(link, StandardCharsets.UTF_8)).isEqualTo("A");

    // Try to create link to source2 without -f should fail
    LnCommandlet cmd2 = new LnCommandlet(context);
    cmd2.symbolic.setValue(Boolean.TRUE);
    cmd2.source.setValue("source2.txt");
    cmd2.link.setValue("link.txt");

    assertThatThrownBy(cmd2::run).isInstanceOf(RuntimeException.class);

    // Now try with -f flag should succeed
    LnCommandlet cmd3 = new LnCommandlet(context);
    cmd3.symbolic.setValue(Boolean.TRUE);
    cmd3.force.setValue(Boolean.TRUE);
    cmd3.source.setValue("source2.txt");
    cmd3.link.setValue("link.txt");

    cmd3.run();

    assertThat(link).exists();
    assertThat(Files.readString(link, StandardCharsets.UTF_8)).isEqualTo("B");
  }

  /**
   * Tests that -f (force) flag allows overriding existing hard links.
   */
  @Test
  void testForceOverridesExistingHardLink() throws Exception {
    IdeTestContext context = newContext(PROJECT_BASIC);

    Path testDir = context.getWorkspacePath().resolve("ln-test-force-hardlink");
    context.getFileAccess().mkdirs(testDir);
    context.setCwd(testDir, context.getWorkspaceName(), context.getIdeHome());

    Path source1 = testDir.resolve("source1.txt");
    Path source2 = testDir.resolve("source2.txt");
    Path link = testDir.resolve("link.txt");
    Files.writeString(source1, "A", StandardCharsets.UTF_8);
    Files.writeString(source2, "B", StandardCharsets.UTF_8);

    // Create initial hard link to source1
    LnCommandlet cmd1 = new LnCommandlet(context);
    cmd1.source.setValue("source1.txt");
    cmd1.link.setValue("link.txt");
    cmd1.run();

    assertThat(link).exists();
    assertThat(Files.readString(link, StandardCharsets.UTF_8)).isEqualTo("A");

    // Override with -f flag
    LnCommandlet cmd2 = new LnCommandlet(context);
    cmd2.force.setValue(Boolean.TRUE);
    cmd2.source.setValue("source2.txt");
    cmd2.link.setValue("link.txt");

    cmd2.run();

    assertThat(link).exists();
    assertThat(Files.readString(link, StandardCharsets.UTF_8)).isEqualTo("B");
  }
}
