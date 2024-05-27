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

  IdeContext context = newContext(PROJECT_BASIC, null, false);

  private XmlMerger merger = new XmlMerger(context);

  @Test
  void testMergeStrategyCombine(@TempDir Path tempDir) throws Exception {

    Path folderPath = TEST_RESOURCES.resolve("combine");
    Path sourcePath = folderPath.resolve("source.xml");
    Path targetPath = tempDir.resolve("target.xml");
    Path resultPath = folderPath.resolve("result.xml");

    Files.copy(folderPath.resolve("target.xml"), targetPath);

    merger.merge(null, sourcePath, context.getVariables(), targetPath);

    Document actualDocument = loadDocument(targetPath);
    Document expectedDocument = loadDocument(resultPath);

    assertTrue(compareDocuments(actualDocument, expectedDocument), "XML documents are not equal");
  }

  @Test
  void testMergeStrategyOverride(@TempDir Path tempDir) throws Exception {

    Path folderPath = TEST_RESOURCES.resolve("override");
    Path sourcePath = folderPath.resolve("source.xml");
    Path targetPath = tempDir.resolve("target.xml");
    Path resultPath = folderPath.resolve("result.xml");

    Files.copy(folderPath.resolve("target.xml"), targetPath);

    merger.merge(null, sourcePath, context.getVariables(), targetPath);

    Document actualDocument = loadDocument(targetPath);
    Document expectedDocument = loadDocument(resultPath);

    assertTrue(compareDocuments(actualDocument, expectedDocument), "XML documents are not equal");
  }

  @Test
  void testMergeStrategyKeep(@TempDir Path tempDir) throws Exception {

    Path folderPath = TEST_RESOURCES.resolve("keep");
    Path sourcePath = folderPath.resolve("source.xml");
    Path targetPath = tempDir.resolve("target.xml");
    Path resultPath = folderPath.resolve("result.xml");

    Files.copy(folderPath.resolve("target.xml"), targetPath);

    merger.merge(null, sourcePath, context.getVariables(), targetPath);

    Document actualDocument = loadDocument(targetPath);
    Document expectedDocument = loadDocument(resultPath);

    assertTrue(compareDocuments(actualDocument, expectedDocument), "XML documents are not equal");
  }

  @Test
  void testMergeStrategyAppend(@TempDir Path tempDir) throws Exception {

    Path folderPath = TEST_RESOURCES.resolve("append");
    Path sourcePath = folderPath.resolve("source.xml");
    Path targetPath = tempDir.resolve("target.xml");
    Path resultPath = folderPath.resolve("result.xml");

    Files.copy(folderPath.resolve("target.xml"), targetPath);

    merger.merge(null, sourcePath, context.getVariables(), targetPath);

    Document actualDocument = loadDocument(targetPath);
    Document expectedDocument = loadDocument(resultPath);

    assertTrue(compareDocuments(actualDocument, expectedDocument), "XML documents are not equal");
  }

  @Test
  void testMergeStrategyId(@TempDir Path tempDir) throws Exception {

    Path folderPath = TEST_RESOURCES.resolve("id");
    Path sourcePath = folderPath.resolve("source.xml");
    Path targetPath = tempDir.resolve("target.xml");
    Path resultPath = folderPath.resolve("result.xml");

    Files.copy(folderPath.resolve("target.xml"), targetPath);

    merger.merge(null, sourcePath, context.getVariables(), targetPath);

    Document actualDocument = loadDocument(targetPath);
    Document expectedDocument = loadDocument(resultPath);

    assertTrue(compareDocuments(actualDocument, expectedDocument), "XML documents are not equal");
  }

  @Test
  void testMergeStrategyCombineNested(@TempDir Path tempDir) throws Exception {

    Path folderPath = TEST_RESOURCES.resolve("combineNested");
    Path sourcePath = folderPath.resolve("source.xml");
    Path targetPath = tempDir.resolve("target.xml");
    Path resultPath = folderPath.resolve("result.xml");

    Files.copy(folderPath.resolve("target.xml"), targetPath);

    merger.merge(null, sourcePath, context.getVariables(), targetPath);

    Document actualDocument = loadDocument(targetPath);
    Document expectedDocument = loadDocument(resultPath);

    assertTrue(compareDocuments(actualDocument, expectedDocument), "XML documents are not equal");
  }

  @Test
  void testMergeStrategyOverrideNested(@TempDir Path tempDir) throws Exception {

    Path folderPath = TEST_RESOURCES.resolve("overrideNested");
    Path sourcePath = folderPath.resolve("source.xml");
    Path targetPath = tempDir.resolve("target.xml");
    Path resultPath = folderPath.resolve("result.xml");

    Files.copy(folderPath.resolve("target.xml"), targetPath);

    merger.merge(null, sourcePath, context.getVariables(), targetPath);

    Document actualDocument = loadDocument(targetPath);
    Document expectedDocument = loadDocument(resultPath);

    assertTrue(compareDocuments(actualDocument, expectedDocument), "XML documents are not equal");
  }

  private Document loadDocument(Path path) throws Exception {

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    try (InputStream is = Files.newInputStream(path)) {
      return builder.parse(is);
    }
  }

  private boolean compareDocuments(Document doc1, Document doc2) {

    return compareNodes(doc1.getDocumentElement(), doc2.getDocumentElement());
  }

  private boolean compareNodes(Node node1, Node node2) {

    if (node1.getNodeType() != node2.getNodeType()) {
      assertEquals(node1.getNodeType(), node2.getNodeType(), "Node types are different");
      return false;
    }

    if (node1.getNodeType() == Node.TEXT_NODE) {
      String value1 = node1.getNodeValue().trim();
      String value2 = node2.getNodeValue().trim();
      assertEquals(value1, value2, "Text content is different");
      return true;
    }

    if (node1.getNodeType() == Node.ELEMENT_NODE) {
      String nodeName1 = node1.getNodeName();
      String nodeName2 = node2.getNodeName();
      assertEquals(nodeName1, nodeName2, "Element names are different");

      if (!compareAttributes(node1.getAttributes(), node2.getAttributes())) {
        return false;
      }

      NodeList childNodes1 = node1.getChildNodes();
      NodeList childNodes2 = node2.getChildNodes();

      assertEquals(childNodes1.getLength(), childNodes2.getLength(),
          "Number of child nodes of " + node1.getNodeName() + " and " + node2.getNodeName() + " is different");

      for (int i = 0; i < childNodes1.getLength(); i++) {
        if (!compareNodes(childNodes1.item(i), childNodes2.item(i))) {
          return false;
        }
      }

      return true;
    }

    return false;
  }

  private boolean compareAttributes(NamedNodeMap attrs1, NamedNodeMap attrs2) {

    assertEquals(attrs1.getLength(), attrs2.getLength(), "Number of attributes is different");

    for (int i = 0; i < attrs1.getLength(); i++) {
      Node attr1 = attrs1.item(i);
      Node attr2 = attrs2.getNamedItem(attr1.getNodeName());

      if (attr2 == null || !attr1.getNodeValue().equals(attr2.getNodeValue())) {
        assertEquals(attr2 != null ? attr2.getNodeValue() : "", attr1.getNodeValue(),
            "Attribute \"" + attr1.getNodeName() + "\" value is different");
        return false;
      }
    }

    return true;
  }
}