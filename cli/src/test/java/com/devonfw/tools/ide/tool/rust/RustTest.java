package com.devonfw.tools.ide.tool.rust;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link Rust}.
 */
class RustTest extends AbstractIdeContextTest {

  private static final String PROJECT_RUST = "rust";

  private static final String RUST_VERSION = "1.80.1";

  @Test
  void testRustInstallViaRustupScript() {

    // arrange
    IdeTestContext context = newContext(PROJECT_RUST);
    Rust rust = new Rust(context);

    // act
    rust.install();

    // assert
    assertThat(context.getSoftwarePath().resolve("rust/.ide.software.version")).exists().hasContent(RUST_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed rust in version " + RUST_VERSION);
  }

  @Test
  void testRustInstallProducesCargoLayoutAndBinLink() {

    // arrange
    IdeTestContext context = newContext(PROJECT_RUST);
    Rust rust = new Rust(context);
    String rustcName = context.getSystemInfo().isWindows() ? "rustc.cmd" : "rustc";

    // act
    rust.install();

    // assert
    Path softwareRust = context.getSoftwarePath().resolve("rust");
    assertThat(softwareRust.resolve(".cargo")).exists();
    assertThat(softwareRust.resolve(".rustup")).exists();
    assertThat(softwareRust.resolve(".cargo/bin").resolve(rustcName)).exists();
    assertThat(softwareRust.resolve("bin").resolve(rustcName)).exists();
  }
}
