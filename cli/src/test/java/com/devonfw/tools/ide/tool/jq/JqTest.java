package com.devonfw.tools.ide.tool.jq;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link Jq}.
 */
public class JqTest extends AbstractIdeContextTest {

  private static final String PROJECT_JQ = "jq";

  private static final String JQ_VERSION = "1.8.2";

  @Test
  void testJqInstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_JQ);
    Jq jq = new Jq(context);

    // act
    jq.install();

    // assert
    assertThat(jq.getName()).isEqualTo(PROJECT_JQ);
    assertThat(jq.getTags()).containsExactly(Tag.JSON);
    // the raw release binary must be renamed to the executable name so that `ide jq` runs it
    String binaryName = context.getSystemInfo().isWindows() ? "jq.exe" : "jq";
    assertThat(context.getSoftwarePath().resolve(PROJECT_JQ).resolve(binaryName)).exists();
    assertThat(context.getSoftwarePath().resolve(PROJECT_JQ).resolve(".ide.software.version")).exists().hasContent(JQ_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed jq in version " + JQ_VERSION);
  }

  @Test
  void testJqInstallViaCommandlet() {

    // arrange
    IdeTestContext context = newContext(PROJECT_JQ);
    Jq jq = context.getCommandletManager().getCommandlet(Jq.class);

    // act
    jq.install();

    // assert
    assertThat(context.getSoftwarePath().resolve(PROJECT_JQ).resolve(".ide.software.version")).exists().hasContent(JQ_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed jq in version " + JQ_VERSION);
  }

}
