package com.devonfw.tools.ide.tool;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.tool.repository.ToolRepository;

/**
 * Test of {@link LocalToolCommandlet}.
 */
class LocalToolCommandletTest extends AbstractIdeContextTest {

  static final String JAVA_VERSION_FOR_INTELLIJ = "17.0.10_7";

  /**
   * Dummy commandlet extending {@link LocalToolCommandlet} for testing.
   */
  public static class LocalToolDummyCommandlet extends LocalToolCommandlet {

    LocalToolDummyCommandlet(IdeContext context) {

      super(context, "dummy", Set.of(Tag.TEST));
    }
  }

  /**
   * Test {@link LocalToolCommandlet#getValidInstalledSoftwareRepoPath(Path, Path)} with a long installation path, as commonly encountered on macOS systems.
   */
  @Test
  void testGetValidInstalledSoftwareRepoPathWithLongPath() {
    // arrange
    Path installPath = Path.of("/projects/_ide/software/default/java/java/21.0.7_6/Contents/Resources/app/");
    Path softwareRepoPath = Path.of("/projects/_ide/software/");
    Path expectedResultPath = Path.of("/projects/_ide/software/default/java/java/21.0.7_6/");
    IdeTestContext context = newContext(PROJECT_BASIC);
    LocalToolDummyCommandlet localToolCommandlet = new LocalToolDummyCommandlet(context);

    // act
    Path resultPath = localToolCommandlet.getValidInstalledSoftwareRepoPath(installPath, softwareRepoPath);

    // assert
    assertThat(resultPath).isEqualTo(expectedResultPath);
  }

  /**
   * Test {@link LocalToolCommandlet#getValidInstalledSoftwareRepoPath(Path, Path)} with a valid installation path.
   */
  @Test
  void testGetValidInstalledSoftwareRepoPathWithValidPath() {
    // arrange
    Path installPath = Path.of("/projects/_ide/software/default/java/java/21.0.7_6/");
    Path softwareRepoPath = Path.of("/projects/_ide/software/");
    IdeTestContext context = newContext(PROJECT_BASIC);
    LocalToolDummyCommandlet localToolCommandlet = new LocalToolDummyCommandlet(context);

    // act
    Path resultPath = localToolCommandlet.getValidInstalledSoftwareRepoPath(installPath, softwareRepoPath);

    // assert
    assertThat(resultPath).isEqualTo(installPath);
  }

  /**
   * Test {@link LocalToolCommandlet#getValidInstalledSoftwareRepoPath(Path, Path)} with an installation path that is too short.
   */
  @Test
  void testGetValidInstalledSoftwareRepoPathWithShortPath() {
    // arrange
    Path installPath = Path.of("/projects/_ide/software/default/java/java/");
    Path softwareRepoPath = Path.of("/projects/_ide/software/");
    IdeTestContext context = newContext(PROJECT_BASIC);
    LocalToolDummyCommandlet localToolCommandlet = new LocalToolDummyCommandlet(context);

    // act
    Path resultPath = localToolCommandlet.getValidInstalledSoftwareRepoPath(installPath, softwareRepoPath);

    // assert
    assertThat(resultPath).isNull();
    assertThat(context).log().hasEntries(IdeLogEntry.ofWarning("The installation path is faulty " + installPath + "."));
  }

  /**
   * Test {@link LocalToolCommandlet#getValidInstalledSoftwareRepoPath(Path, Path)} with an installation path that completely differs from the software
   * repository path.
   */
  @Test
  void testGetValidInstalledSoftwareRepoPathWithWrongPath() {
    // arrange
    Path installPath = Path.of("/projects/IDEasy/workspaces/main/IDEasy/software/java/");
    Path softwareRepoPath = Path.of("/projects/_ide/software/");
    IdeTestContext context = newContext(PROJECT_BASIC);
    LocalToolDummyCommandlet localToolCommandlet = new LocalToolDummyCommandlet(context);

    // act
    Path resultPath = localToolCommandlet.getValidInstalledSoftwareRepoPath(installPath, softwareRepoPath);

    // assert
    assertThat(resultPath).isNull();
    assertThat(context).log().hasEntries(IdeLogEntry.ofWarning("The installation path is not located within the software repository " + installPath + "."));
  }

  /**
   * Test that {@link LocalToolCommandlet#run()} will ensure that a dependent tool is used in the correct version even if that version is not compatible with
   * the project and has been removed after initial installation.
   */
  @Test
  void testRunToolWithDependencies() {
    // arrange
    IdeTestContext context = newContext("dependencies");
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);

    // run intellij and ensure it gets installed together with the compatible Java version
    runIntellijAndCheckInstallationWithJavaDependency(context);
    // run intellij again and ensure the correct Java
    runIntellijAndCheckInstallationWithJavaDependency(context);
    // delete Java installation from software repository
    Path java = findJava4Intellij(context);
    context.getFileAccess().delete(java);
    // run intellij again and verify that compatible Java version gets reinstalled
    runIntellijAndCheckInstallationWithJavaDependency(context);
  }

  private static void runIntellijAndCheckInstallationWithJavaDependency(IdeTestContext context) {
    // arrange
    context.getLogger().getEntries().clear(); // clear logs from previous run(s)
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    // act
    intellij.run();
    // assert
    assertThat(context.getSoftwarePath().resolve("intellij").resolve(IdeContext.FILE_SOFTWARE_VERSION)).hasContent("2023.3.3");
    assertThat(context.getSoftwarePath().resolve("java")).doesNotExist();
    Path javaDependency = findJava4Intellij(context);
    assertThat(javaDependency.resolve(IdeContext.FILE_SOFTWARE_VERSION)).hasContent(JAVA_VERSION_FOR_INTELLIJ);
    assertThat(context).logAtInfo().hasMessage("Intellij using Java from " + javaDependency);
  }

  private static Path findJava4Intellij(IdeContext context) {
    return context.getSoftwareRepositoryPath().resolve(ToolRepository.ID_DEFAULT).resolve("java").resolve("java").resolve(JAVA_VERSION_FOR_INTELLIJ);
  }
}
