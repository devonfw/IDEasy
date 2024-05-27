package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeAttribute;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class AbstractStrategy implements Strategy {

  protected final IdeContext context;
  protected final ElementMatcher elementMatcher;

  public AbstractStrategy(IdeContext context, ElementMatcher elementMatcher) {

    this.context = context;
    this.elementMatcher = elementMatcher;
  }

  @Override
  public void merge(MergeElement updateElement, Document targetDocument) {

    MergeElement targetElement = elementMatcher.matchElement(updateElement, targetDocument);
    if (targetElement != null) {
      mergeElement(updateElement, targetElement, targetDocument);
    } else {
      appendElement(updateElement, targetDocument);
    }
  }

  protected abstract void mergeElement(MergeElement updateElement, MergeElement targetElement, Document targetDocument);

  protected void appendElement(MergeElement updateElement, Document targetDocument) {

    this.context.debug("Appending {}", updateElement.getXPath());
    updateAndRemoveNsAttributes(updateElement);

    Element parent = (Element) updateElement.getElement().getParentNode();
    MergeElement matchParent = elementMatcher.matchElement(new MergeElement(parent), targetDocument);
    if (matchParent != null) {
      Element importedNode = (Element) targetDocument.importNode(updateElement.getElement(), true);
      matchParent.getElement().appendChild(importedNode);
    } else {
      // This should not happen since appending is for children and the parent is at least the root
      throw new IllegalStateException("Cannot find matching parent element for " + updateElement.getXPath());
    }
  }

  protected void updateAndRemoveNsAttributes(MergeElement mergeElement) {

    // TODO: update element's id attribute if exists
    for (MergeElement element : mergeElement.getChildElements()) {
      for (MergeAttribute attribute : element.getElementAttributes()) {
        if (attribute.isMergeNsIdAttr()) {
          elementMatcher.updateId(element.getQName(), attribute.getValue());
        }
      }
      element.removeMergeNSAttributes();
    }
    mergeElement.removeMergeNSAttributes();
  }
}
