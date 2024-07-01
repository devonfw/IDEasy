package com.devonfw.tools.ide.merge.xmlmerger.matcher;

import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * The ElementMatcher class is responsible for matching XML elements in a target document based on the provided update elements.
 */
public class ElementMatcher {

  private final Map<QName, IdComputer> qNameIdMap;

  public ElementMatcher() {

    qNameIdMap = new HashMap<>();
  }

  /**
   * Updates the ID strategy for a given QName (qualified name) of an XML element.
   *
   * @param qname the QName of the XML element
   * @param id the ID value to be used for matching the element
   */
  public void updateId(QName qname, String id) {

    qNameIdMap.put(qname, new IdComputer(id));
  }

  /**
   * Looks for an element matching the source element inside the target element.
   *
   * @param sourceElement the update element to be matched
   * @param targetElement the target element in which to match the element
   * @return the matched MergeElement if found, or {@code null} if not found
   */
  public MergeElement matchElement(MergeElement sourceElement, MergeElement targetElement) {

    String id = sourceElement.getId();
    if (id.isEmpty()) {
      IdComputer idComputer = qNameIdMap.get(sourceElement.getQName());
      if (idComputer == null) {
        throw new IllegalStateException("no Id value was defined for " + sourceElement.getXPath());
      }
      Element matchedNode = idComputer.evaluateExpression(sourceElement, targetElement);
      if (matchedNode != null) {
        return new MergeElement(matchedNode);
      }
    } else {
      updateId(sourceElement.getQName(), id);
      IdComputer idComputer = qNameIdMap.get(sourceElement.getQName());
      Element matchedNode = idComputer.evaluateExpression(sourceElement, targetElement);
      if (matchedNode != null) {
        return new MergeElement(matchedNode);
      }
    }
    return null;
  }
}