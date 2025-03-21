package com.devonfw.tools.ide.merge.xml.matcher;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xml.XmlMergeSupport;

/**
 * The IdComputer class is responsible for building XPath expressions and evaluating those expressions to match elements in a target document.
 */
public class IdComputer {

  /** Name of the {@link com.devonfw.tools.ide.environment.EnvironmentVariables variable} to fail on ambiguous merge. */
  public static final String FAIL_ON_AMBIGOUS_MERGE = "FAIL_ON_AMBIGOUS_MERGE";

  /** The value of merge:id that is used to evaluate the xpath expression. */
  private final String id;

  private final IdeContext context;

  private static final XPathFactory xPathFactory = XPathFactory.newInstance();

  /**
   * The constructor.
   *
   * @param id the {@link #getId() merge ID}.
   */
  public IdComputer(String id, IdeContext context) {

    super();
    this.id = id;
    this.context = context;
  }

  /**
   * @return the value of "merge:id" attribute what is an {@link XPath} expression.
   * @see XmlMergeSupport#getMergeId(Element)
   */
  public String getId() {

    return this.id;
  }

  /**
   * Evaluates the XPath expression for the given merge element in the target element.
   *
   * @param templateElement the template {@link Element} for which to build the {@link XPath} expression.
   * @param workspaceElement the workspace {@link Element} in which to evaluate the {@link XPath} expression.
   * @return the matched Element if found, or {@code null} if not found
   */
  public Element evaluateExpression(Element templateElement, Element workspaceElement) {
    XPath xpath = xPathFactory.newXPath();
    xpath.setNamespaceContext(new NamespaceContextFromElement(templateElement));
    String xpathExpr = buildXPathExpression(templateElement);
    try {
      XPathExpression xpathExpression = xpath.compile(xpathExpr);
      NodeList nodeList = (NodeList) xpathExpression.evaluate(workspaceElement, XPathConstants.NODESET);
      int length = nodeList.getLength();
      if (length == 1) {
        return (Element) nodeList.item(0);
      } else if (length == 0) {
        return null;
      } else {
        String message = length + " matches found for XPath " + xpathExpr + " in workspace XML at " + XmlMergeSupport.getXPath(workspaceElement, true);
        if ("true".equals(this.context.getVariables().get(FAIL_ON_AMBIGOUS_MERGE))) {
          throw new IllegalStateException(message);
        } else {
          this.context.warning(message);
        }
        return (Element) nodeList.item(0);
      }
    } catch (XPathExpressionException e) {
      throw new IllegalStateException("Failed to compile XPath expression " + xpath, e);
    }
  }

  /**
   * Builds the XPath expression for the given merge element based on the {@link #getId()} merge:id} value.
   *
   * @param element the {@link Element} for which to build the XPath expression
   * @return the XPath expression as a {@link String}.
   */
  private String buildXPathExpression(Element element) {

    String namespaceURI = element.getNamespaceURI();
    String localName = element.getLocalName();
    if (localName == null) {
      localName = element.getTagName();
    }
    String prefix = element.getPrefix();

    StringBuilder xpathBuilder = new StringBuilder(localName.length());
    if ((prefix != null) && !prefix.isEmpty()) {
      xpathBuilder.append(prefix).append(":");
    }
    xpathBuilder.append(localName);
    if (this.id.startsWith("@")) {
      String attributeName = this.id.substring(1);
      String attributeValue = element.getAttribute(attributeName);
      xpathBuilder.append('[').append(this.id).append("='").append(XmlMergeSupport.escapeSingleQuotes(attributeValue)).append("']");
    } else if (this.id.equals(XmlMergeSupport.XPATH_ELEMENT_NAME)) {
      xpathBuilder.append("[local-name()='").append(localName).append("']");
      if ((namespaceURI != null) && !namespaceURI.isEmpty()) {
        xpathBuilder.append(" and namespace-uri()='").append(namespaceURI).append('\'');
      }
    } else if (this.id.equals(XmlMergeSupport.XPATH_ELEMENT_TEXT)) {
      String textContent = element.getTextContent();
      xpathBuilder.append("[text()='").append(XmlMergeSupport.escapeSingleQuotes(textContent)).append("']");
    } else { // custom xpath like ../element[@attr='value']
      return this.id;
    }
    return xpathBuilder.toString();
  }

}
