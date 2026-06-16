package com.devonfw.tools.ide.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link GlobalToolCommandlet}.
 */
class GlobalToolCommandletTest extends AbstractIdeContextTest {

  private static final String DUMMY_BINARY = "dummy";

  /**
   * Tests that on macOS the installation path is resolved from the app bundle in the applications folder and its executable folder is registered.
   *
   * @param tempDir the temporary directory.
   * @throws IOException on test setup failure.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  void testGetInstallationPathFindsMacApp(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeTestContext context = new IdeTestContext();
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    Path applicationsPath = tempDir.resolve("Applications");
    Path appPath = applicationsPath.resolve("Dummy.app");
    Path appBinPath = appPath.resolve("Contents").resolve("MacOS");
    Path binary = appBinPath.resolve(DUMMY_BINARY);
    Files.createDirectories(appBinPath);
    Files.writeString(binary, "test");
    context.getFileAccess().makeExecutable(binary);
    GlobalToolCommandlet globalTool = new GlobalToolDummyCommandlet(context, applicationsPath);

    // act
    Path result = globalTool.getInstallationPath("default", VersionIdentifier.of("1.0"));

    // assert
    assertThat(result).isEqualTo(appPath);
    assertThat(context.getPath().getPath(DUMMY_BINARY)).isEqualTo(appBinPath);
  }

  /**
   * Tests that the applications folder is only searched on macOS and not on other operating systems.
   *
   * @param tempDir the temporary directory.
   * @throws IOException on test setup failure.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  void testGetInstallationPathDoesNotSearchApplicationsOnLinux(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeTestContext context = new IdeTestContext();
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Path applicationsPath = tempDir.resolve("Applications");
    Path appBinPath = applicationsPath.resolve("Dummy.app").resolve("Contents").resolve("MacOS");
    Path binary = appBinPath.resolve(DUMMY_BINARY);
    Files.createDirectories(appBinPath);
    Files.writeString(binary, "test");
    context.getFileAccess().makeExecutable(binary);
    GlobalToolCommandlet globalTool = new GlobalToolDummyCommandlet(context, applicationsPath);

    // act & assert
    assertThat(globalTool.getInstallationPath("default", VersionIdentifier.of("1.0"))).isNull();
    assertThat(context.getPath().getPath(DUMMY_BINARY)).isNull();
  }

  private static class GlobalToolDummyCommandlet extends GlobalToolCommandlet {

    private final Path applicationsPath;

    GlobalToolDummyCommandlet(IdeContext context, Path applicationsPath) {

      super(context, DUMMY_BINARY, Set.of(Tag.TEST));
      this.applicationsPath = applicationsPath;
    }

    @Override
    protected String getBinaryName() {

      return DUMMY_BINARY;
    }

    @Override
    protected Path getMacApplicationsPath() {

      return this.applicationsPath;
    }
  }
}
