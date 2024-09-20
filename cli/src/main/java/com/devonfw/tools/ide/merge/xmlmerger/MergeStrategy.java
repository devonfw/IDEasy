package com.devonfw.tools.ide.merge.xmlmerger;

import java.util.Locale;
import javax.xml.XMLConstants;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeAttribute;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;

/**
 * Enum of merge strategies for XML elements.
 */
public enum MergeStrategy {

  /**
   * Combines source and target elements. Overrides text nodes and attributes. This process is recursively applied to child elements. If the source element
   * exists in the target document, they are combined, otherwise, the source element is appended.
   */
  COMBINE {
    @Override
    public void merge(MergeElement sourceElement, MergeElement targetElement, ElementMatcher elementMatcher) {

      combineAttributes(sourceElement, targetElement);
      combineChildNodes(sourceElement, targetElement, elementMatcher);
    }
  },

  /**
   * Replaces the target element with the source element, without considering child elements. If the element exists in the target, it is overridden, otherwise,
   * it is appended.
   */
  OVERRIDE {
    @Override
    public void merge(MergeElement sourceElement, MergeElement targetElement, ElementMatcher elementMatcher) {

      try {
        updateAndRemoveNsAttributes(sourceElement, elementMatcher);
        Node importedNode = targetElement.getElement().getOwnerDocument().importNode(sourceElement.getElement(), true);
        targetElement.getElement().getParentNode().replaceChild(importedNode, targetElement.getElement());
      } catch (DOMException e) {
        throw new IllegalStateException("Failed to override element " + sourceElement.getXPath() + " in " + sourceElement.getDocumentPath(), e);
      }
    }
  },

  /**
   * Keeps the existing target element intact if the source element exists in the target document, otherwise, it is appended.
   */
  KEEP {
    @Override
    public void merge(MergeElement sourceElement, MergeElement targetElement, ElementMatcher elementMatcher) {

      // Do nothing, keep the existing element
    }
  };

  /**
   * Merges the source element into the target element using the specific strategy.
   *
   * @param sourceElement the source element to be merged
   * @param targetElement the target element to merge into
   * @param elementMatcher the element matcher used for matching elements
   */
  public abstract void merge(MergeElement sourceElement, MergeElement targetElement, ElementMatcher elementMatcher);

  /**
   * Returns the MergeStrategy enum constant with the specified name.
   *
   * @param name the name of the enum constant to return
   * @return the enum constant with the specified name
   */
  public static MergeStrategy of(String name) {

    return Enum.valueOf(MergeStrategy.class, name.toUpperCase(Locale.ROOT));
  }

  /**
   * Updates the element matcher with the merge:id attribute and removes all merge namespace attributes.
   *
   * @param mergeElement the merge element to process
   * @param elementMatcher the element matcher to update
   */
  protected void updateAndRemoveNsAttributes(MergeElement mergeElement, ElementMatcher elementMatcher) {

    for (MergeAttribute attribute : mergeElement.getElementAttributes()) {
      if (attribute.isMergeNsAttr()) {
        mergeElement.getElement().removeAttributeNode(attribute.getAttr());
      }
    }
    for (MergeElement childElement : mergeElement.getChildElements()) {
      updateAndRemoveNsAttributes(childElement, elementMatcher);
    }
  }

  /**
   * Combines attributes from the update element into the target element.
   *
   * @param updateElement the element with the new attributes
   * @param targetElement the element to receive the new attributes
   */
  protected void combineAttributes(MergeElement updateElement, MergeElement targetElement) {

    try {
      Element targetElementNode = targetElement.getElement();
      for (MergeAttribute updateAttr : updateElement.getElementAttributes()) {
        if (!updateAttr.isMergeNsAttr()) {
          String namespaceUri = updateAttr.getAttr().getNamespaceURI();
          String attrName = updateAttr.getAttr().getName();
          String attrValue = updateAttr.getValue();

          if (namespaceUri != null && !namespaceUri.isEmpty()) {
            String prefix = updateAttr.getAttr().getPrefix();
            if (prefix != null && !prefix.isEmpty()) {
              if (targetElementNode.getAttributeNodeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix) == null) {
                targetElementNode.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + prefix, namespaceUri);
              }
              targetElementNode.setAttributeNS(namespaceUri, attrName, attrValue);
            } else {
              targetElementNode.setAttributeNS(namespaceUri, attrName, attrValue);
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
   * Combines child nodes (elements, text and CDATA) from the update element into the target element.
   *
   * @param updateElement the element with the new child nodes
   * @param targetElement the element to receive the new child nodes
   * @param elementMatcher the element matcher used for matching elements
   */
  protected void combineChildNodes(MergeElement updateElement, MergeElement targetElement, ElementMatcher elementMatcher) {

    try {
      NodeList updateChildNodes = updateElement.getElement().getChildNodes();
      for (int i = 0; i < updateChildNodes.getLength(); i++) {
        Node updateChild = updateChildNodes.item(i);
        if (updateChild.getNodeType() == Node.ELEMENT_NODE) {
          MergeElement sourceChildElement = new MergeElement((Element) updateChild, updateElement.getDocumentPath());
          MergeElement matchedTargetChild = elementMatcher.matchElement(sourceChildElement, targetElement);
          if (matchedTargetChild != null) {
            MergeStrategy childStrategy = MergeStrategy.of(sourceChildElement.getMergingStrategy());
            childStrategy.merge(sourceChildElement, matchedTargetChild, elementMatcher);
          } else {
            appendElement(sourceChildElement, targetElement, elementMatcher);
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
   * Appends the source element as a child of the target element.
   *
   * @param sourceElement the element to be appended
   * @param targetElement the target element where the source element will be appended
   * @param elementMatcher the element matcher used for updating IDs
   */
  protected void appendElement(MergeElement sourceElement, MergeElement targetElement, ElementMatcher elementMatcher) {

    try {
      updateAndRemoveNsAttributes(sourceElement, elementMatcher);
      Document targetDocument = targetElement.getElement().getOwnerDocument();
      Element importedNode = (Element) targetDocument.importNode(sourceElement.getElement(), true);
      targetElement.getElement().appendChild(importedNode);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to append element: " + sourceElement.getXPath(), e);
    }
  }

  /**
   * Replaces the text node in the target element with the text from the update element, otherwise appends it.
   *
   * @param targetElement the element to be updated
   * @param updateChild the new text node
   */
  protected void replaceTextNode(MergeElement targetElement, Node updateChild) {

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
