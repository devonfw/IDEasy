package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeAttribute;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Merge Strategy implementation for combining XML elements.
 */
public class CombineStrategy extends AbstractStrategy {

  /**
   * @param elementMatcher the element matcher used for matching elements
   */
  public CombineStrategy(ElementMatcher elementMatcher) {

    super(elementMatcher);
  }

  @Override
  protected void mergeElement(MergeElement sourceElement, MergeElement targetElement) {

    combineAttributes(sourceElement, targetElement);
    combineChildNodes(sourceElement, targetElement);
  }

  /**
   * Combines attributes from the update element into the target element.
   *
   * @param updateElement the element with the new attributes
   * @param targetElement the element to receive the new attributes
   */
  private void combineAttributes(MergeElement updateElement, MergeElement targetElement) {

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

  /**
   * Combines child nodes from the update element into the target element.
   *
   * @param updateElement the element with the new child nodes
   * @param targetElement the element to receive the new child nodes
   */
  private void combineChildNodes(MergeElement updateElement, MergeElement targetElement) {

    try {
      NodeList updateChildNodes = updateElement.getElement().getChildNodes();
      for (int i = 0; i < updateChildNodes.getLength(); i++) {
        Node updateChild = updateChildNodes.item(i);
        if (updateChild.getNodeType() == Node.ELEMENT_NODE) {
          MergeElement mergeUpdateChild = new MergeElement((Element) updateChild);
          Strategy strategy = StrategyFactory.createStrategy(mergeUpdateChild.getMergingStrategy(), elementMatcher);
          strategy.merge(mergeUpdateChild, targetElement.getElement().getOwnerDocument());
        } else if (updateChild.getNodeType() == Node.TEXT_NODE || updateChild.getNodeType() == Node.CDATA_SECTION_NODE) {
          if (!updateChild.getTextContent().isBlank()) {
            replaceTextNode(targetElement.getElement(), updateChild);
          }
        }
      }
    } catch (DOMException e) {
      throw new IllegalStateException("Failed to combine child nodes for element: " + updateElement.getXPath(), e);
    }
  }

  /**
   * Replaces the text node in the target element with the text from the update element, otherwise appends it.
   *
   * @param targetElement the element to be updated
   * @param updateChild the new text node
   */
  private void replaceTextNode(Element targetElement, Node updateChild) {

    try {
      NodeList targetChildNodes = targetElement.getChildNodes();
      for (int i = 0; i < targetChildNodes.getLength(); i++) {
        Node targetChild = targetChildNodes.item(i);
        if (targetChild.getNodeType() == Node.TEXT_NODE || targetChild.getNodeType() == Node.CDATA_SECTION_NODE) {
          if (!targetChild.getTextContent().isBlank()) {
            targetChild.setTextContent(updateChild.getTextContent().trim());
            return;
          }
        }
      }
      Node importedNode = targetElement.getOwnerDocument().importNode(updateChild, true);
      targetElement.appendChild(importedNode);
    } catch (DOMException e) {
      throw new IllegalStateException("Failed to replace text node for element: " + targetElement.getTagName(), e);
    }
  }
}
