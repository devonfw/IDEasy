package com.devonfw.tools.ide.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of archive determinism in {@link FileAccessImpl}.
 */
public class ArchiveDeterminismTest extends AbstractIdeContextTest {

  @TempDir
  Path tempDir;

  /**
   * Test that {@link FileAccessImpl#compressTarGz(Path, OutputStream)} is deterministic.
   *
   * @throws IOException if an I/O error occurs.
   */
  @Test
  public void testTarGzDeterminism() throws IOException {

    // arrange
    IdeTestContext context = new IdeTestContext();
    FileAccessImpl fileAccess = new FileAccessImpl(context);
    Path contentDir = this.tempDir.resolve("content");
    Files.createDirectories(contentDir);
    Files.writeString(contentDir.resolve("file1.txt"), "Content 1");
    Path binDir = contentDir.resolve("bin");
    Files.createDirectories(binDir);
    Files.writeString(binDir.resolve("script.sh"), "#!/bin/bash\necho hello");

    Path archive1 = this.tempDir.resolve("archive1.tar.gz");
    Path archive2 = this.tempDir.resolve("archive2.tar.gz");

    // act
    try (OutputStream out1 = Files.newOutputStream(archive1)) {
      fileAccess.compressTarGz(contentDir, out1);
    }
    // Wait a bit to ensure a non-deterministic MTIME would change (though we zero it out)
    try {
      Thread.sleep(1100);
    } catch (InterruptedException e) {
      // ignore
    }
    try (OutputStream out2 = Files.newOutputStream(archive2)) {
      fileAccess.compressTarGz(contentDir, out2);
    }

    // assert
    assertThat(archive1).hasSameBinaryContentAs(archive2);
  }

  /**
   * Test that {@link FileAccessImpl#compressZip(Path, OutputStream)} is deterministic.
   *
   * @throws IOException if an I/O error occurs.
   */
  @Test
  public void testZipDeterminism() throws IOException {

    // arrange
    IdeTestContext context = new IdeTestContext();
    FileAccessImpl fileAccess = new FileAccessImpl(context);
    Path contentDir = this.tempDir.resolve("content-zip");
    Files.createDirectories(contentDir);
    Files.writeString(contentDir.resolve("file1.txt"), "Content 1");

    Path archive1 = this.tempDir.resolve("archive1.zip");
    Path archive2 = this.tempDir.resolve("archive2.zip");

    // act
    try (OutputStream out1 = Files.newOutputStream(archive1)) {
      fileAccess.compressZip(contentDir, out1);
    }
    // Wait a bit to ensure a non-deterministic time would change
    try {
      Thread.sleep(1100);
    } catch (InterruptedException e) {
      // ignore
    }
    try (OutputStream out2 = Files.newOutputStream(archive2)) {
      fileAccess.compressZip(contentDir, out2);
    }

    // assert
    assertThat(archive1).hasSameBinaryContentAs(archive2);
  }
}
