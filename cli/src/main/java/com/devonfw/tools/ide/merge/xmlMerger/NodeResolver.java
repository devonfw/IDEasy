package com.devonfw.tools.ide.merge.xmlMerger;

import com.devonfw.tools.ide.environment.EnvironmentVariables;
import org.w3c.dom.*;

public class NodeResolver {

  public static void resolve(Document document, EnvironmentVariables resolver, boolean inverse, Object src) {
    NodeList nodeList = document.getElementsByTagName("*");
    for (int i = 0; i < nodeList.getLength(); i++) {
      Element element = (Element) nodeList.item(i);
      resolve(element, resolver, inverse, src);
    }
  }

  public static void resolve(Element element, EnvironmentVariables variables, boolean inverse, Object src) {
    resolve(element.getAttributes(), variables, inverse, src);
    NodeList nodeList = element.getChildNodes();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node instanceof Text text) {
        String value = text.getNodeValue();
        String resolvedValue;
        if (inverse) {
          resolvedValue = variables.inverseResolve(value, src);
        } else {
          resolvedValue = variables.resolve(value, src);
        }
        text.setNodeValue(resolvedValue);
      }
    }
  }

  public static void resolve(NamedNodeMap attributes, EnvironmentVariables variables, boolean inverse, Object src) {
    for (int i = 0; i < attributes.getLength(); i++) {
      Attr attribute = (Attr) attributes.item(i);
      String value = attribute.getValue();
      String resolvedValue;
      if (inverse) {
        resolvedValue = variables.inverseResolve(value, src);
      } else {
        resolvedValue = variables.resolve(value, src);
      }
      attribute.setValue(resolvedValue);
    }
  }
}
