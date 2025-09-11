package com.devonfw.tools.ide.tool;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.intellij.Intellij;

/**
 * Test for method lookForCVEs in {@link LocalToolCommandlet}.
 */
public class LookForCVEsTest extends AbstractIdeContextTest {

  protected static final Path URLS_PATH = Path.of("src/test/resources/urls");
  private static final String PROJECT_INTELLIJ = "intellij";
  private final IdeTestContext context = newContext(PROJECT_INTELLIJ);

  /**
   * Tests lookForCVEs with Intellij install.
   */
  @Test
  public void testInstallToolCurrent() {
    //arrange
    context.setUrlsPath(URLS_PATH);
    Intellij commandlet = new Intellij(this.context);
    context.setAnswers("current");

    //act
    commandlet.install();

    //assert
    assertThat(context.getSoftwarePath().resolve("intellij/.ide.software.version")).exists().hasContent("2023.3.3");
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent("17.0.10_7");
  }

  @Test
  public void testInstallToolNearest() {
    //arrange
    context.setUrlsPath(URLS_PATH);
    Intellij commandlet = new Intellij(this.context);
    context.setAnswers("nearest");

    //act
    commandlet.install();

    //assert
    assertThat(context.getSoftwarePath().resolve("intellij/.ide.software.version")).exists().hasContent("2022.3.2");
    assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent("17.0.10_7");
  }

  @Test
  public void testInstallToolLatest() {
    //arrange
    context.setUrlsPath(URLS_PATH);
    Intellij commandlet = new Intellij(this.context);
    context.setAnswers("latest");

    //act
    commandlet.install();

    //assert
    assertThat(context.getSoftwarePath().resolve("intellij/.ide.software.version")).exists().hasContent("2025.1.1.1");
    //assertThat(context.getSoftwarePath().resolve("java/.ide.software.version")).exists().hasContent("21.0.6_7");
  }

}
