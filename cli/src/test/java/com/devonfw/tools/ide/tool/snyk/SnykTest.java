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
