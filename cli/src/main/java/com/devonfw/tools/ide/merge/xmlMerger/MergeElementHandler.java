package com.devonfw.tools.ide.merge.xmlMerger;

import org.w3c.dom.*;

public class MergeElementHandler {

  public void merge(Document updateDocument, Document targetDocument) {

    MergeElement updateRootElement = new MergeElement(updateDocument.getDocumentElement());
    MergeElement targetRootElement = new MergeElement(targetDocument.getDocumentElement());

    // Merge root element attributes
    mergeAttributes(updateRootElement, targetRootElement);

    // Merge children of root element
    for (MergeElement updateChild : updateRootElement.getChildElements()) {
      mergeElement(updateChild.getElement(), targetDocument);
    }
  }

  private void mergeAttributes(MergeElement updateElement, MergeElement targetElement) {

    MergeStrategy strategy = updateElement.getMergingStrategy();
    switch (strategy) {
      case OVERRIDE:
        overrideAttributes(updateElement, targetElement);
        break;
      case COMBINE:
        combineAttributes(updateElement, targetElement);
        break;
      case KEEP:
        keepAttributes(updateElement, targetElement);
        break;
      default:
        throw new IllegalArgumentException("Unsupported merge strategy");
    }
  }

  private void combineAttributes(MergeElement updateElement, MergeElement targetElement) {

    for (MergeAttribute updateAttr : updateElement.getAttributes()) {
      if (!updateAttr.isMergeNSAttr()) {
        targetElement.getElement().setAttribute(updateAttr.getName(), updateAttr.getValue());
      }
    }
  }

  private void overrideAttributes(MergeElement updateElement, MergeElement targetElement) {

    NamedNodeMap targetAttributes = targetElement.getElement().getAttributes();
    while (targetAttributes.getLength() > 0) {
      targetElement.getElement().removeAttributeNode((Attr) targetAttributes.item(0));
    }
    for (MergeAttribute updateAttr : updateElement.getAttributes()) {
      if (!updateAttr.isMergeNSAttr()) {
        targetElement.getElement().setAttribute(updateAttr.getName(), updateAttr.getValue());
      }
    }
  }

  private void keepAttributes(MergeElement updateElement, MergeElement targetElement) {

    for (MergeAttribute updateAttr : updateElement.getAttributes()) {
      if (!updateAttr.isMergeNSAttr() && !targetElement.getElement().hasAttribute(updateAttr.getName())) {
        targetElement.getElement().setAttribute(updateAttr.getName(), updateAttr.getValue());
      }
    }
  }

  private void mergeElement(Element updateElement, Document targetDocument) {

    MergeElement mergeUpdateElement = new MergeElement(updateElement);
    Element targetElement = matchElement(mergeUpdateElement, targetDocument);

    if (targetElement != null) {
      MergeElement mergeTargetElement = new MergeElement(targetElement);
      MergeStrategy strategy = mergeUpdateElement.getMergingStrategy();
      if (strategy == MergeStrategy.OVERRIDE) {
        overrideElements(mergeUpdateElement, mergeTargetElement);
      } else if (strategy == MergeStrategy.COMBINE) {
        combineElements(mergeUpdateElement, mergeTargetElement);
      } else if (strategy == MergeStrategy.KEEP) {
        // do nothing ...
      }
    } else {
      Element importedNode = (Element) targetDocument.importNode(updateElement, true);
      targetDocument.getDocumentElement().appendChild(importedNode);
    }
  }

  private void combineElements(MergeElement updateElement, MergeElement targetElement) {

    combineAttributes(updateElement, targetElement);
    for (MergeElement updateChild : updateElement.getChildElements()) {
      mergeElement(updateChild.getElement(), targetElement.getElement().getOwnerDocument());
    }
  }

  private void overrideElements(MergeElement updateElement, MergeElement targetElement) {

    Node parentNode = targetElement.getElement().getParentNode();
    if (parentNode != null) {
      Element importedElement = (Element) targetElement.getElement().getOwnerDocument()
          .importNode(updateElement.getElement(), true);
      updateElement.removeMergeNSAttributes();
      parentNode.replaceChild(importedElement, targetElement.getElement());
    }
  }

  private Element matchElement(MergeElement updateElement, Document targetDocument) {

    return null;
  }
}
