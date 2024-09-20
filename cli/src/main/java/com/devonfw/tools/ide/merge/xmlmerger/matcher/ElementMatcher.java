package com.devonfw.tools.ide.merge.xmlmerger.matcher;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;

/**
 * The ElementMatcher class is responsible for matching XML elements in a target document based on the provided update elements.
 */
public class ElementMatcher {

  private final IdeContext context;

  private final Map<QName, IdComputer> qNameIdMap;

  public ElementMatcher(IdeContext context) {

    this.context = context;
    this.qNameIdMap = new HashMap<>();
  }

  private IdComputer createIdComputer(String id, QName qname, MergeElement sourceElement) {

    if ((id == null) || id.isEmpty()) {
      throw new IllegalStateException(
          "No merge:id value defined for element " + sourceElement.getXPath() + " in document " + sourceElement.getDocumentPath());
    }
    return new IdComputer(id);
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
    QName qName = sourceElement.getQName();

    IdComputer idComputer = this.qNameIdMap.computeIfAbsent(qName, k -> createIdComputer(id, qName, sourceElement));
    Element matchedNode = idComputer.evaluateExpression(sourceElement, targetElement);
    if (matchedNode != null) {
      return new MergeElement(matchedNode, targetElement.getDocumentPath());
    }
    return null;
  }
}
