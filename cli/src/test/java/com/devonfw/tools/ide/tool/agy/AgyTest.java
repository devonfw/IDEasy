package com.devonfw.tools.ide.tool.agy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.FileAccess;

/**
 * Test of {@link Agy}.
 */
class AgyTest extends AbstractIdeContextTest {

  @TempDir
  Path home;

  private IdeTestContext context;

  private Agy agy;

  private FileAccess getFileAccess() {

    return this.context.getFileAccess();
  }

  private boolean isLink(Path path) {

    return Files.isSymbolicLink(path) || this.getFileAccess().isJunction(path);
  }

  @BeforeEach
  void setup() {
    this.context = newContext(PROJECT_BASIC);
    this.context.setUserHome(this.home);
    this.agy = this.context.getCommandletManager().getCommandlet(Agy.class);
  }

  /**
   * Test {@link Agy#getAgyConfigDir()} resolves to the project-local main configuration directory.
   */
  @Test
  void testGetAgyConfigDir() {
    assertThat(this.agy.getAgyConfigDir()).isEqualTo(this.context.getConfPath().resolve("gemini/antigravity-cli"));
  }

  /**
   * Test {@link Agy#getAgyMcpConfigDir()} resolves to the project-local MCP configuration directory.
   */
  @Test
  void testGetAgyMcpConfigDir() {
    assertThat(this.agy.getAgyMcpConfigDir()).isEqualTo(this.context.getConfPath().resolve("gemini/config"));
  }

  /**
   * Test {@link Agy#linkHomeConfigDir()} creates a link for both Agy home locations pointing to their project-local configuration directories.
   */
  @Test
  void testLinkHomeConfigDirCreatesLinks() {
    // act
    this.agy.linkHomeConfigDir();
    // assert
    Path antigravityHome = this.home.resolve(Agy.HOME_ANTIGRAVITY_CLI_LOCATION);
    Path mcpHome = this.home.resolve(Agy.HOME_MCP_CONFIG_LOCATION);
    assertThat(this.isLink(antigravityHome)).as(antigravityHome.toString()).isTrue();
    assertThat(this.isLink(mcpHome)).as(mcpHome.toString()).isTrue();
    assertThat(this.getFileAccess().toRealPath(antigravityHome)).isEqualTo(this.getFileAccess().toRealPath(this.agy.getAgyConfigDir()));
    assertThat(this.getFileAccess().toRealPath(mcpHome)).isEqualTo(this.getFileAccess().toRealPath(this.agy.getAgyMcpConfigDir()));
  }

  /**
   * Test {@link Agy#linkHomeConfigDir()} is idempotent: calling it again with the link already pointing to the target does not fail.
   */
  @Test
  void testLinkHomeConfigDirIsIdempotent() {
    // arrange
    this.agy.linkHomeConfigDir();
    // act
    assertDoesNotThrow(this.agy::linkHomeConfigDir);
    // assert
    assertThat(this.getFileAccess().toRealPath(this.home.resolve(Agy.HOME_ANTIGRAVITY_CLI_LOCATION)))
        .isEqualTo(this.getFileAccess().toRealPath(this.agy.getAgyConfigDir()));
  }

  /**
   * Test {@link Agy#linkHomeConfigDir()} re-points a link that currently resolves to a different (foreign) project configuration.
   */
  @Test
  void testLinkHomeConfigDirRepointsForeignLink() {
    // arrange
    Path foreign = this.home.resolve("foreign");
    this.getFileAccess().mkdirs(foreign);
    Path antigravityHome = this.home.resolve(Agy.HOME_ANTIGRAVITY_CLI_LOCATION);
    // create an absolute link: on Windows the junction fallback requires an absolute target and an existing parent directory
    this.getFileAccess().mkdirs(antigravityHome.getParent());
    this.getFileAccess().symlink(foreign, antigravityHome, false);
    // act
    this.agy.linkHomeConfigDir();
    // assert
    assertThat(this.getFileAccess().toRealPath(antigravityHome)).isEqualTo(this.getFileAccess().toRealPath(this.agy.getAgyConfigDir()));
  }

  /**
   * Test {@link Agy#linkHomeConfigDir()} does not clobber a regular directory that Agy already uses (holds user data).
   */
  @Test
  void testLinkHomeConfigDirDoesNotClobberExistingDirectory() {
    // arrange
    Path antigravityHome = this.home.resolve(Agy.HOME_ANTIGRAVITY_CLI_LOCATION);
    this.getFileAccess().mkdirs(antigravityHome);
    this.getFileAccess().writeFileContent("user data", antigravityHome.resolve("data.txt"), true);
    // act
    this.agy.linkHomeConfigDir();
    // assert
    assertThat(this.isLink(antigravityHome)).as(antigravityHome.toString()).isFalse();
    assertThat(antigravityHome.resolve("data.txt")).hasContent("user data");
  }

  /**
   * Test {@code postInstall} seeds a minimal {@code settings.json} and a {@code README.md} without overwriting existing files.
   */
  @Test
  void testSeedConfigSkeleton() {
    // arrange
    Path configDir = this.agy.getAgyConfigDir();
    // act
    this.agy.seedConfigSkeleton();
    // assert
    assertThat(configDir.resolve("settings.json")).exists();
    assertThat(configDir.resolve("README.md")).exists();
    // act
    this.agy.seedConfigSkeleton();
    // assert
    assertThat(configDir.resolve("settings.json")).exists();
  }
}
