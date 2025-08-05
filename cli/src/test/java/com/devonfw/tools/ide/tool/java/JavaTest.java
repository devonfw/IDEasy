package com.devonfw.tools.ide.tool.java;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Java}.
 */
@WireMockTest
public class JavaTest extends AbstractIdeContextTest {

  private static final String PROJECT_JAVA = "java";

  /** Tests the installation and run of {@link Java}. */
  @Test
  public void testJavaInstallAndRun(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_JAVA, wmRuntimeInfo);
    createFakedSrcJar(context);
    Java java = context.getCommandletManager().getCommandlet(Java.class);
    java.arguments.addValue("--version");
    // act
    java.run();

    // assert
    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent("17.0.10_7");
    assertThat(context).logAtSuccess().hasMessage("Successfully installed java in version 17.0.10_7");
    assertThat(context).logAtInfo().hasMessage("java --version");
  }

  private static void createFakedSrcJar(IdeTestContext context) {

    // create large dummy src.zip file to reproduce bug #1437 condition
    Path fakedJavaLibDir = context.getIdeRoot().resolve("repository").resolve("java").resolve("java").resolve("default").resolve("lib");
    context.getFileAccess().mkdirs(fakedJavaLibDir);
    Path srcZip = fakedJavaLibDir.resolve("src.zip");
    Random random = new Random(1);
    byte[] buffer = new byte[1000];
    try (OutputStream out = Files.newOutputStream(srcZip)) {
      for (int i = 0; i < 100; i++) {
        for (int j = 0; j < buffer.length; j++) {
          buffer[j] = (byte) random.nextInt();
        }
        out.write(buffer);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
