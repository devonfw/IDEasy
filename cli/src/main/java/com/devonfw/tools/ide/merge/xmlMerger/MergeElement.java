package com.devonfw.tools.ide.merge.xmlMerger;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

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
    // Inherit merging strategy from parent if not explicitly specified
    Element parent = (Element) element.getParentNode();
    if (parent != null) {
      return new MergeElement(parent).getMergingStrategy();
    }
    return null;
  }

  public String getId() {
    // priority: merge:id -> parent's childrenId -> id attribute -> inherited merge:id
    String mergeId = MergeAnnotation.getMergeId(element);
    if (!mergeId.isEmpty()) {
      return mergeId;
    }

    String parentChildrenId = getChildrenId();
    if (parentChildrenId != null) {
      return parentChildrenId;
    }

    String idAttr = element.getAttribute("id"); // use Annotation for this
    if (!idAttr.isEmpty()) {
      return idAttr;
    }

    Element parent = (Element) element.getParentNode();
    if (parent != null) {
      return new MergeElement(parent).getId();
    }
    return null;
  }

  private String getChildrenId() {

    Node parentNode = element.getParentNode();
    if (parentNode == null || parentNode.getNodeType() != Node.ELEMENT_NODE) { // root's parent is document, cant be cast to elem
      return null;
    }
    Element parent = (Element) parentNode;
    String childrenId = MergeAnnotation.getMergeChildrenId(element);
    if (!childrenId.isEmpty()) {
      return childrenId;
    }
    return new MergeElement(parent).getChildrenId();
  }

  public List<MergeAttribute> getAttributes() {
    NamedNodeMap attributes = element.getAttributes();
    List<MergeAttribute> attributeList = new ArrayList<>();
    for (int i = 0; i < attributes.getLength(); i++) {
      attributeList.add(new MergeAttribute((org.w3c.dom.Attr) attributes.item(i)));
    }
    return attributeList;
  }

  public List<MergeElement> getChildElements() {

    NodeList childNodes = element.getChildNodes();
    List<MergeElement> childElements = new ArrayList<>();
    for (int i = 0; i < childNodes.getLength(); i++) {
      if (childNodes.item(i) instanceof Element element) {
        childElements.add(new MergeElement(element));
      }
    }
    return childElements;
  }

  public void removeMergeNSAttributes() {

    List<MergeAttribute> attributes = getAttributes();
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
