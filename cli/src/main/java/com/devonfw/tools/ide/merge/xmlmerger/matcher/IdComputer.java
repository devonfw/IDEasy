package com.devonfw.tools.ide.merge.xmlmerger.matcher;

import java.util.Collections;
import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;

/**
 * The IdComputer class is responsible for building XPath expressions and evaluating those expressions to match elements in a target document.
 */
public class IdComputer {

  /**
   * the value of merge:id that is used to evaluate the xpath expression.
   */
  private final String id;

  private static final XPathFactory xPathFactory = XPathFactory.newInstance();

  public IdComputer(String id) {

    this.id = id;
  }

  /**
   * @return the value of id.
   */
  public String getId() {

    return this.id;
  }

  /**
   * Evaluates the XPath expression for the given merge element in the target element.
   *
   * @param sourceElement the merge element for which to build the XPath expression
   * @param targetElement the target element in which to evaluate the XPath expression
   * @return the matched Element if found, or null if not found
   */

  public Element evaluateExpression(MergeElement sourceElement, MergeElement targetElement) {
    try {
      XPath xpath = xPathFactory.newXPath();
      final String elementPrefix = sourceElement.getElement().getPrefix();
      final String elementNamespaceURI = sourceElement.getElement().getNamespaceURI();

      if (elementPrefix != null && !elementPrefix.isEmpty()) {
        xpath.setNamespaceContext(new NamespaceContext() { // simple impl of NameSpaceContext that should suffice for our usecases
          @Override
          public String getNamespaceURI(String prefix) {
            return prefix.equals(elementPrefix) ? elementNamespaceURI : XMLConstants.NULL_NS_URI;
          }

          @Override
          public String getPrefix(String namespaceURI) {
            return namespaceURI.equals(elementNamespaceURI) ? elementPrefix : null;
          }

          @Override
          public Iterator<String> getPrefixes(String namespaceURI) {
            return Collections.singletonList(getPrefix(namespaceURI)).iterator();
          }
        });
      }

      String xpathExpr = buildXPathExpression(sourceElement);
      XPathExpression xpathExpression = xpath.compile(xpathExpr);
      NodeList nodeList = (NodeList) xpathExpression.evaluate(targetElement.getElement(), XPathConstants.NODESET);
      int length = nodeList.getLength();
      if (length > 1) {
        throw new IllegalStateException(
            length + " matches found when trying to match element " + sourceElement.getXPath() + " in target document " + targetElement.getDocumentPath());
      } else {
        return (Element) nodeList.item(0);
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

    Element element = mergeElement.getElement();
    String namespaceURI = element.getNamespaceURI();
    String localName = element.getLocalName();
    String prefix = element.getPrefix();

    StringBuilder xpathBuilder = new StringBuilder();
    if (prefix != null && !prefix.isEmpty()) {
      xpathBuilder.append(prefix).append(":");
    }
    xpathBuilder.append(localName);

    if (this.id.startsWith("@")) {
      String attributeName = this.id.substring(1);
      String attributeValue = element.getAttribute(attributeName);
      xpathBuilder.append(String.format("[@%s='%s']", attributeName, attributeValue));
    } else if (this.id.equals("name()")) {
      xpathBuilder.append(String.format("[local-name()='%s']", localName));
      if (namespaceURI != null && !namespaceURI.isEmpty()) {
        xpathBuilder.append(String.format(" and namespace-uri()='%s'", namespaceURI));
      }
    } else if (this.id.equals("text()")) {
      String textContent = element.getTextContent();
      xpathBuilder.append(String.format("[text()='%s']", textContent));
    } else { // custom xpath like ../element[@attr='value']
      return this.id;
    }

    return xpathBuilder.toString();
  }

}
