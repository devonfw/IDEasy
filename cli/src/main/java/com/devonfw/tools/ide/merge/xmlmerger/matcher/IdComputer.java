package com.devonfw.tools.ide.merge.xmlmerger.matcher;

import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * The IdComputer class is responsible for building XPath expressions and evaluating those expressions to match elements in a target document.
 */
public class IdComputer {

  private final String id;

  private static final XPathFactory xPathFactory = XPathFactory.newInstance();

  public IdComputer(String id) {

    this.id = id;
  }

  /**
   * Evaluates the XPath expression for the given merge element in the target document.
   *
   * @param mergeElement the merge element for which to build the XPath expression
   * @param targetDocument the target document in which to evaluate the XPath expression
   * @return the matched Element if found, or null if not found
   */

  public Element evaluateExpression(MergeElement sourceElement, MergeElement targetElement) {

    try {
      XPath xpath = xPathFactory.newXPath();
      String xpathExpr = buildXPathExpression(sourceElement);
      XPathExpression xpathExpression = xpath.compile(xpathExpr);
      NodeList nodeList = (NodeList) xpathExpression.evaluate(targetElement.getElement(), XPathConstants.NODESET);
      int length = nodeList.getLength();
      if (length == 1) {
        return (Element) nodeList.item(0);
      } else if (length > 1) {
        throw new IllegalStateException("Couldn't match " + sourceElement.getXPath() + ". Found multiple matches.");
      } else { // == 0
        return null;
      }

    } catch (XPathExpressionException e) {
      throw new IllegalStateException("Failed to match " + sourceElement.getXPath(), e);
    }
  }

  /**
   * Builds the XPath expression for the given merge element based on the ID value.
   *
   * @param mergeElement the merge element for which to build the XPath expression
   * @return the XPath expression as a String
   */
  private String buildXPathExpression(MergeElement mergeElement) {

    String tagName = mergeElement.getElement().getTagName();
    if (id.startsWith(".")) {
      return tagName + "/" + id;
    } else if (id.startsWith("/")) {
      return id;
    } else if (id.startsWith("@")) {
      String attributeName = id.substring(1);
      String attributeValue = mergeElement.getElement().getAttribute(attributeName);
      return tagName + String.format("[@%s='%s']", attributeName, attributeValue);
    } else if (id.equals("name()")) {
      return tagName + String.format("[name()='%s']", tagName);
    } else if (id.equals("text()")) {
      String textContent = mergeElement.getElement().getTextContent();
      return tagName + String.format("[text()='%s']", textContent);
    }
    return null;
  }

}