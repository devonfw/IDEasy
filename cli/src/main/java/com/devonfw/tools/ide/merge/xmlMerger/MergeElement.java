package com.devonfw.tools.ide.merge.xmlMerger;

import org.w3c.dom.Element;

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

    // priority for now: merge:id -> inherited merge:id -> id attribute
    String mergeId = MergeAnnotation.getIdExpression(element);
    if (!mergeId.isEmpty()) {
      return mergeId;
    }
    Element parent = (Element) element.getParentNode();
    if (parent != null) {
      return new MergeElement(parent).getId();
    }
    String idAttr = element.getAttribute("id"); // returns empty string
    if (!idAttr.isEmpty()) {
      return idAttr;
    }
    return null;
  }

  public String getChildrenId() {
    String childrenId = MergeAnnotation.getChildrenIdExpression(element);
    if (!childrenId.isEmpty()) {
      return childrenId;
    }
    Element parent = (Element) element.getParentNode();
    if (parent != null) {
      return new MergeElement(parent).getChildrenId();
    }
    return null;
  }
}
