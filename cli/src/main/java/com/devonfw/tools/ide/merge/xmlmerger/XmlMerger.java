package com.devonfw.tools.ide.merge.xmlmerger;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.merge.FileMerger;
import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import com.devonfw.tools.ide.merge.xmlmerger.strategy.Strategy;
import com.devonfw.tools.ide.merge.xmlmerger.strategy.StrategyFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class XmlMerger extends FileMerger {

  private final ElementMatcher elementMatcher;
  private static final DocumentBuilder DOCUMENT_BUILDER;
  private static final TransformerFactory TRANSFORMER_FACTORY;
  private final StrategyFactory strategyFactory;



  static {
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      DOCUMENT_BUILDER = documentBuilderFactory.newDocumentBuilder();
      TRANSFORMER_FACTORY = TransformerFactory.newInstance();
    } catch (Exception e) {
      throw new IllegalStateException("Invalid XML DOM support in JDK.", e);
    }
  }

  public XmlMerger(IdeContext context) {

    super(context);
    this.elementMatcher = new ElementMatcher();
    this.strategyFactory = new StrategyFactory(context, elementMatcher);
  }

  @Override
  public void merge(Path setup, Path update, EnvironmentVariables resolver, Path workspace) {

    Document document = null;
    boolean updateFileExists = Files.exists(update);
    if (Files.exists(workspace)) {
      if (!updateFileExists) {
        return; // nothing to do ...
      }
      document = load(workspace);
    } else if (Files.exists(setup)) {
      document = load(setup);
    }
    if (updateFileExists) {
      if (document == null) {
        document = load(update);
      } else {
        Document updateDocument = load(update);
        merge(updateDocument, document);
      }
    }
    resolve(document, resolver, false, workspace.getFileName());
    save(document, workspace);
  }

  public void merge(Document sourceDocument, Document targetDocument) {

    MergeElement updateRootElement = new MergeElement(sourceDocument.getDocumentElement());
    Strategy strategy = strategyFactory.createStrategy(updateRootElement.getMergingStrategy());
    strategy.merge(updateRootElement, targetDocument);
  }
  @Override
  public void inverseMerge(Path workspace, EnvironmentVariables variables, boolean addNewProperties, Path update) {

  }

  public Document load(Path file) {

    try (InputStream in = Files.newInputStream(file)) {
      return DOCUMENT_BUILDER.parse(in);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load XML from: " + file, e);
    }
  }
  public void save(Document document, Path file) {
    ensureParentDirectoryExists(file);
    try {
      Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

      // Remove whitespace from the target document before merging
      removeWhitespace(document.getDocumentElement());

      DOMSource source = new DOMSource(document);
      StreamResult result = new StreamResult(file.toFile());
      transformer.transform(source, result);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to save XML to file: " + file, e);
    }
  }

  private void removeWhitespace(Node node) {
    NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.TEXT_NODE) {
        if (child.getTextContent().trim().isEmpty()) {
          node.removeChild(child);
          i--;
        }
      } else {
        removeWhitespace(child);
      }
    }
  }

  private void resolve(Document document, EnvironmentVariables resolver, boolean inverse, Object src) {

    NodeList nodeList = document.getElementsByTagName("*");
    for (int i = 0; i < nodeList.getLength(); i++) {
      Element element = (Element) nodeList.item(i);
      resolve(element, resolver, inverse, src);
    }
  }

  private void resolve(Element element, EnvironmentVariables variables, boolean inverse, Object src) {

    resolve(element.getAttributes(), variables, inverse, src);
    NodeList nodeList = element.getChildNodes();

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node instanceof Text text) {
        String value = text.getNodeValue();
        String resolvedValue;
        if (inverse) {
          resolvedValue = variables.inverseResolve(value, src);
        } else {
          resolvedValue = variables.resolve(value, src);
        }
        text.setNodeValue(resolvedValue);
      }
    }
  }

  private void resolve(NamedNodeMap attributes, EnvironmentVariables variables, boolean inverse, Object src) {

    for (int i = 0; i < attributes.getLength(); i++) {
      Attr attribute = (Attr) attributes.item(i);
      String value = attribute.getValue();
      String resolvedValue;
      if (inverse) {
        resolvedValue = variables.inverseResolve(value, src);
      } else {
        resolvedValue = variables.resolve(value, src);
      }
      attribute.setValue(resolvedValue);
    }
  }

}
