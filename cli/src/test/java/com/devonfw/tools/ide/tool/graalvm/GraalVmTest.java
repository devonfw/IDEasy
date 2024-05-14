package com.devonfw.tools.ide.tool.graalvm;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import org.junit.jupiter.api.Test;

/**
 * Integration test of {@link GraalVm}.
 */
public class GraalVmTest extends AbstractIdeContextTest {

  private static final String PROJECT_GRAALVM = "graalvm";

  @Test
  public void testGraalVmInstallInDirectoryExtra() {

    // arrange
    IdeTestContext context = newContext(PROJECT_GRAALVM);

    GraalVm commandlet = new GraalVm(context);

    // act
    commandlet.install();

    // assert
    assertThat(context.getSoftwareExtraPath().resolve("graalvm/bin/HelloWorld.txt")).exists();
    assertThat(context.getSoftwareExtraPath().resolve("graalvm/.ide.software.version")).exists().hasContent("22.3.3");
  }
}