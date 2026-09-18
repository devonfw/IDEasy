package com.devonfw.tools.ide.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;

import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoImpl;

/**
 * Helper utility for symlink-related tests on Windows. Provides methods to detect symlink capabilities and skip tests gracefully when the required permissions
 * are not available.
 */
public class WindowsSymlinkTestHelper {

  private static final SystemInfo SYSTEM_INFO = SystemInfoImpl.INSTANCE;

  private static Boolean canCreateSymlinks = null;

  /**
   * Standard message explaining why symlink tests are skipped on Windows.
   */
  private static final String SKIP_MESSAGE = "Skipping test: Creating symbolic links requires administrator privileges or Developer Mode on Windows. "
      + "See documentation/symlink.adoc or https://github.com/devonfw/IDEasy/blob/main/documentation/symlink.adoc for details on enabling symlink permissions.";

  /**
   * Checks if the current environment supports creating symbolic links. On Windows, this requires either administrator privileges or Developer Mode to be
   * enabled. On other operating systems, this typically returns {@code true}.
   *
   * @return {@code true} if symbolic links can be created, {@code false} otherwise.
   */
  public static boolean canCreateSymlinks() {

    if (!SYSTEM_INFO.isWindows()) {
      return true; // Non-Windows systems typically allow symlinks
    }

    if (canCreateSymlinks != null) {
      return canCreateSymlinks; // Return cached result
    }

    // Try to create a test symlink to check permissions
    try {
      Path tempDir = Files.createTempDirectory("ideasy-symlink-test");
      try {
        Path testFile = Files.createFile(tempDir.resolve("test-file.txt"));
        Path testLink = tempDir.resolve("test-link");
        Files.createSymbolicLink(testLink, testFile);
        canCreateSymlinks = true;
        // Clean up
        Files.deleteIfExists(testLink);
        Files.deleteIfExists(testFile);
      } finally {
        Files.deleteIfExists(tempDir);
      }
    } catch (IOException | UnsupportedOperationException | SecurityException e) {
      canCreateSymlinks = false;
    }

    return canCreateSymlinks;
  }

  /**
   * Assumes that symbolic links can be created in the current environment. If not, the calling test will be skipped with a standard message explaining the
   * requirement.
   * <p>
   * Use this method at the beginning of tests that require symbolic link creation to ensure they fail gracefully on Windows systems without proper
   * permissions.
   * <p>
   * Example usage:
   *
   * <pre>
   * &#64;Test
   * void testSymlinkFeature(&#64;TempDir Path tempDir) {
   *   WindowsSymlinkTestHelper.assumeSymlinksSupported();
   *   // ... test code that creates symlinks ...
   * }
   * </pre>
   */
  public static void assumeSymlinksSupported() {

    Assumptions.assumeTrue(canCreateSymlinks(), SKIP_MESSAGE);
  }

  /**
   * Creates a directory link from {@code link} to {@code target}. On systems that allow symbolic links a symbolic link is created, otherwise (e.g. Windows
   * without administrator privileges or Developer Mode) a directory junction is used instead, which requires no special privileges and, like a symlink,
   * is resolved by {@link java.nio.file.Path#toRealPath()}.
   *
   * @param link the path of the link to create.
   * @param target the existing directory the link points to.
   * @throws IOException if the link cannot be created.
   */
  public static void createDirectoryLink(Path link, Path target) throws IOException {

    Files.createDirectories(link.getParent());
    Files.deleteIfExists(link);
    if (canCreateSymlinks()) {
      Files.createSymbolicLink(link, target);
    } else {
      Process process = new ProcessBuilder("cmd.exe", "/c", "mklink", "/J", link.toString(), target.toString())
          .redirectErrorStream(true).start();
      String output;
      try {
        output = new String(process.getInputStream().readAllBytes());
      } catch (IOException e) {
        throw new IOException("Failed to read output of mklink /J", e);
      }
      int exitCode;
      try {
        exitCode = process.waitFor();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while creating directory junction " + link + " -> " + target, e);
      }
      if (exitCode != 0) {
        throw new IOException("Failed to create directory junction " + link + " -> " + target + ": " + output.trim());
      }
    }
  }

  /**
   * Resets the cached symlink capability check. This is primarily useful for testing purposes.
   */
  static void resetCache() {

    canCreateSymlinks = null;
  }
}
