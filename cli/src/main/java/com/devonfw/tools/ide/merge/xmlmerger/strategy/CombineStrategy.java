package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeAttribute;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;

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
  public void merge(MergeElement sourceElement, MergeElement targetElement) {

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
      Element targetElementNode = targetElement.getElement();
      for (MergeAttribute updateAttr : updateElement.getElementAttributes()) {
        if (!updateAttr.isMergeNSAttr()) {
          String namespaceURI = updateAttr.getAttr().getNamespaceURI();
          String attrName = updateAttr.getAttr().getName();
          String attrValue = updateAttr.getValue();

          if (namespaceURI != null && !namespaceURI.isEmpty()) { // copied from Stackoverflow, a simple setAttributeNS doesn't work for all cases
            String prefix = updateAttr.getAttr().getPrefix();
            if (prefix != null && !prefix.isEmpty()) {
              if (targetElementNode.getAttributeNodeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix) == null) {
                targetElementNode.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + prefix, namespaceURI);
              }
              targetElementNode.setAttributeNS(namespaceURI, attrName, attrValue);
            } else {
              // For default namespace
              targetElementNode.setAttributeNS(namespaceURI, attrName, attrValue);
            }
          } else {
            targetElementNode.setAttribute(attrName, attrValue);
          }
        }
      }
    } catch (DOMException e) {
      throw new IllegalStateException("Failed to combine attributes for element " + updateElement.getXPath() + " in " + updateElement.getDocumentPath(), e);
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
          MergeElement sourceChildElement = new MergeElement((Element) updateChild, updateElement.getDocumentPath());
          MergeElement matchedTargetChild = elementMatcher.matchElement(sourceChildElement, targetElement);
          if (matchedTargetChild != null) {
            Strategy childStrategy = StrategyFactory.createStrategy(sourceChildElement.getMergingStrategy(), elementMatcher);
            childStrategy.merge(sourceChildElement, matchedTargetChild);
          } else {
            appendElement(sourceChildElement, targetElement);
          }
        } else if (updateChild.getNodeType() == Node.TEXT_NODE || updateChild.getNodeType() == Node.CDATA_SECTION_NODE) {
          if (!updateChild.getTextContent().isBlank()) {
            replaceTextNode(targetElement, updateChild);
          }
        }
      }
    } catch (DOMException e) {
      throw new IllegalStateException("Failed to combine child nodes for element " + updateElement.getXPath() + " in " + updateElement.getDocumentPath(), e);
    }
  }

  /**
   * Replaces the text node in the target element with the text from the update element, otherwise appends it.
   *
   * @param targetElement the element to be updated
   * @param updateChild the new text node
   */
  private void replaceTextNode(MergeElement targetElement, Node updateChild) {

    Element element = targetElement.getElement();
    try {
      NodeList targetChildNodes = element.getChildNodes();
      for (int i = 0; i < targetChildNodes.getLength(); i++) {
        Node targetChild = targetChildNodes.item(i);
        if (targetChild.getNodeType() == Node.TEXT_NODE || targetChild.getNodeType() == Node.CDATA_SECTION_NODE) {
          if (!targetChild.getTextContent().isBlank()) {
            targetChild.setTextContent(updateChild.getTextContent().trim());
            return;
          }
        }
      }
      Node importedNode = element.getOwnerDocument().importNode(updateChild, true);
      element.appendChild(importedNode);
    } catch (DOMException e) {
      throw new IllegalStateException("Failed to replace text node for element " + targetElement.getXPath() + " in " + targetElement.getDocumentPath(), e);
    }
  }
}
