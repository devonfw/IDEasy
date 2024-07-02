package com.devonfw.tools.ide.merge.xmlmerger.model;

import com.devonfw.tools.ide.merge.xmlmerger.XmlMerger;
import com.devonfw.tools.ide.merge.xmlmerger.strategy.MergeStrategy;
import org.w3c.dom.*;

import javax.xml.namespace.QName;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an XML element during the merge process.
 */
public class MergeElement {

  /**
   * The XML element represented by this MergeElement.
   */
  private final Element element;

  /**
   * The path of the document where this element resides.
   */
  private final Path documentPath;

  public MergeElement(Element element, Path documentPath) {

    this.element = element;
    this.documentPath = documentPath;
  }

  public Element getElement() {

    return element;
  }

  public Path getDocumentPath() {

    return documentPath;
  }

  /**
   * Retrieves the merge strategy associated with this MergeElement.
   *
   * @return the merge strategy type
   */
  public MergeStrategy getMergingStrategy() {

    String strategy = this.element.getAttributeNS(XmlMerger.MERGE_NS_URI, "strategy").toLowerCase();
    if ("combine".equals(strategy)) {
      return MergeStrategy.COMBINE;
    } else if ("override".equals(strategy)) {
      return MergeStrategy.OVERRIDE;
    } else if ("keep".equals(strategy)) {
      return MergeStrategy.KEEP;
    }

    // Inherit merging strategy from parent
    Element parent = getParentElement();
    if (parent != null) {
      return new MergeElement(parent, this.documentPath).getMergingStrategy();
    }

    return MergeStrategy.KEEP; // Default strategy
  }

  /**
   * Retrieves the value of the merge:id attribute of this MergeElement.
   *
   * @return the ID attribute value
   */
  public String getId() {

    return this.element.getAttributeNS(XmlMerger.MERGE_NS_URI, "id");
  }

  /**
   * Retrieves the qualified name (URI + local name) of this MergeElement.
   *
   * @return the QName
   */
  public QName getQName() {

    String namespaceURI = this.element.getNamespaceURI();
    String localName = this.element.getLocalName();
    return new QName(namespaceURI, localName);
  }

  /**
   * Retrieves the parent element of this MergeElement.
   *
   * @return the parent element, or {@code null} if there is no parent
   */
  private Element getParentElement() {

    Node parentNode = element.getParentNode();
    if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
      return (Element) parentNode;
    }
    return null;
  }

  /**
   * Retrieves the attributes of this MergeElement.
   *
   * @return a list of {@link MergeAttribute} objects representing the attributes, if there are no attributes, the list
   * is empty.
   */
  public List<MergeAttribute> getElementAttributes() {

    NamedNodeMap attributes = element.getAttributes();
    List<MergeAttribute> attributeList = new ArrayList<>();
    for (int i = 0; i < attributes.getLength(); i++) {
      attributeList.add(new MergeAttribute((Attr) attributes.item(i)));
    }
    return attributeList;
  }

  /**
   * Checks if this MergeElement is a root element.
   *
   * @return {@code true} if this element is a root element, {@code false} otherwise
   */
  public boolean isRootElement() {

    return element.getParentNode().getNodeType() == Node.DOCUMENT_NODE;
  }

  /**
   * Removes merge namespace attributes from this MergeElement.
   */
  public void removeMergeNsAttributes() {

    List<MergeAttribute> attributes = getElementAttributes();
    try {
      for (MergeAttribute attribute : attributes) {
        if (attribute.isMergeNSAttr()) {
          element.removeAttributeNode(attribute.getAttr());
        }
      }
    } catch (DOMException e) {
      throw new IllegalStateException("Failed to remove merge namespace attributes for element:" + getXPath(), e);
    }
  }

  /**
   * Retrieves the XPath of this MergeElement with no criterion. E.g. /root/.../element
   *
   * @return the XPath
   */
  public String getXPath() {

    StringBuilder xpath = new StringBuilder();
    Node current = element;
    while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
      Element currentElement = (Element) current;
      String tagName = currentElement.getTagName();
      xpath.insert(0, "/" + tagName);
      current = current.getParentNode();
    }
    return xpath.toString();
  }

  /**
   * Retrieves the child elements of this MergeElement.
   *
   * @return a list of {@link MergeElement} objects representing the child elements
   */
  public List<MergeElement> getChildElements() {

    List<MergeElement> childElements = new ArrayList<>();
    NodeList nodeList = element.getChildNodes();

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        childElements.add(new MergeElement((Element) node, this.documentPath));
      }
    }
    return childElements;
  }
}
