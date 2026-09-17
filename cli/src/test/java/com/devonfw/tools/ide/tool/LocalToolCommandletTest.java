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
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.BoundaryType;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link LocalToolCommandlet}.
 */
@WireMockTest
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
   * Dummy commandlet extending {@link LocalToolCommandlet} that tolerates a missing software version file (see
   * {@link LocalToolCommandlet#isIgnoreMissingSoftwareVersionFile()}) and can determine its installed version from the installation itself (like python does).
   * The repository lookups and the actual installation performed by {@link LocalToolCommandlet#installTool(ToolInstallRequest)} are short-circuited so the
   * relevant branches can be tested in isolation without requiring the tool to be registered in the tool repository.
   */
  public static class LocalToolRestoreVersionDummyCommandlet extends LocalToolDummyCommandlet {

    /** The version the dummy determines from the installation itself or {@code null} if it cannot determine it. */
    private final VersionIdentifier computedVersion;

    LocalToolRestoreVersionDummyCommandlet(IdeContext context) {
      this(context, VersionIdentifier.of("9.9.9"));
    }

    LocalToolRestoreVersionDummyCommandlet(IdeContext context, VersionIdentifier computedVersion) {
      super(context);
      this.computedVersion = computedVersion;
    }

    @Override
    protected boolean isIgnoreSoftwareRepo() {

      // like package-manager based tools (e.g. python) the installation lives directly in the software folder
      return true;
    }

    @Override
    protected boolean isIgnoreMissingSoftwareVersionFile() {

      return true;
    }

    @Override
    protected VersionIdentifier cveCheck(ToolInstallRequest request) {

      return request.getRequested().getResolvedVersion();
    }

    @Override
    protected void installToolDependencies(ToolInstallRequest request) {

      // the dummy tool has no dependencies
    }

    /**
     * @return the installed version of the dummy tool determined from the installation itself, or {@code null} if it cannot be determined.
     */
    @Override
    protected VersionIdentifier computeInstalledVersionFromLocalSoftwareFolder() {

      // as if the version was determined from the installation itself (e.g. by running the installed tool)
      return this.computedVersion;
    }

    @Override
    protected void performToolInstallation(ToolInstallRequest request, Path installationPath) {

      // simulate a fresh installation without triggering a real download
      context.writeVersionFile(request.getRequested().getResolvedVersion(), installationPath);
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

  /**
   * Test that {@link LocalToolCommandlet#installAsDependency(VersionRange, ToolInstallRequest)} ignores the configured project version when
   * {@link ToolInstallRequest#isIgnoreProject()} is {@code true}.
   */
  @Test
  void testInstallAsDependencyIgnoresProject(WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    IdeTestContext context = newContext("dependencies", wmRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    Java javaTool = context.getCommandletManager().getCommandlet(Java.class);

    ToolInstallRequest parentRequest = new ToolInstallRequest(false);
    parentRequest.setIgnoreProject(true);
    parentRequest.setRequested(new ToolEditionAndVersion(new ToolEdition("intellij", null), VersionIdentifier.of("2023.3.3")));
    VersionRange versionRange = VersionRange.of(VersionIdentifier.of("17"), VersionIdentifier.of("25.0.9"), BoundaryType.CLOSED);

    // act
    ToolInstallation installation = javaTool.installAsDependency(versionRange, parentRequest);

    // assert - a Java version within the range should have been installed in the software repository
    assertThat(installation.newInstallation()).isTrue();
    assertThat(installation.rootDir()).isEqualTo(
        context.getSoftwareRepositoryPath().resolve(ToolRepository.ID_DEFAULT).resolve("java").resolve("java").resolve(JAVA_VERSION_FOR_INTELLIJ));
    // assert - no project symlink should have been created because ignoreProject=true
    assertThat(context.getSoftwarePath().resolve("java")).doesNotExist();
    // assert - the debug log must confirm that the project was ignored
    assertThat(context).logAtDebug().hasMessageContaining("Ignoring project for dependency");
  }

  /**
   * Verifies that {@link LocalToolCommandlet#installTool(ToolInstallRequest)} re-installs an existing installation when the software version file is missing
   * and the installed version cannot be determined.
   * <p>
   * The dummy tool cannot recover its installed version from the installation itself ({@code computeInstalledVersionFromLocalSoftwareFolder()} returns
   * {@code null}). Since the version remains unknown, the installation isconsidered broken and must be re-installed instead of being preserved.
   */
  @Test
  void testInstallToolReinstallsWhenInstalledVersionNotDetermined() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    LocalToolRestoreVersionDummyCommandlet commandlet = new LocalToolRestoreVersionDummyCommandlet(context, null);
    // an existing installation without a software version file for a tool that cannot determine its installed version
    Path installationPath = context.getSoftwarePath().resolve("dummy");
    context.getFileAccess().mkdirs(installationPath);
    context.getFileAccess().writeFileContent("tool content", installationPath.resolve("somefile.txt"));
    Path versionFile = installationPath.resolve(IdeContext.FILE_SOFTWARE_VERSION);
    assertThat(versionFile).doesNotExist();
    VersionIdentifier resolvedVersion = VersionIdentifier.of("1.2.3");
    ToolInstallRequest request = new ToolInstallRequest(false);
    request.setRequested(new ToolEditionAndVersion(new ToolEdition("dummy", "dummy"), resolvedVersion));

    // act
    ToolInstallation installation = commandlet.installTool(request);

    // assert - the broken installation was re-installed (the dummy performToolInstallation writes the version file)
    assertThat(installation.rootDir()).isEqualTo(installationPath);
    assertThat(installation.newInstallation()).isTrue();
    assertThat(installation.resolvedVersion()).isEqualTo(resolvedVersion);
    assertThat(versionFile).exists().hasContent("1.2.3");
    // assert - the tool was treated as broken (reinstalled), not as already installed and not as corrupted and deleted
    assertThat(context).logAtWarning().hasMessageContaining("considered broken and will be reinstalled");
    assertThat(context).logAtWarning().hasNoMessageContaining("Deleting corrupted installation");
  }

  /**
   * Verifies that a missing software version file is restored when the installed version can be determined from the installation itself.
   * <p>
   * The tool reports its actual installed version via {@link LocalToolCommandlet#computeInstalledVersionFromLocalSoftwareFolder()}. That version is written
   * back to the missing version file and the existing installation is kept if it matches the requested version.
   */
  @Test
  void testGetInstalledEditionAndVersionRestoresMissingVersionFile() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    LocalToolRestoreVersionDummyCommandlet commandlet = new LocalToolRestoreVersionDummyCommandlet(context);
    Path installationPath = context.getSoftwarePath().resolve("dummy");
    context.getFileAccess().mkdirs(installationPath);
    Path versionFile = installationPath.resolve(IdeContext.FILE_SOFTWARE_VERSION);
    assertThat(versionFile).doesNotExist();

    // act - determine the installed edition and version with the version file missing
    EditionAndVersion installed = commandlet.getInstalledEditionAndVersion();

    // assert - the version was determined from the installation itself
    assertThat(installed).isNotNull();
    assertThat(installed.version()).isEqualTo(VersionIdentifier.of("9.9.9"));
    // assert - the missing version file was restored with the determined (actual) version
    assertThat(versionFile).exists().hasContent("9.9.9");
    assertThat(context).logAtWarning().hasMessageContaining("restoring it with version 9.9.9");
  }

  /**
   * Verifies that a missing software version file does not trigger a reinstallation when the installed version can be determined and matches the requested
   * version.
   * <p>
   * The version file is restored with the detected installed version and the existing installation is preserved.
   */
  @Test
  void testInstallToolKeepsInstallationWhenInstalledVersionIsRestored() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    LocalToolRestoreVersionDummyCommandlet commandlet = new LocalToolRestoreVersionDummyCommandlet(context);
    // an existing installation without a software version file; the tool determines its installed version from the installation itself (9.9.9)
    Path installationPath = context.getSoftwarePath().resolve("dummy");
    context.getFileAccess().mkdirs(installationPath);
    context.getFileAccess().writeFileContent("tool content", installationPath.resolve("somefile.txt"));
    Path versionFile = installationPath.resolve(IdeContext.FILE_SOFTWARE_VERSION);
    assertThat(versionFile).doesNotExist();
    ToolInstallRequest request = new ToolInstallRequest(false);
    request.setRequested(new ToolEditionAndVersion(new ToolEdition("dummy", "dummy"), VersionIdentifier.of("9.9.9")));

    // act
    ToolInstallation installation = commandlet.installTool(request);

    // assert - the version file was restored with the installed version determined from the installation itself
    assertThat(versionFile).exists().hasContent("9.9.9");
    // assert - the installation was kept (not reinstalled, because installed version matches the requested version)
    assertThat(installation.rootDir()).isEqualTo(installationPath);
    assertThat(installation.newInstallation()).isFalse();
    assertThat(installation.resolvedVersion()).isEqualTo(VersionIdentifier.of("9.9.9"));
    assertThat(installationPath.resolve("somefile.txt")).exists();
    // assert - the tool was not treated as broken and reinstalled
    assertThat(context).logAtWarning().hasMessageContaining("restoring it with version 9.9.9");
    assertThat(context).logAtWarning().hasNoMessageContaining("considered broken and will be reinstalled");
    assertThat(context).logAtWarning().hasNoMessageContaining("Deleting corrupted installation");
  }

  private static void runIntellijAndCheckInstallationWithJavaDependency(IdeTestContext context) {
    // arrange
    context.getTestStartContext().getEntries().clear(); // clear logs from previous run(s)
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
