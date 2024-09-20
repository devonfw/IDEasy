package com.devonfw.tools.ide.merge.xmlmerger.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.devonfw.tools.ide.merge.xmlmerger.MergeStrategy;
import com.devonfw.tools.ide.merge.xmlmerger.XmlMerger;

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

  /**
   * @param element the {@link Element} to be represented.
   * @param documentPath the {@link Path} to the document this element belongs to.
   */
  public MergeElement(Element element, Path documentPath) {

    this.element = element;
    this.documentPath = documentPath;
  }

  public Element getElement() {

    return this.element;
  }

  /**
   * @return the path to the document this element belongs to.
   */
  public Path getDocumentPath() {

    return this.documentPath;
  }

  /**
   * @return the {@link MergeStrategy} of this element or {@code null} if undefined.
   */
  public MergeStrategy getMergingStrategy() {

    String strategy = this.element.getAttributeNS(XmlMerger.MERGE_NS_URI, "strategy").toLowerCase();
    if (!strategy.isEmpty()) {
      return MergeStrategy.of(strategy);
    }
    return null;
  }

  /**
   * Retrieves the value of the merge:id attribute of this MergeElement.
   *
   * @return the ID attribute value
   */
  public String getId() {

    String id = this.element.getAttributeNS(XmlMerger.MERGE_NS_URI, "id");
    if (id.isEmpty()) {
      // handle case where element has no attribute
      if (getElementAttributes().isEmpty()) {
        // use name as id
        id = "name()";
      } else {
        // look for id or name attributes
        String idAttr = this.element.getAttribute("id");
        if (idAttr.isEmpty()) {
          idAttr = this.element.getAttribute("name");
          if (!idAttr.isEmpty()) {
            id = "@name";
          }
        } else {
          id = "@id";
        }
      }
    }
    return id;
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

    Node parentNode = this.element.getParentNode();
    if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
      return (Element) parentNode;
    }
    return null;
  }

  /**
   * Retrieves the attributes of this MergeElement.
   *
   * @return a list of {@link MergeAttribute} objects representing the attributes, if there are no attributes, the list is empty.
   */
  public List<MergeAttribute> getElementAttributes() {

    NamedNodeMap attributes = this.element.getAttributes();
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

    return this.element.getParentNode().getNodeType() == Node.DOCUMENT_NODE;
  }

  /**
   * Retrieves the XPath of this MergeElement with no criterion. E.g. /root/.../element
   *
   * @return the XPath
   */
  public String getXPath() {

    StringBuilder xpath = new StringBuilder();
    Node current = this.element;
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
    NodeList nodeList = this.element.getChildNodes();

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        childElements.add(new MergeElement((Element) node, this.documentPath));
      }
    }
    return childElements;
  }
}
