package com.devonfw.tools.ide.merge;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.XmlMerger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlMergerTest extends AbstractIdeContextTest {

  private static final Path TEST_RESOURCES = Path.of("src", "test", "resources", "xmlmerger");

  public static final String SOURCE_XML = "source.xml";

  public static final String TARGET_XML = "target.xml";

  public static final String RESULT_XML = "result.xml";

  private IdeContext context = newContext(PROJECT_BASIC, null, false);

  private XmlMerger merger = new XmlMerger(context);

  @Test
  void testMergeStrategyCombine(@TempDir Path tempDir) throws Exception {

    // arrange
    Path folderPath = TEST_RESOURCES.resolve("combine");
    Path sourcePath = folderPath.resolve(SOURCE_XML);
    Path targetPath = tempDir.resolve(TARGET_XML);
    Path resultPath = folderPath.resolve(RESULT_XML);
    Files.copy(folderPath.resolve(TARGET_XML), targetPath);

    // act
    merger.merge(null, sourcePath, context.getVariables(), targetPath);

    // assert
    assertThat(targetPath).hasContent(Files.readString(resultPath));
  }

  @Test
  void testMergeStrategyOverride(@TempDir Path tempDir) throws Exception {

    // arrange
    Path folderPath = TEST_RESOURCES.resolve("override");
    Path sourcePath = folderPath.resolve(SOURCE_XML);
    Path targetPath = tempDir.resolve(TARGET_XML);
    Path resultPath = folderPath.resolve(RESULT_XML);
    Files.copy(folderPath.resolve(TARGET_XML), targetPath);

    // act
    merger.merge(null, sourcePath, context.getVariables(), targetPath);

    // assert
    assertThat(targetPath).hasContent(Files.readString(resultPath));
  }

  @Test
  void testMergeStrategyKeep(@TempDir Path tempDir) throws Exception {

    // arrange
    Path folderPath = TEST_RESOURCES.resolve("keep");
    Path sourcePath = folderPath.resolve(SOURCE_XML);
    Path targetPath = tempDir.resolve(TARGET_XML);
    Path resultPath = folderPath.resolve(RESULT_XML);
    Files.copy(folderPath.resolve(TARGET_XML), targetPath);

    // act
    merger.merge(null, sourcePath, context.getVariables(), targetPath);

    // assert
    assertThat(targetPath).hasContent(Files.readString(resultPath));
  }

  @Test
  void testMergeStrategyAppend(@TempDir Path tempDir) throws Exception {


    // arrange
    Path folderPath = TEST_RESOURCES.resolve("append");
    Path sourcePath = folderPath.resolve(SOURCE_XML);
    Path targetPath = tempDir.resolve(TARGET_XML);
    Path resultPath = folderPath.resolve(RESULT_XML);
    Files.copy(folderPath.resolve(TARGET_XML), targetPath);

    // act
    merger.merge(null, sourcePath, context.getVariables(), targetPath);

    // assert
    assertThat(targetPath).hasContent(Files.readString(resultPath));
  }

  @Test
  void testMergeStrategyId(@TempDir Path tempDir) throws Exception {

    // arrange
    Path folderPath = TEST_RESOURCES.resolve("id");
    Path sourcePath = folderPath.resolve(SOURCE_XML);
    Path targetPath = tempDir.resolve(TARGET_XML);
    Path resultPath = folderPath.resolve(RESULT_XML);
    Files.copy(folderPath.resolve(TARGET_XML), targetPath);

    // act
    merger.merge(null, sourcePath, context.getVariables(), targetPath);

    // assert
    assertThat(targetPath).hasContent(Files.readString(resultPath));
  }

  @Test
  void testMergeStrategyCombineNested(@TempDir Path tempDir) throws Exception {

    // arrange
    Path folderPath = TEST_RESOURCES.resolve("combineNested");
    Path sourcePath = folderPath.resolve(SOURCE_XML);
    Path targetPath = tempDir.resolve(TARGET_XML);
    Path resultPath = folderPath.resolve(RESULT_XML);
    Files.copy(folderPath.resolve(TARGET_XML), targetPath);

    // act
    merger.merge(null, sourcePath, context.getVariables(), targetPath);

    // assert
    assertThat(targetPath).hasContent(Files.readString(resultPath));
  }

  @Test
  void testMergeStrategyOverrideNested(@TempDir Path tempDir) throws Exception {

    // arrange
    Path folderPath = TEST_RESOURCES.resolve("overrideNested");
    Path sourcePath = folderPath.resolve(SOURCE_XML);
    Path targetPath = tempDir.resolve(TARGET_XML);
    Path resultPath = folderPath.resolve(RESULT_XML);
    Files.copy(folderPath.resolve(TARGET_XML), targetPath);

    // act
    merger.merge(null, sourcePath, context.getVariables(), targetPath);

    // assert
    assertThat(targetPath).hasContent(Files.readString(resultPath));
  }
}