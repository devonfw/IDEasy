package com.devonfw.tools.ide.merge.xmlMerger;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlMerger.matcher.ElementMatcher;
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
    mergeElement(updateRootElement, targetDocument);
  }

  private void mergeElement(MergeElement updateElement, Document targetDocument) {

    this.context.debug("Merging {} ...", updateElement.getXPath());
    Element targetElement = elementMatcher.matchElement(updateElement, targetDocument);

    if (targetElement != null) {
      // match found
      MergeElement mergeTargetElement = new MergeElement(targetElement);
      MergeStrategy strategy = updateElement.getMergingStrategy();
      this.context.debug("Merge strategy {}", strategy.toString());
      switch(strategy) {
        case COMBINE:
          combineElements(updateElement, mergeTargetElement);
          break;
        case OVERRIDE:
          overrideElements(updateElement, mergeTargetElement);
          break;
        case KEEP:
          // do nothing ...
          break;
        default:
          this.context.debug("unknown merging strategy, skipping element {}", updateElement.getXPath());
      }
    } else {
      // append the element
      appendElement(updateElement, targetDocument);
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

  private void combineElements(MergeElement updateElement, MergeElement targetElement) {

    for (MergeAttribute updateAttr : updateElement.getAttributes()) {
      if (!updateAttr.isMergeNSAttr() && !updateAttr.getValue().equals(MergeAnnotation.MERGE_NS_URI)) {
        targetElement.getElement().setAttribute(updateAttr.getName(), updateAttr.getValue());
      }
    }

    NodeList updateChildren = updateElement.getElement().getChildNodes();
    for (int i = 0; i < updateChildren.getLength(); i++) {
      Node child = updateChildren.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        mergeElement(new MergeElement((Element) child), targetElement.getElement().getOwnerDocument());
      } else if (child.getNodeType() == Node.TEXT_NODE) {
        String text = child.getTextContent().trim();
        if (!text.isEmpty()) {
          targetElement.getElement().setTextContent(text);
        }
      } else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
        CDATASection updateCDATA = (CDATASection) child;
        targetElement.getElement().setTextContent(updateCDATA.getData());
      }
    }
  }

  private void overrideElements(MergeElement updateElement, MergeElement targetElement) {

    // remove ns attributes
    if (updateElement.isRootElement()) {
      Document targetDocument = targetElement.getElement().getOwnerDocument();
      Node newRoot = targetDocument.importNode(updateElement.getElement(), true);
      targetDocument.replaceChild(newRoot, targetDocument.getDocumentElement());
    } else {
      Node parentNode = targetElement.getElement().getParentNode();
      if (parentNode != null) {
        Element importedElement = (Element) targetElement.getElement().getOwnerDocument()
            .importNode(updateElement.getElement(), true);
        updateElement.removeMergeNSAttributes();
        parentNode.replaceChild(importedElement, targetElement.getElement());
      }
    }
  }



}
