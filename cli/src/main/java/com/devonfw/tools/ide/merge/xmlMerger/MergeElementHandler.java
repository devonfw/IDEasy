package com.devonfw.tools.ide.merge.xmlMerger;

import org.w3c.dom.*;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

public class MergeElementHandler {

  public void merge(Document updateDocument, Document targetDocument) {
    MergeElement updateRootElement = new MergeElement(updateDocument.getDocumentElement());
    MergeElement targetRootElement = new MergeElement(targetDocument.getDocumentElement());

    // Merge root element attributes
    mergeAttributes(updateRootElement, targetRootElement);

    // Merge children of root element
    for (MergeElement updateChild : updateRootElement.getChildElements()) {
      mergeElement(updateChild.getElement(), targetDocument);
    }
  }

  private void mergeAttributes(MergeElement updateElement, MergeElement targetElement) {
    MergeStrategy strategy = updateElement.getMergingStrategy();
    switch (strategy) {
      case OVERRIDE:
        overrideAttributes(updateElement, targetElement);
        break;
      case COMBINE:
        combineAttributes(updateElement, targetElement);
        break;
      case KEEP:
        keepAttributes(updateElement, targetElement);
        break;
      default:
        throw new IllegalArgumentException("Unsupported merge strategy");
    }
  }

  private void combineAttributes(MergeElement updateElement, MergeElement targetElement) {
    for (MergeAttribute updateAttr : updateElement.getAttributes()) {
      if (!updateAttr.isMergeNSAttr() && !updateAttr.getValue().equals(MergeAnnotation.MERGE_NS_URI)) {
        targetElement.getElement().setAttribute(updateAttr.getName(), updateAttr.getValue());
      }
    }
  }

  private void overrideAttributes(MergeElement updateElement, MergeElement targetElement) {
    NamedNodeMap targetAttributes = targetElement.getElement().getAttributes();
    while (targetAttributes.getLength() > 0) {
      targetElement.getElement().removeAttributeNode((Attr) targetAttributes.item(0));
    }
    for (MergeAttribute updateAttr : updateElement.getAttributes()) {
      if (!updateAttr.isMergeNSAttr()) {
        targetElement.getElement().setAttribute(updateAttr.getName(), updateAttr.getValue());
      }
    }
  }

  private void keepAttributes(MergeElement updateElement, MergeElement targetElement) {
    for (MergeAttribute updateAttr : updateElement.getAttributes()) {
      if (!updateAttr.isMergeNSAttr() && !targetElement.getElement().hasAttribute(updateAttr.getName())) {
        targetElement.getElement().setAttribute(updateAttr.getName(), updateAttr.getValue());
      }
    }
  }

  private void mergeElement(Element updateElement, Document targetDocument) {
    MergeElement mergeUpdateElement = new MergeElement(updateElement);
    Element targetElement = matchElement(mergeUpdateElement, targetDocument);

    if (targetElement != null) {
      MergeElement mergeTargetElement = new MergeElement(targetElement);
      MergeStrategy strategy = mergeUpdateElement.getMergingStrategy();
      if (strategy == MergeStrategy.OVERRIDE) {
        overrideElements(mergeUpdateElement, mergeTargetElement);
      } else if (strategy == MergeStrategy.COMBINE) {
        combineElements(mergeUpdateElement, mergeTargetElement);
      } else if (strategy == MergeStrategy.KEEP) {
        // do nothing ...
      }
    } else {
      // append the element
      Element parent = (Element) updateElement.getParentNode();
      Element matchParent = matchElement(new MergeElement(parent), targetDocument);
      if (matchParent != null) {
        Element importedNode = (Element) targetDocument.importNode(updateElement, true);
        matchParent.appendChild(importedNode);
      } else {
        // should actually never happen, since appending is for children and parent is at least root, remove
        throw new IllegalStateException("Cannot find matching parent element for: " + updateElement.getTagName());
      }
    }
  }

  private void combineElements(MergeElement updateElement, MergeElement targetElement) {
    combineAttributes(updateElement, targetElement);
    for (MergeElement updateChild : updateElement.getChildElements()) {
      mergeElement(updateChild.getElement(), targetElement.getElement().getOwnerDocument());
    }
  }

  private void overrideElements(MergeElement updateElement, MergeElement targetElement) {
    Node parentNode = targetElement.getElement().getParentNode();
    if (parentNode != null) {
      Element importedElement = (Element) targetElement.getElement().getOwnerDocument()
          .importNode(updateElement.getElement(), true);
      updateElement.removeMergeNSAttributes();
      parentNode.replaceChild(importedElement, targetElement.getElement());
    }
  }

  private Element matchElement(MergeElement updateElement, Document targetDocument) {


    if (updateElement.getElement().getParentNode() instanceof Document) { // if element is root
      if (targetDocument.getDocumentElement().getTagName().equals(updateElement.getElement().getTagName())) {
        return targetDocument.getDocumentElement();
      }
      return null;
    }

    String id = updateElement.getId();
    if (id == null || id.isEmpty()) {
      return null;
    }

    try {
      XPath xpath = XPathFactory.newInstance().newXPath();
      String xpathExpression = buildXPathExpression(id, updateElement);

      // Evaluate the XPath expression in the context of the targetDocument
      XPathExpression expr = xpath.compile(xpathExpression);
      NodeList result = (NodeList) expr.evaluate(targetDocument, XPathConstants.NODESET);

      if (result.getLength() > 0) {
        return (Element) result.item(0); // Return the first matching element
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to match element with id: " + id, e);
    }

    return null;
  }

  private String buildXPathExpression(String id, MergeElement updateElement) {
    Element contextElement = updateElement.getElement();
    String xPath = updateElement.getXPath();

    if (id.startsWith("./") || id.startsWith("/")) {
      return xPath + id;
    } else if (id.startsWith("@")) {
      String attributeName = id.substring(1);
      String attributeValue = contextElement.getAttribute(attributeName);
      return xPath + String.format("[@%s='%s']", attributeName, attributeValue);
    } else if (id.equals("name()")) {
      String tagName = contextElement.getTagName();
      return xPath + String.format("[name()='%s']", tagName);
    } else if (id.equals("text()")) {
      String textContent = contextElement.getTextContent();
      return xPath + String.format("[text()='%s']", textContent);
    } else {
      return xPath + id; // Assume it's a custom XPath
    }
  }
}
