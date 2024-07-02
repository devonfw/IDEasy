package com.devonfw.tools.ide.merge;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.XmlMerger;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class XmlMergerTest extends AbstractIdeContextTest {

  private static final Path TEST_RESOURCES = Path.of("src", "test", "resources", "xmlmerger");

  private static final String SOURCE_XML = "source.xml";

  private static final String TARGET_XML = "target.xml";

  private static final String RESULT_XML = "result.xml";

  private IdeContext context = newContext(PROJECT_BASIC, null, false);

  private XmlMerger merger = new XmlMerger(context);

  @Test
  void testAllCases(@TempDir Path tempDir) throws Exception {

    try(Stream<Path> folders = Files.list(TEST_RESOURCES)) {
      // arrange
      SoftAssertions softly = new SoftAssertions();
      folders.forEach(folder -> {
        Path sourcePath = folder.resolve(SOURCE_XML);
        Path targetPath = tempDir.resolve(TARGET_XML);
        Path resultPath = folder.resolve(RESULT_XML);
        try {
          Files.copy(folder.resolve(TARGET_XML), targetPath, REPLACE_EXISTING);
          // act
          merger.merge(null, sourcePath, context.getVariables(), targetPath);
          // assert
          softly.assertThat(targetPath).hasContent(Files.readString(resultPath));
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      });
      softly.assertAll();
    }
  }

  @Test
  void test(@TempDir Path tempDir) throws Exception {

    Path folder = TEST_RESOURCES.resolve("namespace");
    Path sourcePath = folder.resolve(SOURCE_XML);
    Path targetPath = folder.resolve(TARGET_XML);
    Path resultPath = folder.resolve(RESULT_XML);
    merger.merge(null, sourcePath, context.getVariables(), targetPath);
  }
}