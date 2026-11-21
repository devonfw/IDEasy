package com.devonfw.ide.gui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the GUI logo assets are available on the classpath at the locations the GUI references.
 * <p>
 * The banner logo and the app icon are committed as source resources of this module. The on-classpath checks guard that the binaries reach the runtime
 * classpath intact (not corrupted by filtering). The source-file checks guard that the assets stay there even if the {@code generate-resources} phase is
 * skipped, e.g. when running {@code AppLauncher} from an IDE or with a plain {@code java -cp} (#2783).
 */
public class LogoAssetResourceTest extends Assertions {

  /** Absolute classpath location referenced by {@code NavigationPanel.fxml}. */
  private static final String BANNER_CLASSPATH = "/com/devonfw/ide/gui/assets/logo.png";

  /** Absolute classpath location referenced by {@code App.ICON_PATH}. */
  private static final String ICON_CLASSPATH = "/com/devonfw/ide/gui/assets/icon.png";

  /**
   * Working directory of the test JVM, i.e. the basedir of the {@code gui} module: the module's own {@code mvn ... test} runs the tests from the basedir (the
   * default of {@code ${basedir}}), and {@code mvn ... test} run from the multi-module reactor root resolves the module's own basedir (not the reactor root's)
   * the same way.
   */
  private static final Path MODULE_BASEDIR = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();

  /** The banner logo is a 680x156 wordmark. */
  private static final int BANNER_WIDTH = 680;

  private static final int BANNER_HEIGHT = 156;

  /** The app icon is a square 256x256 master. */
  private static final int ICON_SIZE = 256;

  @Test
  public void bannerLogoIsAvailableOnTheClasspathAndIntact() throws IOException {

    assertPngOnClasspath(BANNER_CLASSPATH, BANNER_WIDTH, BANNER_HEIGHT);
  }

  @Test
  public void appIconIsAvailableOnTheClasspathAndIntact() throws IOException {

    assertPngOnClasspath(ICON_CLASSPATH, ICON_SIZE, ICON_SIZE);
  }

  @Test
  public void bannerLogoIsACommittedSourceResource() throws IOException {

    assertCommittedSourceResource("logo.png", BANNER_WIDTH, BANNER_HEIGHT);
  }

  @Test
  public void appIconIsACommittedSourceResource() throws IOException {

    assertCommittedSourceResource("icon.png", ICON_SIZE, ICON_SIZE);
  }

  /**
   * Verifies that the given logo asset is committed as a source resource of this module (not only produced by a Maven build-phase
   * side effect), and that it is a valid PNG of the expected size.
   *
   * @param fileName the asset file name within the module's resources
   * @param expectedWidth the expected PNG width
   * @param expectedHeight the expected PNG height
   */
  private void assertCommittedSourceResource(String fileName, int expectedWidth, int expectedHeight) throws IOException {

    Path source = MODULE_BASEDIR.resolve("src/main/resources/com/devonfw/ide/gui/assets/" + fileName);
    assertThat(source).as("%s must be a committed source resource of the gui module", fileName).isRegularFile();

    byte[] png = Files.readAllBytes(source);
    assertPngIntact(fileName, png, expectedWidth, expectedHeight);
  }

  /**
   * Verifies the PNG signature and dimensions of the given asset.
   *
   * @param assetLabel label of the asset used in the assertion messages (file name or classpath location)
   * @param png the raw PNG bytes
   * @param expectedWidth the expected PNG width
   * @param expectedHeight the expected PNG height
   */
  private void assertPngIntact(String assetLabel, byte[] png, int expectedWidth, int expectedHeight) {

    assertThat(png.length).as("PNG %s must not be empty (guards against filtering corrupting the binary)", assetLabel).isGreaterThan(1000);
    assertThat(png[0] & 0xFF).isEqualTo(0x89);
    assertThat(png[1]).isEqualTo((byte) 'P');
    assertThat(png[2]).isEqualTo((byte) 'N');
    assertThat(png[3]).isEqualTo((byte) 'G');

    int width = ((png[16] & 0xFF) << 24) | ((png[17] & 0xFF) << 16) | ((png[18] & 0xFF) << 8) | (png[19] & 0xFF);
    int height = ((png[20] & 0xFF) << 24) | ((png[21] & 0xFF) << 16) | ((png[22] & 0xFF) << 8) | (png[23] & 0xFF);
    assertThat(width).as("width of %s", assetLabel).isEqualTo(expectedWidth);
    assertThat(height).as("height of %s", assetLabel).isEqualTo(expectedHeight);
  }

  private void assertPngOnClasspath(String classpath, int expectedWidth, int expectedHeight) throws IOException {

    URL url = getClass().getResource(classpath);
    assertThat(url).as("logo asset %s must be on the classpath", classpath).isNotNull();

    byte[] png;
    try (InputStream in = url.openStream()) {
      png = in.readAllBytes();
    }

    assertPngIntact(classpath, png, expectedWidth, expectedHeight);
  }
}
