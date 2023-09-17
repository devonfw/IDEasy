package com.devonfw.tools.ide.configurator.merge;

import java.io.File;

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

import com.devonfw.tools.ide.configurator.resolve.VariableResolver;
import com.devonfw.tools.ide.logging.Log;

/**
 * Implementation of {@link FileTypeMerger} for XML.
 *
 * @since 3.0.0
 */
public class XmlMerger extends FileTypeMerger {

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
   */
  public XmlMerger() {

    super();
  }

  @Override
  public void merge(File setupFile, File updateFile, VariableResolver resolver, File workspaceFile) {

    Document document = null;
    boolean updateFileExists = updateFile.exists();
    if (workspaceFile.exists()) {
      if (!updateFileExists) {
        return; // nothing to do ...
      }
      document = load(workspaceFile);
    } else if (setupFile.exists()) {
      document = load(setupFile);
    }
    if (updateFileExists) {
      if (document == null) {
        document = load(updateFile);
      } else {
        Document updateDocument = load(updateFile);
        merge(updateDocument, document, true, true);
      }
    }
    resolve(document, resolver, false);
    save(document, workspaceFile);
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
  public void inverseMerge(File workspaceFile, VariableResolver resolver, boolean addNewProperties, File updateFile) {

    if (!workspaceFile.exists() || !updateFile.exists()) {
      return;
    }
    Document updateDocument = load(updateFile);
    Document workspaceDocument = load(workspaceFile);
    merge(workspaceDocument, updateDocument, true, addNewProperties);
    resolve(updateDocument, resolver, true);
    save(updateDocument, updateFile);
    Log.debug("Saved changes in " + workspaceFile.getName() + " to: " + updateFile.getAbsolutePath());
  }

  /**
   * @param file the {@link File} to load.
   * @return the loaded XML {@link Document}.
   */
  public static Document load(File file) {

    try {
      return DOCUMENT_BUILDER.parse(file);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load XML from: " + file, e);
    }
  }

  /**
   * @param document the XML {@link Document} to save.
   * @param file the {@link File} to save to.
   */
  public static void save(Document document, File file) {

    ensureParentDirecotryExists(file);
    try {
      Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
      DOMSource source = new DOMSource(document);
      StreamResult result = new StreamResult(file);
      transformer.transform(source, result);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to save XML to file: " + file, e);
    }

  }

  private void resolve(Document document, VariableResolver resolver, boolean inverse) {

    NodeList nodeList = document.getElementsByTagName("*");
    for (int i = 0; i < nodeList.getLength(); i++) {
      Element element = (Element) nodeList.item(i);
      resolve(element, resolver, inverse);
    }
  }

  private void resolve(Element element, VariableResolver resolver, boolean inverse) {

    resolve(element.getAttributes(), resolver, inverse);
    NodeList nodeList = element.getChildNodes();

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node instanceof Text) {
        Text text = (Text) node;
        String value = text.getNodeValue();
        String resolvedValue;
        if (inverse) {
          resolvedValue = resolver.inverseResolve(value);
        } else {
          resolvedValue = resolver.resolve(value);
        }
        text.setNodeValue(resolvedValue);
      }
    }
  }

  private void resolve(NamedNodeMap attributes, VariableResolver resolver, boolean inverse) {

    for (int i = 0; i < attributes.getLength(); i++) {
      Attr attribute = (Attr) attributes.item(i);
      String value = attribute.getValue();
      String resolvedValue;
      if (inverse) {
        resolvedValue = resolver.inverseResolve(value);
      } else {
        resolvedValue = resolver.resolve(value);
      }
      attribute.setValue(resolvedValue);
    }
  }

}
