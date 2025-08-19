package com.devonfw.tools.ide.tool;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.intellij.Intellij;

public class LookForCVEsTest extends AbstractIdeContextTest {

  protected static final Path URLS_PATH = Path.of("src/test/resources/urls");
  private static final String PROJECT_INTELLIJ = "intellij";
  private final IdeTestContext context = newContext(PROJECT_INTELLIJ);

  @Test
  public void testInstallTool() {
    //arrange
    context.setUrlsPath(URLS_PATH);
    Intellij commandlet = new Intellij(this.context);
    context.setAnswers("current");
    
    //act
    commandlet.install();

    //assert
    assertThat(context.getSoftwarePath().resolve("intellij/.ide.software.version")).exists().hasContent("2023.3.3");
  }

}
