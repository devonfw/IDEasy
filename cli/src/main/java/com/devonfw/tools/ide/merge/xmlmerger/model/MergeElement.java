package com.devonfw.tools.ide.merge.xmlmerger.model;

import com.devonfw.tools.ide.merge.xmlmerger.annotation.MergeAnnotation;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.List;

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

    MergeStrategy mergeStrategy = MergeAnnotation.getMergeStrategy(element);
    if (mergeStrategy != null) {
      return mergeStrategy;
    }

    // Inherit merging strategy from parent
    Element parent = getParentElement();
    if (parent != null) {
      return new MergeElement(parent).getMergingStrategy();
    }

    return MergeStrategy.KEEP; // Default strategy
  }

  public String getId() {

    String mergeId = MergeAnnotation.getMergeId(element);
    if (!mergeId.isEmpty()) {
      return mergeId;
    }

    String parentChildrenId = getParentMergeChildrenId();
    if (parentChildrenId != null) {
      return parentChildrenId;
    }

    String idAttr = element.getAttribute("id");
    if (!idAttr.isEmpty()) {
      return idAttr;
    }

    Element parent = getParentElement();
    if (parent != null) {
      return new MergeElement(parent).getId();
    }

    return null;
  }

  private String getParentMergeChildrenId() {

    Element parent = getParentElement();
    if (parent != null) {
      String childrenId = MergeAnnotation.getMergeChildrenId(element);
      if (!childrenId.isEmpty()) {
        return childrenId;
      }
      return new MergeElement(parent).getParentMergeChildrenId();
    }
    return null;
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

  public void removeMergeNSAttributes() {

    List<MergeAttribute> attributes = getElementAttributes();
    for (MergeAttribute attribute : attributes) {
      if (attribute.isMergeNSAttr()) {
        element.removeAttributeNode(attribute.getAttr());
      }
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
}