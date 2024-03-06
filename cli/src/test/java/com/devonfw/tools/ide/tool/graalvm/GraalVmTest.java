package com.devonfw.tools.ide.tool.graalvm;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

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
    commandlet.run();

    // assert
    assertThat(context.getSoftwarePath().resolve("extra/graalvm/graalvm-ce-java11-22.3.3/bin/HelloWorld.txt")).exists();
    assertThat(context.getSoftwarePath().resolve("extra/graalvm/.ide.software.version")).exists().hasContent("22.3.3");
  }

  @Test
  public void testAddToPropertiesFile() {

    // arrange
    IdeTestContext context = newContext(PROJECT_GRAALVM);

    GraalVm commandlet = new GraalVm(context);
    final String graalvmExport = "export GRAALVM_HOME=" + context.getSoftwarePath().resolve("extra/graalvm");

    // act
    commandlet.run();
    String file = context.getConfPath().resolve("devon.properties").toString();

    // assert
    assertThat(context.getConfPath().resolve("devon.properties")).exists();
    // todo: look for content
  }

}
