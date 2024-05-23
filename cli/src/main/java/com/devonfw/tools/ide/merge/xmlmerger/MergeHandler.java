package com.devonfw.tools.ide.merge.xmlmerger;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeAttribute;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeStrategy;
import org.w3c.dom.*;

public class MergeHandler {

  private final ElementMatcher elementMatcher;
  
  private final IdeContext context;

  public MergeHandler(ElementMatcher elementMatcher, IdeContext context) {

    this.elementMatcher = elementMatcher;
    this.context = context;
  }

  public void merge(Document updateDocument, Document targetDocument) {

    MergeElement updateRootElement = new MergeElement(updateDocument.getDocumentElement());
    MergeElement targetRootElement = new MergeElement(targetDocument.getDocumentElement());
    mergeElements(updateRootElement, targetRootElement, targetDocument);
  }

  private void mergeElements(MergeElement updateElement, MergeElement targetElement, Document targetDocument) {

    context.debug("Merging {} ...", updateElement.getXPath());
    Element matchedTargetElement = elementMatcher.matchElement(updateElement, targetDocument);

    if (matchedTargetElement != null) {
      targetElement = new MergeElement(matchedTargetElement);
      MergeStrategy strategy = updateElement.getMergingStrategy();
      context.debug("Merge strategy {}", strategy);

      switch (strategy) {
        case COMBINE:
          combineElements(updateElement, targetElement);
          break;
        case OVERRIDE:
          overrideElement(updateElement, targetElement);
          break;
        case KEEP:
          // Do nothing if the element already exists
          break;
        default:
          context.debug("Unknown merging strategy, skipping element {}", updateElement.getXPath());
      }
    } else {
      // Element doesn't exist in the target document, append it
      appendElement(updateElement, targetDocument);
    }
  }

  private void combineElements(MergeElement updateElement, MergeElement targetElement) {

    combineAttributes(updateElement, targetElement);
    combineChildNodes(updateElement, targetElement);
  }

  private void combineAttributes(MergeElement updateElement, MergeElement targetElement) {

    for (MergeAttribute updateAttr : updateElement.getElementAttributes()) {
      if (!updateAttr.isMergeNSAttr()) {
        targetElement.getElement().setAttribute(updateAttr.getName(), updateAttr.getValue());
      }
    }
  }

  private void combineChildNodes(MergeElement updateElement, MergeElement targetElement) {

    NodeList updateChildNodes = updateElement.getElement().getChildNodes();

    for (int i = 0; i < updateChildNodes.getLength(); i++) {
      Node updateChild = updateChildNodes.item(i);

      if (updateChild.getNodeType() == Node.ELEMENT_NODE) {
        MergeElement mergeUpdateChild = new MergeElement((Element) updateChild);
        mergeElements(mergeUpdateChild, targetElement, targetElement.getElement().getOwnerDocument());
      } else if (updateChild.getNodeType() == Node.TEXT_NODE || updateChild.getNodeType() == Node.CDATA_SECTION_NODE) {
        targetElement.getElement().setTextContent(updateChild.getTextContent());
      }
    }
  }

  private void overrideElement(MergeElement updateElement, MergeElement targetElement) {

    Node parentNode = targetElement.getElement().getParentNode();
    if (parentNode != null) {
      Element importedElement = (Element) parentNode.getOwnerDocument().importNode(updateElement.getElement(), true);
      updateElement.removeMergeNSAttributes();
      parentNode.replaceChild(importedElement, targetElement.getElement());
    }
  }

  private void appendElement(MergeElement updateElement, Document targetDocument) {

    Element parent = (Element) updateElement.getElement().getParentNode();
    Element matchParent = elementMatcher.matchElement(new MergeElement(parent), targetDocument);
    if (matchParent != null) {
      Element importedNode = (Element) targetDocument.importNode(updateElement.getElement(), true);
      matchParent.appendChild(importedNode);
    } else {
      // should actually never happen, since appending is for children and parent is at least root
      this.context.debug("Cannot find matching parent element for {} ", updateElement.getXPath());
    }
  }


}
