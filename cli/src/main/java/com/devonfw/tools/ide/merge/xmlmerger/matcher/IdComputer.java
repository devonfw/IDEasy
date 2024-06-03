package com.devonfw.tools.ide.merge.xmlmerger.matcher;

import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class IdComputer {
  private final String id;

  private static XPathFactory xPathFactory = XPathFactory.newInstance();

  public IdComputer(String id) {
    this.id = id;
  }

  public Element evaluateExpression(MergeElement mergeElement, Document targetDocument) {

    try {
      XPath xpath = xPathFactory.newXPath();
      String xpathExpr = buildXPathExpression(mergeElement);
      XPathExpression xpathExpression = xpath.compile(xpathExpr);
      return (Element) xpathExpression.evaluate(targetDocument, XPathConstants.NODE);
    } catch (XPathExpressionException e) {
      throw new IllegalStateException("Failed to match " + mergeElement.getXPath(), e);
    }
  }

  public String buildXPathExpression(MergeElement mergeElement) {

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