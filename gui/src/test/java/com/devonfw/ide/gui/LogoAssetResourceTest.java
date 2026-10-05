package com.devonfw.ide.gui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the banner logo is available on the classpath at the location the navigation panel references
 * ({@code com/devonfw/ide/gui/assets/logo.png}).
 * <p>
 * The file is not stored in this module; it is copied at build time from the canonical asset in
 * {@code documentation/images/logo.png} (see #2567). This test guards that the derived copy is present and that
 * resource filtering has not corrupted the binary.
 */
public class LogoAssetResourceTest extends Assertions {

  /** Absolute classpath location referenced by {@code NavigationPanel.fxml}. */
  private static final String CLASSPATH = "/com/devonfw/ide/gui/assets/logo.png";

  /** The banner logo is a 680x156 wordmark. */
  private static final int EXPECTED_WIDTH = 680;

  private static final int EXPECTED_HEIGHT = 156;

  @Test
  public void bannerLogoIsAvailableOnTheClasspathAndIntact() throws IOException {

    URL url = getClass().getResource(CLASSPATH);
    assertThat(url).as("banner logo %s must be on the classpath", CLASSPATH).isNotNull();

    byte[] png;
    try (InputStream in = url.openStream()) {
      png = in.readAllBytes();
    }

    assertThat(png.length).as("PNG must not be empty (guards against filtering corrupting the binary)").isGreaterThan(1000);
    assertThat(png[0] & 0xFF).isEqualTo(0x89);
    assertThat(png[1]).isEqualTo((byte) 'P');
    assertThat(png[2]).isEqualTo((byte) 'N');
    assertThat(png[3]).isEqualTo((byte) 'G');

    int width = ((png[16] & 0xFF) << 24) | ((png[17] & 0xFF) << 16) | ((png[18] & 0xFF) << 8) | (png[19] & 0xFF);
    int height = ((png[20] & 0xFF) << 24) | ((png[21] & 0xFF) << 16) | ((png[22] & 0xFF) << 8) | (png[23] & 0xFF);
    assertThat(width).isEqualTo(EXPECTED_WIDTH);
    assertThat(height).isEqualTo(EXPECTED_HEIGHT);
  }
}
