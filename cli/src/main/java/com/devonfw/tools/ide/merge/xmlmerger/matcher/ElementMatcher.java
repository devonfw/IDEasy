package com.devonfw.tools.ide.merge.xmlmerger.matcher;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeAttribute;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * The ElementMatcher class is responsible for matching XML elements in a target document based on the provided update
 * elements.
 */
public class ElementMatcher {

  private final IdeContext context;

  private final Map<QName, IdComputer> qNameIdMap;

  public ElementMatcher(IdeContext context) {

    this.context = context;
    qNameIdMap = new HashMap<>();
  }

  /**
   * Updates the ID strategy for a given QName (qualified name) of an XML element.
   *
   * @param qname the qualified name (ns + local name) of the XML element
   * @param id the ID value (value of merge:id) to be used for matching the element
   */
  public void updateId(QName qname, String id) {

    IdComputer idComputer = new IdComputer(id);
    IdComputer duplicate = qNameIdMap.put(qname, idComputer);
    if (duplicate != null) {
      this.context.debug("ID replaced for element '{}': old ID '{}' -> new ID '{}'", qname, duplicate.getId(),
          idComputer.getId());
    }
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

    IdComputer idComputer = qNameIdMap.get(qName);
    if (idComputer == null) {
      if (id.isEmpty()) {
        // handle case where element has no attribute
        if (sourceElement.getElementAttributes().isEmpty()) {
          // use name as id
          id = sourceElement.getElement().getLocalName();
        } else {
          // look for id or name attributes
          String idAttr = sourceElement.getElement().getAttribute("id");
          if (idAttr.isEmpty()) {
            idAttr = sourceElement.getElement().getAttribute("name");
            if (idAttr.isEmpty()) {
              throw new IllegalStateException("No merge:id value defined for element " + sourceElement.getXPath());
            }
          }
        }
      }
      updateId(qName, id);
      idComputer = qNameIdMap.get(qName);
    }
    Element matchedNode = idComputer.evaluateExpression(sourceElement, targetElement);
    if (matchedNode != null) {
      return new MergeElement(matchedNode, targetElement.getDocumentPath());
    }
    return null;
  }
}