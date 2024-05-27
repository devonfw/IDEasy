package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class OverrideStrategy extends AbstractStrategy {

  public OverrideStrategy(IdeContext context, ElementMatcher elementMatcher) {
    super(context, elementMatcher);
  }

  @Override
  protected void mergeElement(MergeElement updateElement, MergeElement targetElement, Document targetDocument) {

    this.context.debug("Overriding element {}", updateElement.getXPath());
    overrideElement(updateElement, targetElement);
  }

  private void overrideElement(MergeElement updateElement, MergeElement targetElement) {

    try {
      updateAndRemoveNsAttributes(updateElement);
      Node targetNode = targetElement.getElement();
      Node parentNode = targetNode.getParentNode();
      Document targetDocument = targetNode.getOwnerDocument();
      Node importedNode = targetDocument.importNode(updateElement.getElement(), true);
      if (parentNode.getNodeType() == Node.DOCUMENT_NODE) {
        targetDocument.replaceChild(importedNode, targetDocument.getDocumentElement());
      } else {
        parentNode.replaceChild(importedNode, targetNode);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to override element: " + updateElement.getXPath(), e);
    }
  }
}
