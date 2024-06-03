package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Merge strategy that overrides existing elements with new elements.
 */
public class OverrideStrategy extends AbstractStrategy {

  /**
   * @param context the context
   * @param elementMatcher the element matcher used for matching elements
   */
  public OverrideStrategy(IdeContext context, ElementMatcher elementMatcher) {

    super(context, elementMatcher);
  }

  @Override
  protected void mergeElement(MergeElement sourceElement, MergeElement targetElement) {

    this.context.debug("Overriding element {}", sourceElement.getXPath());
    overrideElement(sourceElement, targetElement);
  }

  /**
   * Overrides the target element with the source element.
   *
   * @param sourceElement the source element to be merged
   * @param targetElement the target element to be overridden
   */
  private void overrideElement(MergeElement sourceElement, MergeElement targetElement) {

    try {
      updateAndRemoveNsAttributes(sourceElement);
      Node targetNode = targetElement.getElement();
      Node parentNode = targetNode.getParentNode();
      Document targetDocument = targetNode.getOwnerDocument();
      Node importedNode = targetDocument.importNode(sourceElement.getElement(), true);
      if (parentNode.getNodeType() == Node.DOCUMENT_NODE) {
        targetDocument.replaceChild(importedNode, targetDocument.getDocumentElement());
      } else {
        parentNode.replaceChild(importedNode, targetNode);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to override element: " + sourceElement.getXPath(), e);
    }
  }
}
