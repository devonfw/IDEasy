package com.devonfw.tools.ide.tool;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.tool.repository.ToolRepository;

/**
 * Test of {@link com.devonfw.tools.ide.url.model.file.json.Cve} checks in {@link LocalToolCommandlet}.
 */
class CveCheckInstallTest extends AbstractIdeContextTest {

  protected static final Path URLS_PATH = Path.of("src/test/resources/urls");
  private static final String PROJECT_INTELLIJ = "intellij";

  private static final String SECURITY_JSON_EQUAL_SEVERITY = """
      {
        "issues": [
          {
            "id": "CVE-TEST-EQUAL",
            "severity": 5.0,
            "versions": [
              "(0,2025.1.1)"
            ]
          },
          {
            "id": "CVE-TEST-HIGH",
            "severity": 9.0,
            "versions": [
              "[2025.1.1]"
            ]
          }
        ]
      }
      """;

  private final IdeTestContext context = newContext(PROJECT_INTELLIJ);


  /**
   * +   * Install Intellij with a configured version for which a version above it is the nearest safe upgrade. Ensures the documented "nearest" suggestion (see
   * +   * {@code documentation/security.adoc}) also offers versions greater than the configured one. +
   */
  @Test
  void testInstallToolNearestSuggestsMinimalSafeUpgrade() {
    //arrange
    IdeTestContext context = newContext(PROJECT_INTELLIJ);
    context.setUrlsPath(URLS_PATH);
    context.getFileAccess().writeFileContent("JAVA_VERSION=17.0.10_7\nINTELLIJ_VERSION=2022.3\n", context.getSettingsPath().resolve("ide.properties"));
    context.reload();
    Intellij commandlet = new Intellij(context);
    context.setAnswers("nearest");

    //act
    commandlet.install();

    //assert
    assertThat(context.getSoftwarePath().resolve("intellij").resolve(IdeTestContext.FILE_SOFTWARE_VERSION)).exists().hasContent("2022.3.2");
  }

  /**
   * Install Intellij where the only allowed version above the configured one has exactly the same CVEs. Such a version must never be offered as "nearest" since
   * it does not reduce the vulnerabilities - this is the bug reported in <a href="https://github.com/devonfw/IDEasy/issues/2313">#2313</a>.
   */
  @Test
  void testInstallToolWithoutNearestIfNotSaferThanCurrent() {
    //arrange
    IdeTestContext context = newContext(PROJECT_INTELLIJ);
    Path urlsPath = context.getIdeRoot().resolve("cve-urls");
    FileAccess fileAccess = context.getFileAccess();
    fileAccess.mkdirs(urlsPath);
    fileAccess.copy(URLS_PATH, urlsPath, FileCopyMode.COPY_TREE_OVERRIDE_TREE);
    fileAccess.writeFileContent(SECURITY_JSON_EQUAL_SEVERITY, urlsPath.resolve("urls").resolve("intellij").resolve("security.json"));
    context.setUrlsPath(urlsPath.resolve("urls"));
    Intellij commandlet = new Intellij(context);
    context.setAnswers("current");

    //act
    commandlet.install();

    //assert
    assertThat(context).logAtInteraction().hasMessageContaining("latest (2025.1.1.1 - safe)");
    assertThat(context).logAtInteraction().hasNoMessageContaining("nearest (");
  }

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
