package com.devonfw.tools.ide.tool.npm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.claude.RecordingEnvironmentContext;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Npm}.
 */
@WireMockTest
class NpmTest extends AbstractIdeContextTest {

  private static final String PROJECT_NPM = "npm";

  /**
   * Tests if the {@link Npm} install works correctly across all three operating systems.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testNpmInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, wireMockRuntimeInfo);
    Npm commandlet = new Npm(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if npm can be run properly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testNpmRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, wireMockRuntimeInfo);
    Npm commandlet = new Npm(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessage("9.9.2");
  }

  /**
   * Tests if the {@link Npm} uninstall works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testNpmUninstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, wireMockRuntimeInfo);
    Npm commandlet = new Npm(context);

    // act I
    commandlet.install();

    // assert I
    checkInstallation(context);

    // act II
    commandlet.uninstall();

    // assert II
    assertThat(context).logAtInfo().hasNoMessageContaining("npm uninstall -g npm");

    assertThat(context).logAtSuccess().hasMessage("Successfully uninstalled npm");
  }

  /**
   * Tests that {@link Npm#setEnvironment(EnvironmentContext, ToolInstallation, boolean)} points the npm global prefix at the per-project
   * {@code software/node_modules} folder so that globally installed npm packages are isolated per project (see
   * <a href="https://github.com/devonfw/IDEasy/issues/352">issue #352</a> and <a href=
   * "https://github.com/devonfw/IDEasy/issues/2381">issue #2381</a>).
   */
  @Test
  void testSetEnvironmentPointsGlobalPrefixToPerProjectNpmGlobal() {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, null, true);
    Npm commandlet = new Npm(context);
    context.getFileAccess().mkdirs(context.getSoftwarePath().resolve("npm"));
    ToolInstallation installation = new ToolInstallation(context.getSoftwarePath(), context.getSoftwarePath(), context.getSoftwarePath(),
        VersionIdentifier.of("9.9.2"), false);
    RecordingEnvironmentContext environmentContext = new RecordingEnvironmentContext();

    // act
    commandlet.setEnvironment(environmentContext, installation, false);

    // assert
    Path npmGlobalPath = context.getSoftwarePath().resolve(Npm.NPM_GLOBAL_FOLDER);
    assertThat(environmentContext.set).containsEntry("npm_config_prefix", npmGlobalPath.toString());
  }

  /**
   * Tests that {@link Npm#setEnvironment(EnvironmentContext, ToolInstallation, boolean)} puts the folder holding the executables of globally installed npm
   * packages on the PATH (the prefix's {@code bin} folder on POSIX but the prefix root on Windows, matching where npm actually places the shims) so that the
   * packages (e.g. {@code task}, {@code cdk}) are resolvable (see
   * <a href="https://github.com/devonfw/IDEasy/issues/2381">issue #2381</a>).
   */
  @Test
  void testSetEnvironmentAddsGlobalPackagesFolderToPath() {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, null, true);
    Npm commandlet = new Npm(context);
    context.getFileAccess().mkdirs(context.getSoftwarePath().resolve("npm"));
    Path softwareNpm = context.getSoftwarePath().resolve("npm");
    ToolInstallation installation = new ToolInstallation(softwareNpm, softwareNpm, softwareNpm, VersionIdentifier.of("9.9.2"), false);
    RecordingEnvironmentContext environmentContext = new RecordingEnvironmentContext();

    // act
    commandlet.setEnvironment(environmentContext, installation, false);

    // assert - the PATH must point at the folder where npm actually places the global shims (the prefix root on Windows, <prefix>/bin otherwise)
    Path npmGlobalPath = context.getSoftwarePath().resolve(Npm.NPM_GLOBAL_FOLDER);
    Path expectedBin = context.getSystemInfo().isWindows() ? npmGlobalPath : npmGlobalPath.resolve(IdeContext.FOLDER_BIN);
    assertThat(environmentContext.pathEntries).contains(expectedBin);
  }

  /**
   * Tests that {@link Npm#setEnvironment(EnvironmentContext, ToolInstallation, boolean)} does not create the per-project {@code software/node_modules} prefix
   * folder on purpose: it is created by npm when the first global package is installed and removed again by {@link Npm#cleanupGlobalPackagesFolder()} once the
   * last one is uninstalled, so the software folder stays clean (see
   * <a href="https://github.com/devonfw/IDEasy/issues/2381">issue #2381</a>).
   */
  @Test
  void testSetEnvironmentDoesNotCreatePerProjectNpmGlobalFolder() {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, null, true);
    Npm commandlet = new Npm(context);
    context.getFileAccess().mkdirs(context.getSoftwarePath().resolve("npm"));
    Path softwareNpm = context.getSoftwarePath().resolve("npm");
    ToolInstallation installation = new ToolInstallation(softwareNpm, softwareNpm, softwareNpm, VersionIdentifier.of("9.9.2"), false);

    // act
    commandlet.setEnvironment(new RecordingEnvironmentContext(), installation, false);

    // assert
    assertThat(Files.isDirectory(context.getSoftwarePath().resolve(Npm.NPM_GLOBAL_FOLDER))).isFalse();
  }

  /**
   * Tests that {@link Npm#cleanupGlobalPackagesFolder()} removes the per-project prefix once the package container no longer holds a package (e.g. after the
   * last global package was uninstalled) (see <a href="https://github.com/devonfw/IDEasy/issues/2381">issue #2381</a>).
   */
  @Test
  void testCleanupGlobalPackagesFolderRemovesPrefixWhenNoPackageInstalled() {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, null, true);
    Npm commandlet = new Npm(context);
    context.getFileAccess().mkdirs(getPackageContainer(context));

    // act
    commandlet.cleanupGlobalPackagesFolder();

    // assert
    assertThat(Files.isDirectory(context.getSoftwarePath().resolve(Npm.NPM_GLOBAL_FOLDER))).isFalse();
  }

  /**
   * Tests that {@link Npm#cleanupGlobalPackagesFolder()} keeps the per-project prefix if a global package is still installed, so that only the last
   * uninstallation actually removes it (see <a href="https://github.com/devonfw/IDEasy/issues/2381">issue #2381</a>).
   */
  @Test
  void testCleanupGlobalPackagesFolderKeepsPrefixWhenPackageStillInstalled() {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, null, true);
    Npm commandlet = new Npm(context);
    Path somePackage = context.getSoftwarePath().resolve(Npm.NPM_GLOBAL_FOLDER).resolve("lib").resolve("node_modules").resolve("some-package");
    context.getFileAccess().mkdirs(somePackage);
    context.getFileAccess().touch(somePackage.resolve("package.json"));

    // act
    commandlet.cleanupGlobalPackagesFolder();

    // assert
    assertThat(Files.isDirectory(context.getSoftwarePath().resolve(Npm.NPM_GLOBAL_FOLDER))).isTrue();
  }

  /**
   * Tests that {@link Npm#cleanupGlobalPackagesFolder()} removes the per-project prefix even if npm left an empty scope folder behind after removing a scoped
   * package, since no actual package file remains (see
   * <a href="https://github.com/devonfw/IDEasy/issues/2381">issue #2381</a>).
   */
  @Test
  void testCleanupGlobalPackagesFolderRemovesPrefixWhenOnlyEmptyScopeFolderRemains() {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, null, true);
    Npm commandlet = new Npm(context);
    context.getFileAccess().mkdirs(getPackageContainer(context).resolve("@go-task"));

    // act
    commandlet.cleanupGlobalPackagesFolder();

    // assert
    assertThat(Files.isDirectory(context.getSoftwarePath().resolve(Npm.NPM_GLOBAL_FOLDER))).isFalse();
  }

  /**
   * Tests that {@link Npm#postExtract(Path)} rewrites the npm launcher shims (that the npm registry tarball ships in the node-bundled layout) to the flat
   * layout, so that {@code npm}/{@code npx} resolve to this pristine installation instead of the npm bundled with node (see <a href=
   * "https://github.com/devonfw/IDEasy/issues/2381">issue #2381</a>).
   */
  @Test
  void testPostExtractRepairsFlatLayoutShims() {

    // arrange - a flat npm installation (as extracted from the npm registry tarball) with the broken node-bundled-layout shims
    IdeTestContext context = newContext(PROJECT_NPM, (String) null, false);
    Npm commandlet = new Npm(context);
    FileAccess fileAccess = context.getFileAccess();
    Path extractedDir = null;
    try {
      extractedDir = Files.createTempDirectory("npm-shim-repair");
      Path bin = extractedDir.resolve("bin");
      fileAccess.mkdirs(bin);
      // flat-layout CLI entry points (these are the ones the shims should launch)
      fileAccess.touch(bin.resolve("npm-cli.js"));
      fileAccess.touch(bin.resolve("npx-cli.js"));
      // the node-bundled-layout shims that npm's registry tarball ships (broken - they point at node_modules/npm/bin/npm-cli.js)
      String bundledShim = "node node_modules\\npm\\bin\\npm-cli.js";
      for (String shim : new String[] { "npm.cmd", "npx.cmd", "npm.ps1", "npx.ps1", "npm", "npx" }) {
        fileAccess.writeFileContent(bundledShim, bin.resolve(shim), false);
      }

      // act
      commandlet.postExtract(extractedDir);

      // assert - only the shims of the current platform were repaired and now launch this installation's own CLI entry
      // points instead of the broken node-bundled layout (node_modules\npm\bin\npm-cli.js)
      boolean windows = context.getSystemInfo().isWindows();
      if (windows) {
        String npmCmd = fileAccess.readFileContent(bin.resolve("npm.cmd"));
        org.assertj.core.api.Assertions.assertThat(npmCmd).contains("npm-cli.js").doesNotContain("node_modules");
        String npxCmd = fileAccess.readFileContent(bin.resolve("npx.cmd"));
        org.assertj.core.api.Assertions.assertThat(npxCmd).contains("npx-cli.js").doesNotContain("node_modules");
        String npmPs1 = fileAccess.readFileContent(bin.resolve("npm.ps1"));
        org.assertj.core.api.Assertions.assertThat(npmPs1).contains("npm-cli.js").doesNotContain("node_modules");
      } else {
        String npmPosix = fileAccess.readFileContent(bin.resolve("npm"));
        org.assertj.core.api.Assertions.assertThat(npmPosix).contains("npm-cli.js").doesNotContain("node_modules").doesNotContain("\r");
        String npxPosix = fileAccess.readFileContent(bin.resolve("npx"));
        org.assertj.core.api.Assertions.assertThat(npxPosix).contains("npx-cli.js").doesNotContain("node_modules").doesNotContain("\r");
      }
      // the CLI entry points themselves are left untouched
      org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(bin.resolve("npm-cli.js"))).isTrue();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (extractedDir != null) {
        fileAccess.delete(extractedDir);
      }
    }
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed npm in version 9.9.2");
  }

  /**
   * @param context the {@link IdeTestContext}.
   * @return a {@link Path} to a folder inside the per-project global npm prefix that represents where an installed global package would live. The cleanup under
   *     test searches the prefix as a whole for a {@code package.json} (independent of npm's platform-specific layout), so the exact location does not matter
   *     for the test.
   */
  private Path getPackageContainer(IdeTestContext context) {

    return context.getSoftwarePath().resolve(Npm.NPM_GLOBAL_FOLDER).resolve(Npm.NPM_GLOBAL_FOLDER);
  }
}
