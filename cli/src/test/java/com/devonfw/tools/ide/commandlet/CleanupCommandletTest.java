package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.cleanup.CleanupCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.WindowsSymlinkTestHelper;

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
