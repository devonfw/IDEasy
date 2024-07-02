package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Merge strategy that overrides existing elements with new elements.
 */
public class OverrideStrategy extends AbstractStrategy {

  /**
   * @param elementMatcher the element matcher used for matching elements
   */
  public OverrideStrategy(ElementMatcher elementMatcher) {

    super(elementMatcher);
  }

  @Override
  public void merge(MergeElement sourceElement, MergeElement targetElement) {

    try {
      updateAndRemoveNsAttributes(sourceElement);
      Node importedNode = targetElement.getElement().getOwnerDocument().importNode(sourceElement.getElement(), true);
      targetElement.getElement().getParentNode().replaceChild(importedNode, targetElement.getElement());
    } catch (DOMException e) {
      throw new IllegalStateException(
          "Failed to override element " + sourceElement.getXPath() + " in " + sourceElement.getDocumentPath(), e);
    }
  }
}
