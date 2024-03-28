package com.devonfw.tools.ide.tool.graalvm;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;

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
    assertThat(context.getSoftwarePath().resolve("extra/graalvm/bin/HelloWorld.txt")).exists();
    assertThat(context.getSoftwarePath().resolve("extra/graalvm/.ide.software.version")).exists().hasContent("22.3.3");
  }

  @Test
  public void testAddTextToPropertiesFile() {

    // arrange
    IdeTestContext context = newContext(PROJECT_GRAALVM);
    GraalVm commandlet = new GraalVm(context);

    // act
    commandlet.run();

    // assert
    String graalvmExport = "export GRAALVM_HOME=" + context.getSoftwarePath().resolve("extra/graalvm");
    Path file = context.getConfPath().resolve("devon.properties");
    assertThat(hasFileContent(file, graalvmExport)).isTrue();
  }

  @Test
  public void testRunCMDWithArgs() {

    // arrange
    IdeTestContext context = newContext(PROJECT_GRAALVM);
    GraalVm commandlet = new GraalVm(context);
    String argument = "testArgument";
    commandlet.arguments.setValue(List.of("testFile", argument));

    if (context.getSystemInfo().isWindows()) {
      // act
      commandlet.run();

      // assert
      assertLogMessage(context, IdeLogLevel.INFO, "graalvm windows " + argument);
    }
  }

  private boolean hasFileContent(Path path, String textToSearch) {

    try (BufferedReader br = Files.newBufferedReader(path)) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.contains(textToSearch)) {
          return true;
        }
      }
      return false;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}