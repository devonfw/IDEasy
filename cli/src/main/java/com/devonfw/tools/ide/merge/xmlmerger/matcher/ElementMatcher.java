package com.devonfw.tools.ide.merge.xmlmerger.matcher;

import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.xpath.*;
import java.util.HashMap;
import java.util.Map;

public class ElementMatcher {

  private final Map<QName, String> qNameIdMap;

  private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();

  public ElementMatcher() {

    qNameIdMap = new HashMap<>();
  }

  public void updateId(QName qname, String id) {

    qNameIdMap.put(qname, id);
  }

  public MergeElement matchElement(MergeElement updateElement, Document targetDocument) {

    if (updateElement.isRootElement()) {
      Element sourceRoot = updateElement.getElement();
      Element targetRoot = targetDocument.getDocumentElement();
      if (sourceRoot.getNamespaceURI() != null || targetRoot.getNamespaceURI() != null) {
        if (!sourceRoot.getNamespaceURI().equals(targetRoot.getNamespaceURI())) {
          throw new IllegalStateException("URI of elements don't match. Found " + sourceRoot.getNamespaceURI() + "and " + targetRoot.getNamespaceURI());
        }
      }
      return new MergeElement(targetRoot);
    }

    String id = updateElement.getId();
    if (id == null) {
      id = qNameIdMap.get(updateElement.getQName());
      if (id == null) {
        throw new IllegalStateException("no Id value was found for " + updateElement.getXPath());
      }
    }
    updateId(updateElement.getQName(), id);

    try {
      XPath xpath = XPATH_FACTORY.newXPath();
      String xpathExpression = buildXPathExpression(updateElement, id);
      XPathExpression expr = xpath.compile(xpathExpression);
      Node matchedNode = (Node) expr.evaluate(targetDocument, XPathConstants.NODE);
      if (matchedNode != null) {
        return new MergeElement((Element) matchedNode);
      }
    } catch (XPathExpressionException e) {
      throw new IllegalStateException("Failed to match element with id: " + id, e);
    }

    return null;
  }

  public String buildXPathExpression(MergeElement mergeElement, String id) {

    String xPath = mergeElement.getXPath();
    if (id.startsWith(".")) {
      return xPath + "/" + id;
    } else if (id.startsWith("/")) {
      return id;
    } else if (id.startsWith("@")) {
      String attributeName = id.substring(1);
      String attributeValue = mergeElement.getElement().getAttribute(attributeName);
      return xPath + String.format("[@%s='%s']", attributeName, attributeValue);
    } else if (id.equals("name()")) {
      String tagName = mergeElement.getElement().getTagName();
      return xPath + String.format("[name()='%s']", tagName);
    } else if (id.equals("text()")) {
      String textContent = mergeElement.getElement().getTextContent();
      return xPath + String.format("[text()='%s']", textContent);
    }
    return null;
  }
}
