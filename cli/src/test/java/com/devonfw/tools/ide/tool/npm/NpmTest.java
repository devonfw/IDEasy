package com.devonfw.tools.ide.tool.npm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
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
   * Tests that {@link Npm#setEnvironment(EnvironmentContext, ToolInstallation, boolean)} points the npm global prefix at a per-project
   * {@code .npm-global} folder inside the IDE home so that projects do not interfere with each other (see
   * <a href="https://github.com/devonfw/IDEasy/issues/352">issue #352</a> and <a href=
   * "https://github.com/devonfw/IDEasy/issues/2381">issue #2381</a>).
   */
  @Test
  void testSetEnvironmentPointsGlobalPrefixToPerProjectNpmGlobal() {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, (String) null, false);
    Npm commandlet = new Npm(context);
    Path dummy = context.getSoftwarePath().resolve("npm");
    ToolInstallation installation = new ToolInstallation(dummy, dummy, dummy, VersionIdentifier.of("9.9.2"), false);
    RecordingEnvironmentContext environmentContext = new RecordingEnvironmentContext();

    // act
    commandlet.setEnvironment(environmentContext, installation, false);

    // assert
    Path npmGlobalPath = context.getIdeHome().resolve(Npm.NPM_GLOBAL_FOLDER);
    assertThat(environmentContext.set).containsEntry("npm_config_prefix", npmGlobalPath.toString());
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

      // assert - the shims now launch this installation's own CLI entry points and no longer reference the node-bundled layout
      boolean windows = context.getSystemInfo().isWindows();
      String npmCmd = fileAccess.readFileContent(bin.resolve("npm.cmd"));
      org.assertj.core.api.Assertions.assertThat(npmCmd).contains("npm-cli.js").doesNotContain("node_modules");
      String npxCmd = fileAccess.readFileContent(bin.resolve("npx.cmd"));
      org.assertj.core.api.Assertions.assertThat(npxCmd).contains("npx-cli.js").doesNotContain("node_modules");
      String npmPs1 = fileAccess.readFileContent(bin.resolve("npm.ps1"));
      org.assertj.core.api.Assertions.assertThat(npmPs1).contains("npm-cli.js").doesNotContain("node_modules");
      // the POSIX shims are only rewritten on non-Windows systems
      if (!windows) {
        String npmPosix = fileAccess.readFileContent(bin.resolve("npm"));
        org.assertj.core.api.Assertions.assertThat(npmPosix).contains("npm-cli.js").doesNotContain("node_modules");
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
}
