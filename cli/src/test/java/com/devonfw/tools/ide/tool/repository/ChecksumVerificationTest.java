package com.devonfw.tools.ide.tool.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.url.model.file.UrlGenericChecksum;

/**
 * Test of checksum verification in {@link AbstractToolRepository}.
 */
public class ChecksumVerificationTest extends AbstractIdeContextTest {

  @TempDir
  Path tempDir;

  /**
   * Test {@link AbstractToolRepository#verifyChecksum(Path, UrlGenericChecksum)} with matching checksum.
   *
   * @throws IOException if an I/O error occurs.
   */
  @Test
  public void testVerifyChecksumMatching() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    AbstractToolRepository repo = new DefaultToolRepository(context);
    Path file = this.tempDir.resolve("testfile.txt");
    String content = "Hello World";
    Files.writeString(file, content);
    String checksum = context.getFileAccess().checksum(file, "SHA-256");
    UrlGenericChecksum expectedChecksum = new TestUrlGenericChecksum(checksum, "SHA-256");

    // act
    repo.verifyChecksum(file, expectedChecksum);

    // assert (no exception thrown)
  }

  /**
   * Test {@link AbstractToolRepository#verifyChecksum(Path, UrlGenericChecksum)} with mismatching checksum.
   *
   * @throws IOException if an I/O error occurs.
   */
  @Test
  public void testVerifyChecksumMismatch() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    AbstractToolRepository repo = new DefaultToolRepository(context);
    Path file = this.tempDir.resolve("testfile.txt");
    String content = "Hello World";
    Files.writeString(file, content);
    String wrongChecksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"; // SHA-256 of empty string
    UrlGenericChecksum expectedChecksum = new TestUrlGenericChecksum(wrongChecksum, "SHA-256");

    // act & assert
    try {
      repo.verifyChecksum(file, expectedChecksum);
      fail("Exception expected");
    } catch (CliException e) {
      assertThat(e).hasMessageContaining("has the wrong SHA-256 checksum");
      assertThat(e).hasMessageContaining("Expected " + wrongChecksum);
    }
  }

  private static class TestUrlGenericChecksum implements UrlGenericChecksum {

    private final String checksum;

    private final String algorithm;

    public TestUrlGenericChecksum(String checksum, String algorithm) {

      this.checksum = checksum;
      this.algorithm = algorithm;
    }

    @Override
    public String getChecksum() {

      return this.checksum;
    }

    @Override
    public String getHashAlgorithm() {

      return this.algorithm;
    }

    @Override
    public String toString() {

      return this.checksum;
    }
  }
}
