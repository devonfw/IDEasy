package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeAttribute;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CombineStrategy extends AbstractStrategy {

  public CombineStrategy(IdeContext context, ElementMatcher elementMatcher) {

    super(context, elementMatcher);
  }

  @Override
  protected void mergeElement(MergeElement updateElement, MergeElement targetElement, Document targetDocument) {

    this.context.debug("Combining element {}", updateElement.getXPath());
    combineAttributes(updateElement, targetElement);
    combineChildNodes(updateElement, targetElement, targetElement.getElement().getOwnerDocument());
  }


  private void combineAttributes(MergeElement updateElement, MergeElement targetElement) {

    this.context.debug("Combining attributes for {}", updateElement.getXPath());
    try {
      for (MergeAttribute updateAttr : updateElement.getElementAttributes()) {
        if (!updateAttr.isMergeNSAttr()) {
          String namespaceURI = updateAttr.getAttr().getNamespaceURI();
          String attrName = updateAttr.getAttr().getLocalName();
          targetElement.getElement().setAttributeNS(namespaceURI, attrName, updateAttr.getValue());
        }
      }
    } catch (DOMException e) {
      throw new IllegalStateException("Failed to combine attributes for element: " + updateElement.getXPath(), e);
    }
  }

  private void combineChildNodes(MergeElement updateElement, MergeElement targetElement, Document targetDocument) {

    this.context.debug("Combining child nodes for {}", updateElement.getXPath());
    try {
      NodeList updateChildNodes = updateElement.getElement().getChildNodes();
      for (int i = 0; i < updateChildNodes.getLength(); i++) {
        Node updateChild = updateChildNodes.item(i);
        if (updateChild.getNodeType() == Node.ELEMENT_NODE) {
          // Handle element nodes
          MergeElement mergeUpdateChild = new MergeElement((Element) updateChild);
          StrategyFactory strategyFactory = new StrategyFactory(context, elementMatcher);
          Strategy strategy = strategyFactory.createStrategy(mergeUpdateChild.getMergingStrategy());
          strategy.merge(mergeUpdateChild, targetDocument);
        } else if (updateChild.getNodeType() == Node.TEXT_NODE || updateChild.getNodeType() == Node.CDATA_SECTION_NODE) {
          // Handle text and CDATA nodes
          if (!updateChild.getTextContent().isBlank()) {
            replaceTextNode(targetElement.getElement(), updateChild);
          }
        }
      }
    } catch (DOMException e) {
      throw new IllegalStateException("Failed to combine child nodes for element: " + updateElement.getXPath(), e);
    }
  }

  private void replaceTextNode(Element targetElement, Node updateChild) {

    try {
      NodeList targetChildNodes = targetElement.getChildNodes();
      for (int i = 0; i < targetChildNodes.getLength(); i++) {
        Node targetChild = targetChildNodes.item(i);
        if (targetChild.getNodeType() == Node.TEXT_NODE || targetChild.getNodeType() == Node.CDATA_SECTION_NODE) {
          // Replace the text node content
          if (!targetChild.getTextContent().isBlank()) {
            targetChild.setTextContent(updateChild.getTextContent().trim());
            return;
          }
        }
      }
      // If no text node was found, append the new text content
      Node importedNode = targetElement.getOwnerDocument().importNode(updateChild, true);
      targetElement.appendChild(importedNode);
    } catch (DOMException e) {
      throw new IllegalStateException("Failed to replace text node for element: " + targetElement.getTagName(), e);
    }

  }
}