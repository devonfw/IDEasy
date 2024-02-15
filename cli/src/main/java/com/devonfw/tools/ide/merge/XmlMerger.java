package com.devonfw.tools.ide.merge;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;

/**
 * Implementation of {@link FileMerger} for XML files.
 */
public class XmlMerger extends FileMerger {

  private static final DocumentBuilder DOCUMENT_BUILDER;

  private static final TransformerFactory TRANSFORMER_FACTORY;

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

  /**
   * The constructor.
   *
   * @param context the {@link #context}.
   */
  public XmlMerger(IdeContext context) {

    super(context);
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
        merge(updateDocument, document, true, true);
      }
    }
    resolve(document, resolver, false, workspace.getFileName());
    save(document, workspace);
  }

  private void merge(Document sourceDocument, Document targetDocument, boolean override, boolean add) {

    assert (override || add);
    merge(sourceDocument.getDocumentElement(), targetDocument.getDocumentElement(), override, add);
  }

  private void merge(Element sourceElement, Element targetElement, boolean override, boolean add) {

    merge(sourceElement.getAttributes(), targetElement, override, add);
    NodeList sourceChildNodes = sourceElement.getChildNodes();
    int length = sourceChildNodes.getLength();
    for (int i = 0; i < length; i++) {
      Node child = sourceChildNodes.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {

      } else if (child.getNodeType() == Node.TEXT_NODE) {

      } else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {

      }
    }
  }

  private void merge(NamedNodeMap sourceAttributes, Element targetElement, boolean override, boolean add) {

    int length = sourceAttributes.getLength();
    for (int i = 0; i < length; i++) {
      Attr sourceAttribute = (Attr) sourceAttributes.item(i);
      String namespaceURI = sourceAttribute.getNamespaceURI();
      // String localName = sourceAttribute.getLocalName();
      String name = sourceAttribute.getName();
      Attr targetAttribute = targetElement.getAttributeNodeNS(namespaceURI, name);
      if (targetAttribute == null) {
        if (add) {
          // ridiculous but JDK does not provide namespace support by default...
          targetElement.setAttributeNS(namespaceURI, name, sourceAttribute.getValue());
          // targetElement.setAttribute(name, sourceAttribute.getValue());
        }
      } else if (override) {
        targetAttribute.setValue(sourceAttribute.getValue());
      }
    }
  }

  @Override
  public void inverseMerge(Path workspace, EnvironmentVariables variables, boolean addNewProperties, Path update) {

    if (!Files.exists(workspace) || !Files.exists(update)) {
      return;
    }
    Document updateDocument = load(update);
    Document workspaceDocument = load(workspace);
    merge(workspaceDocument, updateDocument, true, addNewProperties);
    resolve(updateDocument, variables, true, workspace.getFileName());
    save(updateDocument, update);
    this.context.debug("Saved changes in {} to {}", workspace.getFileName(), update);
  }

  /**
   * @param file the {@link Path} to load.
   * @return the loaded XML {@link Document}.
   */
  public static Document load(Path file) {

    try (InputStream in = Files.newInputStream(file)) {
      return DOCUMENT_BUILDER.parse(in);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load XML from: " + file, e);
    }
  }

  /**
   * @param document the XML {@link Document} to save.
   * @param file the {@link Path} to save to.
   */
  public static void save(Document document, Path file) {

    ensureParentDirectoryExists(file);
    try {
      Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
      DOMSource source = new DOMSource(document);
      StreamResult result = new StreamResult(file.toFile());
      transformer.transform(source, result);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to save XML to file: " + file, e);
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
