package com.devonfw.tools.ide.merge;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.XmlMerger;

class XmlMergerTest extends AbstractIdeContextTest {

  private static Logger LOG = LoggerFactory.getLogger(XmlMergerTest.class);

  private static final Path TEST_RESOURCES = Path.of("src", "test", "resources", "xmlmerger");

  private static final String SOURCE_XML = "source.xml";

  private static final String TARGET_XML = "target.xml";

  private static final String RESULT_XML = "result.xml";

  private IdeContext context = newContext(PROJECT_BASIC, null, false);

  private XmlMerger merger = new XmlMerger(this.context);

  /**
   * Tests the XML merger functionality across multiple test cases. This test method iterates through all subdirectories in the test resources folder, each
   * representing a different test case.
   */
  @Test
  void testMerger(@TempDir Path tempDir) throws Exception {

    try (Stream<Path> folders = Files.list(TEST_RESOURCES)) {
      // arrange
      SoftAssertions softly = new SoftAssertions();
      folders.forEach(folder -> {
        LOG.info("Testing XML merger for test-case {}", folder.getFileName());
        Path sourcePath = folder.resolve(SOURCE_XML);
        Path targetPath = tempDir.resolve(TARGET_XML);
        Path resultPath = folder.resolve(RESULT_XML);
        try {
          Files.copy(folder.resolve(TARGET_XML), targetPath, REPLACE_EXISTING);
          // act
          this.merger.merge(null, sourcePath, this.context.getVariables(), targetPath);
          // assert
          softly.assertThat(targetPath).hasContent(Files.readString(resultPath));
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      });
      softly.assertAll();
    }
  }
}
