package com.devonfw.ide.gui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the GUI logo assets are available on the classpath at the locations the GUI references.
 * <p>
 * The banner logo ({@code com/devonfw/ide/gui/assets/logo.png}) and the app icon
 * ({@code com/devonfw/ide/gui/assets/ideasy.png}) are not stored in this module; they are copied at build time
 * from the canonical assets in {@code documentation/images/} (see #2567). These tests guard that the derived
 * copies are present and that resource filtering has not corrupted the binaries.
 */
public class LogoAssetResourceTest extends Assertions {

  /** Absolute classpath location referenced by {@code NavigationPanel.fxml}. */
  private static final String BANNER_CLASSPATH = "/com/devonfw/ide/gui/assets/logo.png";

  /** Absolute classpath location referenced by {@code App.ICON_PATH}. */
  private static final String ICON_CLASSPATH = "/com/devonfw/ide/gui/assets/ideasy.png";

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

  private void assertPngOnClasspath(String classpath, int expectedWidth, int expectedHeight) throws IOException {

    URL url = getClass().getResource(classpath);
    assertThat(url).as("logo asset %s must be on the classpath", classpath).isNotNull();

    byte[] png;
    try (InputStream in = url.openStream()) {
      png = in.readAllBytes();
    }

    assertThat(png.length).as("PNG %s must not be empty (guards against filtering corrupting the binary)", classpath).isGreaterThan(1000);
    assertThat(png[0] & 0xFF).isEqualTo(0x89);
    assertThat(png[1]).isEqualTo((byte) 'P');
    assertThat(png[2]).isEqualTo((byte) 'N');
    assertThat(png[3]).isEqualTo((byte) 'G');

    int width = ((png[16] & 0xFF) << 24) | ((png[17] & 0xFF) << 16) | ((png[18] & 0xFF) << 8) | (png[19] & 0xFF);
    int height = ((png[20] & 0xFF) << 24) | ((png[21] & 0xFF) << 16) | ((png[22] & 0xFF) << 8) | (png[23] & 0xFF);
    assertThat(width).as("width of %s", classpath).isEqualTo(expectedWidth);
    assertThat(height).as("height of %s", classpath).isEqualTo(expectedHeight);
  }
}
