package com.devonfw.tools.ide.merge;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.XmlMerger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class XmlMergerTest extends AbstractIdeContextTest {

  private static final Path TEST_DIR = Path.of("C:\\Users\\saboucha\\Desktop\\tests1");

  private IdeContext context = newContext(PROJECT_BASIC, null, false);

  private XmlMerger merger = new XmlMerger(context);

  @Test
  public void combine() {

    // arrange
    Path xmlFiles = TEST_DIR.resolve("combine");
    Path update = xmlFiles.resolve("template.xml");
    Path workspace = xmlFiles.resolve("workspace.xml");
    Path result = xmlFiles.resolve("result.xml");

    // act
    merger.merge(null, update, context.getVariables(), workspace);

    // assert
    assertThat(workspace).hasContent(xmlToString(result));
  }

  @Test
  public void override() {

    // arrange
    Path xmlFiles = TEST_DIR.resolve("override");
    Path update = xmlFiles.resolve("template.xml");
    Path workspace = xmlFiles.resolve("workspace.xml");
    Path result = xmlFiles.resolve("result.xml");

    // act
    merger.merge(null, update, context.getVariables(), workspace);

    // assert
    assertThat(workspace).hasContent(xmlToString(result));
  }



  public static String xmlToString(Path xmlFilePath) {

    // Read content of the XML file
    try {
      byte[] bytes = Files.readAllBytes(xmlFilePath);
      return new String(bytes);
    } catch (IOException e) {
      throw new IllegalStateException("Could not read xml file: " + xmlFilePath, e);
    }
  }

}