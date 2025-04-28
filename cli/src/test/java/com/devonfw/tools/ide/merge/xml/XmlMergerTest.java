package com.devonfw.tools.ide.merge.xml;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlunit.assertj3.XmlAssert;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesPropertiesMock;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;

/**
 * Test of {@link XmlMerger}.
 */
class XmlMergerTest extends AbstractIdeContextTest {

  private static final Logger LOG = LoggerFactory.getLogger(XmlMergerTest.class);

  private static final Path XML_TEST_RESOURCES = Path.of("src", "test", "resources", "xmlmerger");

  private static final String SOURCE_XML = "template.xml";

  private static final String TARGET_XML = "target.xml";

  private static final String RESULT_XML = "result.xml";

  /**
   * Tests the XML merger functionality across multiple test cases. This test method iterates through all subdirectories in the test resources folder, each
   * representing a different test case.
   */
  @ParameterizedTest
  @MethodSource("xmlMergerTestCases")
  void testMerger(Path folder, @TempDir Path tempDir) throws Exception {

    // arrange
    LOG.info("Testing XML merger for test-case {}", folder.getFileName());
    Path sourcePath = folder.resolve(SOURCE_XML);
    Path targetPath = tempDir.resolve(TARGET_XML);
    Path resultPath = folder.resolve(RESULT_XML);
    Files.copy(folder.resolve(TARGET_XML), targetPath, REPLACE_EXISTING);
    IdeTestContextMock context = IdeTestContextMock.get();
    EnvironmentVariablesPropertiesMock mockVariables = new EnvironmentVariablesPropertiesMock(null, EnvironmentVariablesType.SETTINGS, context);
    mockVariables.set("JAVA_HOME", "/projects/myproject", false);
    mockVariables.set("JAVA_VERSION", "21", false);
    EnvironmentVariables variables = mockVariables.resolved();
    XmlMerger merger = new XmlMerger(context);
    // act
    int errors = merger.merge(null, sourcePath, variables, targetPath);
    // assert
    XmlAssert.assertThat(targetPath).and(resultPath.toFile()).areIdentical();
    assertThat(errors).isZero();
  }

  private static Stream<Path> xmlMergerTestCases() throws IOException {

    return Files.list(XML_TEST_RESOURCES).filter(Files::isDirectory);
  }

  @Test
  void testFailOnAmbiguousMerge(@TempDir Path tempDir) throws Exception {

    // arrange
    IdeTestContext context = new IdeTestContext();
    EnvironmentVariables variables = context.getVariables();
    variables.getByType(EnvironmentVariablesType.CONF).set("FAIL_ON_AMBIGOUS_MERGE", "true");
    XmlMerger merger = new XmlMerger(context);
    Path folder = XML_TEST_RESOURCES.resolve("ambiguous-id");
    Path sourcePath = folder.resolve(SOURCE_XML);
    Path targetPath = tempDir.resolve(TARGET_XML);
    Files.copy(folder.resolve(TARGET_XML), targetPath, REPLACE_EXISTING);
    // act
    assertThatThrownBy(() -> {
      merger.doMerge(null, sourcePath, variables, targetPath);
    })
        // assert
        .hasRootCauseInstanceOf(IllegalStateException.class).hasRootCauseMessage(
            "2 matches found for XPath configuration[@default='true' and @type='JUnit'] in workspace XML at /project[@version='4']/component[@name='RunManager' @selected='Application.IDEasy']");
    ;

  }

  /**
   * Tests for XML merge of legacy devonfw-ide templates without XML namespace prefix merge.
   */
  @Test
  void testLegacySupportXmlMerge() {

    // arrange
    String projectDevonfwIde = "devonfw-ide";
    IdeTestContext context = newContext(projectDevonfwIde);
    Path devonfwIdePath = TEST_PROJECTS_COPY.resolve(projectDevonfwIde).resolve("project");
    EnvironmentVariables variables = context.getVariables();
    variables.getByType(EnvironmentVariablesType.CONF).set("IDE_XML_MERGE_LEGACY_SUPPORT_ENABLED", "true");
    Path settingsWorkspaceFolder = devonfwIdePath.resolve("settings").resolve("workspace");
    Path settingsSetupPath = settingsWorkspaceFolder.resolve("setup").resolve("setup.xml");
    Path settingsUpdatePath = settingsWorkspaceFolder.resolve("update").resolve("update.xml");
    Path settingsUpdateWithNsPath = settingsWorkspaceFolder.resolve("update").resolve("updateWithNs.xml");
    Path workspaceSetupPath = devonfwIdePath.resolve("workspaces").resolve("main").resolve("setup.xml");
    Path workspaceUpdatePath = devonfwIdePath.resolve("workspaces").resolve("main").resolve("update.xml");
    Path workspaceUpdateWithNsPath = devonfwIdePath.resolve("workspaces").resolve("main").resolve("updateWithNs.xml");
    Path workspaceResultUpdateWithNsPath = devonfwIdePath.resolve("workspaces").resolve("main").resolve("expectedResultUpdateWithNs.xml");
    XmlMerger merger = new XmlMerger(context);

    // act
    merger.doMerge(settingsSetupPath, settingsUpdatePath, variables, workspaceSetupPath);
    merger.doMerge(settingsSetupPath, settingsUpdatePath, variables, workspaceUpdatePath);
    merger.doMerge(settingsSetupPath, settingsUpdateWithNsPath, variables, workspaceUpdateWithNsPath);

    // assert
    XmlAssert.assertThat(settingsSetupPath.toFile()).and(workspaceSetupPath.toFile()).areIdentical();
    XmlAssert.assertThat(settingsUpdatePath.toFile()).and(settingsUpdatePath.toFile()).areIdentical();
    XmlAssert.assertThat(workspaceUpdateWithNsPath.toFile()).and(workspaceResultUpdateWithNsPath.toFile()).areIdentical();

  }

  @Test
  void testLegacySupportWarningWhenEnvNotSetAndNoNamespace() {

    // arrange
    String projectDevonfwIde = "devonfw-ide";
    IdeTestContext context = newContext(projectDevonfwIde);
    Path devonfwIdePath = TEST_PROJECTS_COPY.resolve(projectDevonfwIde).resolve("project");
    EnvironmentVariables variables = context.getVariables();
    Path settingsWorkspaceFolder = devonfwIdePath.resolve("settings").resolve("workspace");
    Path settingsSetupPath = settingsWorkspaceFolder.resolve("setup").resolve("setup.xml");
    Path settingsUpdatePath = settingsWorkspaceFolder.resolve("update").resolve("update.xml");
    Path workspaceSetupPath = devonfwIdePath.resolve("workspaces").resolve("main").resolve("setup.xml");
    XmlMerger merger = new XmlMerger(context);

    // act
    merger.doMerge(settingsSetupPath, settingsUpdatePath, variables, workspaceSetupPath);

    // assert
    assertThat(context).logAtWarning().hasEntries(
        "XML merge namespace not found. If you are working in a legacy devonfw-ide project, please set IDE_XML_MERGE_LEGACY_SUPPORT_ENABLED=true to "
            + "proceed correctly.");
  }

}
