package com.devonfw.tools.ide.locking;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link EclipseWorkspaceLockChecker}.
 */
class EclipseWorkspaceLockCheckerTest extends Assertions {

  /**
   * Test of {@link EclipseWorkspaceLockChecker#isLocked(Path)} for an unlocked file.
   */
  @Test
  void testIsLockedUnlockedFile() throws IOException {
    Path tempFile = Files.createTempFile("eclipse-lock-test", ".lock");
    try {
      assertThat(EclipseWorkspaceLockChecker.isLocked(tempFile)).isFalse();
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  /**
   * Test of {@link EclipseWorkspaceLockChecker#isLocked(Path)} for a locked file.
   */
  @Test
  void testIsLockedLockedFile() throws IOException {
    Path tempFile = Files.createTempFile("eclipse-lock-test", ".lock");
    try (RandomAccessFile raFile = new RandomAccessFile(tempFile.toFile(), "rw");
        FileLock lock = raFile.getChannel().lock()) {
      assertThat(EclipseWorkspaceLockChecker.isLocked(tempFile)).isTrue();
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  /**
   * Test of {@link EclipseWorkspaceLockChecker#isLocked(Path)} for a non-existent file.
   */
  @Test
  void testIsLockedNonExistentFile() {
    Path file = Path.of("non-existent-file.lock");
    assertThat(EclipseWorkspaceLockChecker.isLocked(file)).isFalse();
  }
}
