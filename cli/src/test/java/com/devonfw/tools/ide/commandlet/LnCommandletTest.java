package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoImpl;

/**
 * Test of {@link LnCommandlet}.
 */
class LnCommandletTest extends AbstractIdeContextTest {

  /**
   * Tests link creation for both default hard links and symbolic links created with -s.
   */
  @Test
  void testLnCreatesLinkAndReflectsChanges() throws IOException {

    // default hard link
    IdeTestContext context = newContext(PROJECT_BASIC);

    Path testDir = context.getWorkspacePath().resolve("ln-test-hard-or-symlink");
    context.getFileAccess().mkdirs(testDir);
    context.setCwd(testDir, context.getWorkspaceName(), context.getIdeHome());

    Path source = testDir.resolve("source.txt");
    Path link = testDir.resolve("link.txt");
    Files.writeString(source, "A", StandardCharsets.UTF_8);

    LnCommandlet cmd = new LnCommandlet(context);
    cmd.symbolic.setValue(Boolean.TRUE);
    cmd.relative.setValue(Boolean.TRUE);
    cmd.source.setValue(Path.of("source.txt"));
    cmd.link.setValue(Path.of("link.txt"));

    cmd.run();

    assertThat(link).exists();

    Files.writeString(source, "B", StandardCharsets.UTF_8);
    assertThat(Files.readString(link, StandardCharsets.UTF_8)).isEqualTo("B");

    assertThat(link).exists();
    if (SystemInfoImpl.INSTANCE.isMac() || SystemInfoImpl.INSTANCE.isLinux()) {
      assertThat(link).isSymbolicLink();
      Path linkTarget = Files.readSymbolicLink(link);
      assertThat(linkTarget.isAbsolute()).isFalse();
      assertThat(linkTarget.toString()).isEqualTo("source.txt");
    }
  }

  /**
   * Tests replacing an existing hard link and an existing symbolic link.
   */
  @Test
  void testLnReplacesExistingLink() throws IOException {

    IdeTestContext context = newContext(PROJECT_BASIC);

    Path testDir = context.getWorkspacePath().resolve("ln-test-replace-link");
    context.getFileAccess().mkdirs(testDir);
    context.setCwd(testDir, context.getWorkspaceName(), context.getIdeHome());

    Path source1 = testDir.resolve("source1.txt");
    Path source2 = testDir.resolve("source2.txt");
    Path link = testDir.resolve("link.txt");
    Files.writeString(source1, "A", StandardCharsets.UTF_8);
    Files.writeString(source2, "B", StandardCharsets.UTF_8);

    LnCommandlet cmd1 = new LnCommandlet(context);
    cmd1.source.setValue(Path.of("source1.txt"));
    cmd1.symbolic.setValue(Boolean.TRUE);
    cmd1.relative.setValue(Boolean.TRUE);
    cmd1.link.setValue(Path.of("link.txt"));
    cmd1.run();

    assertThat(link).exists();
    assertThat(Files.readString(link, StandardCharsets.UTF_8)).isEqualTo("A");

    LnCommandlet cmd2 = new LnCommandlet(context);
    cmd2.source.setValue(Path.of("source2.txt"));
    cmd2.symbolic.setValue(Boolean.TRUE);
    cmd2.relative.setValue(Boolean.TRUE);
    cmd2.link.setValue(Path.of("link.txt"));
    cmd2.run();

    assertThat(link).exists();
    assertThat(Files.readString(link, StandardCharsets.UTF_8)).isEqualTo("B");

    if (SystemInfoImpl.INSTANCE.isMac() || SystemInfoImpl.INSTANCE.isLinux()) {
      assertThat(link).isSymbolicLink();
      assertThat(Files.readSymbolicLink(link).toString()).isEqualTo("source2.txt");
    }
  }
}
