package com.devonfw.tools.ide.tool.pgadmin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link PgAdmin}.
 */
class PgAdminTest extends AbstractIdeContextTest {

  /**
   * Tests that the pgAdmin binary name matches the target operating system.
   *
   * @param os the operating system to simulate.
   * @param binaryName the expected binary name.
   */
  @ParameterizedTest
  @CsvSource({ "windows, pgadmin4", "linux, pgadmin4", "mac, pgAdmin 4" })
  void testGetBinaryName(String os, String binaryName) {

    // arrange
    IdeTestContext context = new IdeTestContext();
    context.setSystemInfo(SystemInfoMock.of(os));
    PgAdmin pgAdmin = new PgAdmin(context);

    // act & assert
    assertThat(pgAdmin.getBinaryName()).isEqualTo(binaryName);
  }

  /**
   * Tests that {@link PgAdmin#getInstallationPath(String, VersionIdentifier)} preserves the generic PATH based result.
   *
   * @param tempDir the temporary directory.
   * @throws IOException on test setup failure.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  void testGetInstallationPathKeepsPathResult(@TempDir Path tempDir) throws IOException {

    // arrange
    IdeTestContext context = new IdeTestContext();
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Path installationPath = tempDir.resolve("pgadmin");
    Path binPath = installationPath.resolve("bin");
    Path binary = binPath.resolve("pgadmin4");
    Files.createDirectories(binPath);
    Files.writeString(binary, "test");
    context.getFileAccess().makeExecutable(binary);
    context.getPath().setPath("pgadmin", binPath);
    PgAdmin pgAdmin = new TestPgAdmin(context, tempDir.resolve("Applications"));

    // act
    Path result = pgAdmin.getInstallationPath("pgadmin", VersionIdentifier.of("8.13"));

    // assert
    assertThat(result).isEqualTo(installationPath);
  }

  /**
   * Tests that macOS installation detection resolves the pgAdmin app bundle and registers its executable folder.
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
    Path appPath = applicationsPath.resolve("pgAdmin 4.app");
    Path appBinPath = appPath.resolve("Contents").resolve("MacOS");
    Path binary = appBinPath.resolve("pgAdmin 4");
    Files.createDirectories(appBinPath);
    Files.writeString(binary, "test");
    context.getFileAccess().makeExecutable(binary);
    PgAdmin pgAdmin = new TestPgAdmin(context, applicationsPath);

    // act
    Path result = pgAdmin.getInstallationPath("pgadmin", VersionIdentifier.of("8.13"));

    // assert
    assertThat(result).isEqualTo(appPath);
    assertThat(context.getPath().getPath("pgadmin")).isEqualTo(appBinPath);
  }

  private static class TestPgAdmin extends PgAdmin {

    private final Path applicationsPath;

    TestPgAdmin(IdeTestContext context, Path applicationsPath) {

      super(context);
      this.applicationsPath = applicationsPath;
    }

    @Override
    protected Path getMacApplicationsPath() {

      return this.applicationsPath;
    }
  }
}
