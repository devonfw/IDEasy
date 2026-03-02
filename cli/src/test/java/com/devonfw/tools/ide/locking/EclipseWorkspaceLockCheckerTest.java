package com.devonfw.tools.ide.locking;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link EclipseWorkspaceLockChecker}.
 */
class EclipseWorkspaceLockCheckerTest extends Assertions {

  /**
   * Test of {@link EclipseWorkspaceLockChecker#isLocked(File)} for an unlocked file.
   */
  @Test
  void testIsLockedUnlockedFile() throws IOException {
    File tempFile = File.createTempFile("eclipse-lock-test", ".lock");
    try {
      assertThat(EclipseWorkspaceLockChecker.isLocked(tempFile)).isFalse();
    } finally {
      tempFile.delete();
    }
  }

  /**
   * Test of {@link EclipseWorkspaceLockChecker#isLocked(File)} for a locked file.
   */
  @Test
  void testIsLockedLockedFile() throws IOException {
    File tempFile = File.createTempFile("eclipse-lock-test", ".lock");
    try (RandomAccessFile raFile = new RandomAccessFile(tempFile, "rw");
        FileLock lock = raFile.getChannel().lock()) {
      assertThat(EclipseWorkspaceLockChecker.isLocked(tempFile)).isTrue();
    } finally {
      tempFile.delete();
    }
  }

  /**
   * Test of {@link EclipseWorkspaceLockChecker#isLocked(File)} for a non-existent file.
   */
  @Test
  void testIsLockedNonExistentFile() {
    File file = new File("non-existent-file.lock");
    assertThat(EclipseWorkspaceLockChecker.isLocked(file)).isFalse();
  }
}

