package com.devonfw.tools.ide.merge.xmlmerger.matcher;

import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

    if (updateElement.isRootElement()) {  //TODO: check additionally for ns
      if (targetDocument.getDocumentElement().getTagName().equals(updateElement.getElement().getTagName())) {
        return new MergeElement(targetDocument.getDocumentElement());
      } else {
        throw new IllegalStateException("XML Documents don't have matching root elements!");
      }
    }
    String id = updateElement.getId(qNameIdMap);
    if (id == null || id.isEmpty()) { //TODO: if no id and element is empty then match by tagname
      if (updateElement.getElementAttributes().isEmpty()) {

      } else {

      }
    }
    try {
      XPath xpath = XPATH_FACTORY.newXPath();
      String xpathExpression = buildXPathExpression(updateElement, id);
      XPathExpression expr = xpath.compile(xpathExpression);
      NodeList result = (NodeList) expr.evaluate(targetDocument, XPathConstants.NODESET);
      for (int i = 0; i < result.getLength(); i++) {
        Element nodeElement = (Element) result.item(i);
        if (new MergeElement(nodeElement).getXPath().equals(updateElement.getXPath())) {
          return new MergeElement(nodeElement);
        }
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
    }else if (id.startsWith("@")) {
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
