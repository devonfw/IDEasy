package com.devonfw.tools.ide.merge;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlunit.assertj3.XmlAssert;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesPropertiesMock;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.merge.xmlmerger.XmlMerger;

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
}
