package com.devonfw.tools.ide.tool;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.tool.repository.ToolRepository;

/**
 * Test of {@link com.devonfw.tools.ide.url.model.file.json.Cve} checks in {@link LocalToolCommandlet}.
 */
class CveCheckInstallTest extends AbstractIdeContextTest {

  protected static final Path URLS_PATH = Path.of("src/test/resources/urls");
  private static final String PROJECT_INTELLIJ = "intellij";
  private final IdeTestContext context = newContext(PROJECT_INTELLIJ);

  /**
   * Install Intellij version currently configured, regardless of CVEs.
   */
  @Test
  void testInstallToolCurrent() {
    //arrange
    context.setUrlsPath(URLS_PATH);
    Intellij commandlet = new Intellij(this.context);
    context.setAnswers("current");

    //act
    commandlet.install();

    //assert
    assertThat(context.getSoftwarePath().resolve("intellij").resolve(IdeTestContext.FILE_SOFTWARE_VERSION)).exists().hasContent("2023.3.3");
    assertThat(context.getSoftwarePath().resolve("java").resolve(IdeTestContext.FILE_SOFTWARE_VERSION)).exists().hasContent("17.0.10_7");
  }

  @Test
  void testInstallToolNearest() {
    //arrange
    context.setUrlsPath(URLS_PATH);
    Intellij commandlet = new Intellij(this.context);
    context.setAnswers("nearest");

    //act
    commandlet.install();

    //assert
    assertThat(context.getSoftwarePath().resolve("intellij").resolve(IdeTestContext.FILE_SOFTWARE_VERSION)).exists().hasContent("2022.3.2");
    assertThat(context.getSoftwarePath().resolve("java").resolve(IdeTestContext.FILE_SOFTWARE_VERSION)).exists().hasContent("17.0.10_7");
  }

  /**
   * Install Intellij latest version to avoid CVEs. This will install an extra java version not compatible with the project as dependency.
   */
  @Test
  void testInstallToolLatest() {
    //arrange
    context.setUrlsPath(URLS_PATH);
    Intellij commandlet = new Intellij(this.context);
    context.setAnswers("latest");

    //act
    commandlet.install();

    //assert
    assertThat(context.getSoftwarePath().resolve("intellij").resolve(IdeTestContext.FILE_SOFTWARE_VERSION)).exists().hasContent("2025.1.1.1");
    assertThat(context.getSoftwareRepositoryPath().resolve(ToolRepository.ID_DEFAULT).resolve("java").resolve("java").resolve("21.0.6_7")
        .resolve(IdeTestContext.FILE_SOFTWARE_VERSION)).exists().hasContent("21.0.6_7");
  }

}
