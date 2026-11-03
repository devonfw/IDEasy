package com.devonfw.tools.ide.tool.snyk;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link Snyk}.
 */
class SnykTest extends AbstractIdeContextTest {

  private static final String SNYK_PROJECT = "snyk";

  private static final String SNYK_VERSION = "1.1307.4";

  @Test
  void testSnykInstallSucceedsViaLocalRepository() {

    IdeTestContext context = newContext(SNYK_PROJECT);
    Snyk snyk = new Snyk(context);

    snyk.install();

    assertInstalled(context);
  }

  /**
   * Tests that running {@link Snyk#run()} installs the tool on demand and actually executes the installed binary, forwarding the given
   * arguments. This asserts the commandlet behaves correctly at runtime, not only that the install wrote its markers.
   */
  @Test
  void testSnykRunExecutesTheInstalledBinary() {

    IdeTestContext context = newContext(SNYK_PROJECT);
    Snyk snyk = new Snyk(context);
    snyk.arguments.setValue("--version");

    snyk.run();

    assertInstalled(context);
    // the mock binary echoes the executed command, so a successful execution of the installed binary is observable in the log
    assertThat(context).logAtInfo().hasMessageContaining("snyk --version");
  }

  private void assertInstalled(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("snyk/.ide.software.version")).exists().hasContent(SNYK_VERSION);
    // the raw downloaded binary (snyk-linux / snyk-win.exe) must be normalized to the name the launcher resolves
    String binaryName = context.getSystemInfo().isWindows() ? "snyk.exe" : "snyk";
    Path toolDir = context.getSoftwarePath().resolve("snyk");
    // proves the rename actually happened: the download mock names the served file "content.snyk.<version>",
    // so the launcher-expected name (snyk / snyk.exe) only exists if installDownloadedToolPayload moved it.
    // (A content check is not used because Windows blocks reading PE files via a plain read channel.)
    assertThat(toolDir.resolve(binaryName)).exists();
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed snyk in version " + SNYK_VERSION);
  }
}
