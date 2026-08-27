package com.devonfw.tools.ide.commandlet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.cleanup.CleanupCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.WindowsSymlinkTestHelper;
import com.devonfw.tools.ide.property.StringProperty;

/**
 * Test of {@link CleanupCommandlet}.
 */
class CleanupCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_BASIC = "basic";

  /**
   * Tests that unused software is deleted while software used by a project is retained.
   *
   * @throws IOException if the test setup cannot be created.
   */
  @Test
  void testCleanupDeletesUnusedAndKeepsUsedGlobalSoftware() throws IOException {

    WindowsSymlinkTestHelper.assumeSymlinksSupported();

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    Path usedVersion = createInstalledVersion(context, "default", "cleanup-test-java", "default", "21");

    Path unusedVersion = createInstalledVersion(context, "default", "cleanup-test-java", "default", "17");

    Path projectSoftware = context.getIdeHome().resolve(IdeContext.FOLDER_SOFTWARE);

    createSoftwareLink(projectSoftware.resolve("cleanup-test-java"), usedVersion);

    CleanupCommandlet cleanup = getCleanupWithConfirmation(context);

    // act
    cleanup.run();

    // assert
    assertThat(usedVersion).as("Software used by a project must not be deleted").exists();

    assertThat(unusedVersion).as("Unused software should be deleted").doesNotExist();

    assertThat(context).logAtSuccess().hasMessage("Unused tools have been deleted successfully.");
  }

  /**
   * Tests that a version is retained when a project links to a subdirectory of the installation, for example {@code <version>/Contents/MacOS}.
   *
   * @throws IOException if the test setup cannot be created.
   */
  @Test
  void testCleanupKeepsVersionReferencedBySubdirectory() throws IOException {

    WindowsSymlinkTestHelper.assumeSymlinksSupported();

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    Path usedVersion = createInstalledVersion(context, "default", "cleanup-test-macos", "default", "1.0");

    Path macOsFolder = usedVersion.resolve("Contents").resolve("MacOS");
    Files.createDirectories(macOsFolder);

    Path unusedVersion = createInstalledVersion(context, "default", "cleanup-test-macos", "default", "2.0");

    Path projectSoftware = context.getIdeHome().resolve(IdeContext.FOLDER_SOFTWARE);

    createSoftwareLink(projectSoftware.resolve("cleanup-test-macos"), macOsFolder);

    CleanupCommandlet cleanup = getCleanupWithConfirmation(context);

    // act
    cleanup.run();

    // assert
    assertThat(usedVersion)
        .as("The version must be retained when the project links to a subdirectory of that version")
        .exists();

    assertThat(unusedVersion).as("The unrelated unused version should be deleted").doesNotExist();
  }

  /**
   * Tests that a version referenced by {@code software/extra/<tool>/<name>} is retained.
   *
   * @throws IOException if the test setup cannot be created.
   */
  @Test
  void testCleanupKeepsVersionReferencedByNestedExtraTool() throws IOException {

    WindowsSymlinkTestHelper.assumeSymlinksSupported();

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    Path usedVersion = createInstalledVersion(context, "default", "cleanup-test-extra", "default", "1.0");

    Path unusedVersion = createInstalledVersion(context, "default", "cleanup-test-extra", "default", "2.0");

    Path nestedExtraToolLink = context.getIdeHome()
        .resolve(IdeContext.FOLDER_SOFTWARE)
        .resolve(IdeContext.FOLDER_EXTRA)
        .resolve("cleanup-test-extra")
        .resolve("custom-installation");

    createSoftwareLink(nestedExtraToolLink, usedVersion);

    CleanupCommandlet cleanup = getCleanupWithConfirmation(context);

    // act
    cleanup.run();

    // assert
    assertThat(usedVersion)
        .as("Software referenced by software/extra/<tool>/<name> must not be deleted")
        .exists();

    assertThat(unusedVersion).as("The unrelated unused extra-tool version should be deleted").doesNotExist();
  }

  /**
   * Tests that unused installations in the configured custom repository are discovered and deleted.
   *
   * @throws IOException if the test setup cannot be created.
   */
  @Test
  void testCleanupDeletesUnusedSoftwareFromCustomRepository() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    String customRepositoryId = context.getCustomToolRepository().getId();

    Path unusedVersion = createInstalledVersion(context, customRepositoryId, "cleanup-test-tool", "default", "1.0");

    CleanupCommandlet cleanup = getCleanupWithConfirmation(context);

    // act
    cleanup.run();

    // assert
    assertThat(unusedVersion)
        .as("Unused software from the configured custom repository should be discovered and deleted")
        .doesNotExist();
  }

  /**
   * Tests that batch mode combined with force mode skips the confirmation prompt.
   *
   * @throws IOException if the test setup cannot be created.
   */
  @Test
  void testCleanupSkipsConfirmationInBatchForceMode() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    Path unusedVersion = createInstalledVersion(
        context, "default", "cleanup-test-force", "default", "1.0");

    context.getStartContext().setBatchMode(true);
    context.getStartContext().setForceMode(true);

    CleanupCommandlet cleanup = context.getCommandletManager().getCommandlet(CleanupCommandlet.class);

    // act
    cleanup.run();

    // assert
    assertThat(unusedVersion)
        .as("Unused software should be deleted without an interactive confirmation in batch force mode")
        .doesNotExist();

    assertThat(context).logAtSuccess().hasMessage("Unused tools have been deleted successfully.");
  }

  /**
   * Tests that files older than the {@code --retention-delay} are deleted from the updates, {@code _ide/tmp} and {@code ~/Downloads/ide} folders
   * while newer files are kept.
   *
   * @throws IOException if the test setup cannot be created.
   */
  @Test
  void testCleanupDeletesStaleFilesOlderThanRetentionDelay() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Duration retentionDelay = Duration.ofDays(1);

    Path staleUpdatesFile = createStaleFile(context.getIdeHome().resolve(IdeContext.FOLDER_UPDATES).resolve("stale-updates.bin"),
        retentionDelay);
    Path staleTmpFile = createStaleFile(context.getTempPath().resolve("stale-tmp.bin"), retentionDelay);
    Path staleDownloadsFile = createStaleFile(
        context.getUserHome().resolve(IdeContext.FOLDER_DOWNLOADS).resolve("ide").resolve("stale-download.bin"), retentionDelay);

    Path freshUpdatesFile = createFreshFile(context.getIdeHome().resolve(IdeContext.FOLDER_UPDATES).resolve("fresh-updates.bin"),
        retentionDelay);
    Path freshTmpFile = createFreshFile(context.getTempPath().resolve("fresh-tmp.bin"), retentionDelay);
    Path freshDownloadsFile = createFreshFile(
        context.getUserHome().resolve(IdeContext.FOLDER_DOWNLOADS).resolve("ide").resolve("fresh-download.bin"), retentionDelay);

    context.setAnswers("yes");
    setRetentionDelay(context, "P1D");
    CleanupCommandlet cleanup = context.getCommandletManager().getCommandlet(CleanupCommandlet.class);

    // act
    cleanup.run();

    // assert
    assertThat(staleUpdatesFile).as("Stale file in updates should be deleted").doesNotExist();
    assertThat(staleTmpFile).as("Stale file in _ide/tmp should be deleted").doesNotExist();
    assertThat(staleDownloadsFile).as("Stale file in ~/Downloads/ide should be deleted").doesNotExist();

    assertThat(freshUpdatesFile).as("Fresh file in updates must not be deleted").exists();
    assertThat(freshTmpFile).as("Fresh file in _ide/tmp must not be deleted").exists();
    assertThat(freshDownloadsFile).as("Fresh file in ~/Downloads/ide must not be deleted").exists();

    assertThat(context).logAtSuccess().hasMessage("Stale files have been deleted successfully.");
  }

  /**
   * Tests that empty sub-folders below the scanned folders are removed after their stale contents have been deleted, while the scanned folders
   * themselves are kept.
   *
   * @throws IOException if the test setup cannot be created.
   */
  @Test
  void testCleanupRemovesEmptyFoldersAfterDeletingStaleFiles() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Duration retentionDelay = Duration.ofDays(1);

    Path subFolder = context.getIdeHome().resolve(IdeContext.FOLDER_UPDATES).resolve("nested").resolve("deep");
    Files.createDirectories(subFolder);
    Path staleFile = createStaleFile(subFolder.resolve("stale.bin"), retentionDelay);

    context.setAnswers("yes");
    setRetentionDelay(context, "P1D");
    CleanupCommandlet cleanup = context.getCommandletManager().getCommandlet(CleanupCommandlet.class);

    // act
    cleanup.run();

    // assert
    assertThat(staleFile).as("The stale file should be deleted").doesNotExist();
    assertThat(subFolder).as("The now-empty sub-folder should be removed").doesNotExist();
    Path updatesRoot = context.getIdeHome().resolve(IdeContext.FOLDER_UPDATES);
    assertThat(updatesRoot).as("The scanned root folder itself must not be removed").exists();
  }

  /**
   * Tests that an invalid {@code --retention-delay} value results in a {@link CliException}.
   */
  @Test
  void testCleanupRejectsInvalidRetentionDelay() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.setAnswers("yes");
    setRetentionDelay(context, "PT6M10D");
    CleanupCommandlet cleanup = context.getCommandletManager().getCommandlet(CleanupCommandlet.class);

    // act
    assertThatThrownBy(cleanup::run).isInstanceOf(CliException.class).hasMessageContaining("Invalid value 'PT6M10D' for --retention-delay");
  }

  /**
   * Tests that stale files are not deleted when the {@code --retention-delay} option is not provided, i.e. the default retention delay (1 year)
   * applies and the recently created test files are kept.
   *
   * @throws IOException if the test setup cannot be created.
   */
  @Test
  void testCleanupKeepsFilesWithinDefaultRetentionDelay() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Path recentFile = createFreshFile(context.getTempPath().resolve("recent.bin"), CleanupCommandlet.DEFAULT_RETENTION_DELAY);

    context.setAnswers("yes");
    CleanupCommandlet cleanup = context.getCommandletManager().getCommandlet(CleanupCommandlet.class);

    // act
    cleanup.run();

    // assert
    assertThat(recentFile).as("Files within the default retention delay must not be deleted").exists();
  }

  /**
   * Creates a file with a modification time older than the given retention delay.
   *
   * @param file the file to create.
   * @param retentionDelay the age the file must exceed.
   * @return the created file.
   * @throws IOException if the file cannot be created.
   */
  private Path createStaleFile(Path file, Duration retentionDelay) throws IOException {

    Files.createDirectories(file.getParent());
    Files.writeString(file, "stale content");
    Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() - retentionDelay.toMillis() - 1_000_000L));
    return file;
  }

  /**
   * Creates a file with a modification time newer than the given retention delay.
   *
   * @param file the file to create.
   * @param retentionDelay the age the file must not exceed.
   * @return the created file.
   * @throws IOException if the file cannot be created.
   */
  private Path createFreshFile(Path file, Duration retentionDelay) throws IOException {

    Files.createDirectories(file.getParent());
    Files.writeString(file, "fresh content");
    Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() - retentionDelay.toMillis() / 2));
    return file;
  }

  /**
   * Sets the value of the {@code --retention-delay} option of the cleanup commandlet.
   *
   * @param context the test context.
   * @param value the value to set.
   */
  private void setRetentionDelay(IdeTestContext context, String value) {

    ((StringProperty) context.getCommandletManager().getCommandlet(CleanupCommandlet.class).getOption("--retention-delay")).setValue(value);
  }

  /**
   * Creates an installed software version with the structure {@code _ide/software/<repository>/<tool>/<edition>/<version>}.
   *
   * @param context the test context.
   * @param repositoryId the repository ID.
   * @param toolName the tool name.
   * @param editionName the edition name.
   * @param versionName the version name.
   * @return the real path of the created version directory.
   * @throws IOException if the directory or version marker cannot be created.
   */
  private Path createInstalledVersion(IdeTestContext context, String repositoryId, String toolName,
      String editionName, String versionName) throws IOException {

    Path versionFolder = context.getSoftwareRepositoryPath()
        .resolve(repositoryId)
        .resolve(toolName)
        .resolve(editionName)
        .resolve(versionName);

    Files.createDirectories(versionFolder);

    Files.writeString(versionFolder.resolve(IdeContext.FILE_SOFTWARE_VERSION), versionName);

    return versionFolder.toRealPath();
  }

  /**
   * Creates a symbolic link from a project software location to an installed software version or one of its subdirectories.
   *
   * @param link the project-side link.
   * @param target the link target.
   * @throws IOException if the link cannot be created.
   */
  private void createSoftwareLink(Path link, Path target) throws IOException {

    Files.createDirectories(link.getParent());
    Files.deleteIfExists(link);
    Files.createSymbolicLink(link, target);
  }

  /**
   * Gets the cleanup commandlet and configures an affirmative confirmation answer.
   *
   * @param context the test context.
   * @return the configured cleanup commandlet.
   */
  private CleanupCommandlet getCleanupWithConfirmation(IdeTestContext context) {

    context.setAnswers("yes");
    return context.getCommandletManager().getCommandlet(CleanupCommandlet.class);
  }
}
