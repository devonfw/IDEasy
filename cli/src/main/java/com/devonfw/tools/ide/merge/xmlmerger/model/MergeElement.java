package com.devonfw.tools.ide.merge.xmlmerger.model;

import com.devonfw.tools.ide.merge.xmlmerger.XmlMerger;
import com.devonfw.tools.ide.merge.xmlmerger.strategy.MergeStrategy;
import org.w3c.dom.*;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents an XML element during the merge process.
 */
public class MergeElement {
  private final Element element;

  public MergeElement(Element element) {

    this.element = element;
  }

  public Element getElement() {

    return element;
  }

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
      return new MergeElement(parent).getMergingStrategy();
    }

    return MergeStrategy.KEEP; // Default strategy
  }

  public String getId() {

    String id =  this.element.getAttributeNS(XmlMerger.MERGE_NS_URI, "id");
    if (!id.isEmpty()) {
      return id;
    }
    id = element.getAttribute("id");
    if (!id.isEmpty()) {
      return "@" + id;
    }
    return null;
  }


  public QName getQName() {

    String namespaceURI = this.element.getNamespaceURI();
    String localName = this.element.getLocalName();
    return new QName(namespaceURI, localName);
  }



  private Element getParentElement() {

    Node parentNode = element.getParentNode();
    if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
      return (Element) parentNode;
    }
    return null;
  }

  public List<MergeAttribute> getElementAttributes() {
    
    NamedNodeMap attributes = element.getAttributes();
    List<MergeAttribute> attributeList = new ArrayList<>();
    for (int i = 0; i < attributes.getLength(); i++) {
      attributeList.add(new MergeAttribute((Attr) attributes.item(i)));
    }
    return attributeList;
  }

  public boolean isRootElement() {

    return element.getParentNode().getNodeType() == Node.DOCUMENT_NODE;
  }

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

  public List<MergeElement> getChildElements() {

    List<MergeElement> childElements = new ArrayList<>();
    NodeList nodeList = element.getChildNodes();

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        childElements.add(new MergeElement((Element) node));
      }
    }

    return childElements;
  }

}