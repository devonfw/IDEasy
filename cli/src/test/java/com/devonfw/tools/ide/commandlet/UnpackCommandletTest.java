package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link UnpackCommandlet}.
 */
class UnpackCommandletTest extends AbstractIdeContextTest {

  /** Base filename of the test archive without extension. */
  private static final String TEST_ARCHIVE_BASENAME = "executable_and_non_executable";

  /** Path to the test archive directory. */
  private static final Path TEST_ARCHIVE_DIR = Path.of("src/test/resources/com/devonfw/tools/ide/io");

  /** Test ZIP archive. */
  private static final Path TEST_ARCHIVE_ZIP = TEST_ARCHIVE_DIR.resolve(TEST_ARCHIVE_BASENAME + ".zip");

  /** Test TAR.GZ archive. */
  private static final Path TEST_ARCHIVE_TAR_GZ = TEST_ARCHIVE_DIR.resolve(TEST_ARCHIVE_BASENAME + ".tar.gz");

  /** Test 7Z archive. */
  private static final Path TEST_ARCHIVE_7Z = TEST_ARCHIVE_DIR.resolve(TEST_ARCHIVE_BASENAME + ".7z");

  /**
   * Tests extraction of a ZIP archive to the default target directory derived from the archive filename.
   */
  @Test
  void testUnpackZipWithDefaultTarget() throws IOException {

    IdeTestContext context = newContext(PROJECT_BASIC);

    Path archive = TEST_ARCHIVE_ZIP.toAbsolutePath();
    UnpackCommandlet cmd = new UnpackCommandlet(context);
    cmd.archive.setValue(archive);

    cmd.run();

    Path expectedTarget = context.getCwd().resolve(TEST_ARCHIVE_BASENAME);
    assertThat(expectedTarget).isDirectory();
    assertThat(expectedTarget.resolve("executableFile.txt")).isRegularFile();
    assertThat(expectedTarget.resolve("nonExecutableFile.txt")).isRegularFile();
  }

  /**
   * Tests extraction of a ZIP archive to an explicit target directory via --target.
   */
  @Test
  void testUnpackZipWithExplicitTarget() throws IOException {

    IdeTestContext context = newContext(PROJECT_BASIC);

    Path testDir = context.getWorkspacePath().resolve("unpack-test");
    context.getFileAccess().mkdirs(testDir);
    context.setCwd(testDir, context.getWorkspaceName(), context.getIdeHome());

    Path archive = TEST_ARCHIVE_ZIP.toAbsolutePath();
    Path target = testDir.resolve("my-extraction");

    UnpackCommandlet cmd = new UnpackCommandlet(context);
    cmd.archive.setValue(archive);
    cmd.target.setValue(target);

    cmd.run();

    assertThat(target).isDirectory();
    assertThat(target.resolve("executableFile.txt")).isRegularFile();
    assertThat(target.resolve("nonExecutableFile.txt")).isRegularFile();
  }

  /**
   * Tests that extracting a non-existing archive fails with an appropriate error.
   */
  @Test
  void testUnpackNonExistingArchiveFails() {

    IdeTestContext context = newContext(PROJECT_BASIC);

    UnpackCommandlet cmd = new UnpackCommandlet(context);
    cmd.archive.setValue(Path.of("does_not_exist.zip"));

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(cmd::run)
        .withMessageContaining("does_not_exist.zip")
        .withMessageContaining("Failed to extract");
  }

  /**
   * Tests extraction of a tar.gz archive with default target directory.
   */
  @Test
  void testUnpackTarGzWithDefaultTarget() throws IOException {

    IdeTestContext context = newContext(PROJECT_BASIC);

    Path archive = TEST_ARCHIVE_TAR_GZ.toAbsolutePath();
    UnpackCommandlet cmd = new UnpackCommandlet(context);
    cmd.archive.setValue(archive);

    cmd.run();

    Path expectedTarget = context.getCwd().resolve(TEST_ARCHIVE_BASENAME);
    assertThat(expectedTarget).isDirectory();
  }

  /**
   * Tests extraction of a 7z archive with default target directory.
   */
  @Test
  void testUnpack7zWithDefaultTarget() throws IOException {

    IdeTestContext context = newContext(PROJECT_BASIC);

    Path archive = TEST_ARCHIVE_7Z.toAbsolutePath();
    UnpackCommandlet cmd = new UnpackCommandlet(context);
    cmd.archive.setValue(archive);

    cmd.run();

    Path expectedTarget = context.getCwd().resolve(TEST_ARCHIVE_BASENAME);
    assertThat(expectedTarget).isDirectory();
  }

  /**
   * End-to-end test that a positional {@code target} argument on the command line ({@code ide unpack <archive> <target>})
   * is bound to the target property and the archive is extracted into the given directory.
   * <p>
   * The {@code target} property has no long option (it is a positional value argument); this guards that the positional
   * binding works end-to-end, which the property-level tests above (which set the property directly) cannot verify.
   * </p>
   */
  @Test
  void testUnpackWithPositionalTarget() throws IOException {

    IdeTestContext context = newContext(PROJECT_BASIC);

    Path archive = TEST_ARCHIVE_ZIP.toAbsolutePath();
    Path target = context.getCwd().resolve("e2e-unpack-target");
    CliArguments args = new CliArguments("unpack", archive.toString(), target.toString());
    args.next();

    int exitCode = context.run(args);

    assertThat(exitCode).isEqualTo(0);
    assertThat(context).logAtError().hasNoMessageContaining("Unknown command");
    assertThat(context).logAtError().hasNoMessageContaining("Invalid option");
    assertThat(context).logAtError().hasNoMessageContaining("No matching property");
    assertThat(target).isDirectory();
    assertThat(target.resolve("executableFile.txt")).isRegularFile();
    assertThat(target.resolve("nonExecutableFile.txt")).isRegularFile();
  }
}
