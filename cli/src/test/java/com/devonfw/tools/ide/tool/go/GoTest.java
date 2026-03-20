package com.devonfw.tools.ide.tool.go;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test of {@link Go}.
 */
class GoTest extends AbstractIdeContextTest {

  private static final String PROJECT_GO = "go";

  private static final String GO_VERSION = "1.22.4";

  @ParameterizedTest
  @ValueSource(strings = { "windows", "linux", "mac" })
  void testGoInstallSucceedsOnAllPlatforms(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_GO);
    context.setSystemInfo(SystemInfoMock.of(os));
    Go go = new Go(context);

    // act
    go.install();

    // assert
    assertThat(context.getSoftwarePath().resolve("go/.ide.software.version"))
        .exists()
        .hasContent(GO_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed go in version " + GO_VERSION);
  }
}
