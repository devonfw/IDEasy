package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeAttribute;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Abstract base class for merge strategies.
 */
public abstract class AbstractStrategy implements Strategy {

  /**
   * The matcher for matching elements in the XML document.
   */
  protected final ElementMatcher elementMatcher;

  /**
   * @param elementMatcher the element matcher used for matching elements
   */
  public AbstractStrategy(ElementMatcher elementMatcher) {

    this.elementMatcher = elementMatcher;
  }

  @Override
  public abstract void merge(MergeElement sourceElement, MergeElement targetElement);

  /**
   * Appends the source element as a child of the target element.
   *
   * @param sourceElement the element to be appended.
   * @param targetElement the target element where the source element will be appended.
   */
  protected void appendElement(MergeElement sourceElement, MergeElement targetElement) {

    try {
      updateAndRemoveNsAttributes(sourceElement);
      Document targetDocument = targetElement.getElement().getOwnerDocument();
      Element importedNode = (Element) targetDocument.importNode(sourceElement.getElement(), true);
      targetElement.getElement().appendChild(importedNode);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to append element: " + sourceElement.getXPath(), e);
    }
  }

  /**
   * Updates the {@link ElementMatcher} and removes namespace attributes from the merge element. Is used when overriding or appending an element to make sure
   * that no information regarding merge:id of a child element gets lost.
   *
   * @param mergeElement the merge element whose id is to be updated and merge namespace attributes removed.
   */
  protected void updateAndRemoveNsAttributes(MergeElement mergeElement) {

    for (MergeAttribute attribute : mergeElement.getElementAttributes()) {
      if (attribute.isMergeNSAttr()) {
        if (attribute.isMergeNsIdAttr()) {
          elementMatcher.updateId(mergeElement.getQName(), attribute.getValue());
        }
        mergeElement.getElement().removeAttributeNode(attribute.getAttr());
      }
    }
    for (MergeElement childElement : mergeElement.getChildElements()) {
      updateAndRemoveNsAttributes(childElement);
    }
  }
}
