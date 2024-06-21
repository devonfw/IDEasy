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
   * Matches an update element in the target document.
   *
   * @param updateElement the update element to be matched
   * @param targetDocument the target document in which to match the element
   * @return the matched MergeElement if found, or {@code null} if not found
   */
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
    if (id.isEmpty()) {
      IdComputer idComputer = qNameIdMap.get(updateElement.getQName());
      if (idComputer == null) {
        throw new IllegalStateException("no Id value was defined for " + updateElement.getXPath());
      }
      Element matchedNode = idComputer.evaluateExpression(updateElement, targetDocument);
      if (matchedNode != null) {
        return new MergeElement(matchedNode);
      }
    } else {
      updateId(updateElement.getQName(), id);
      IdComputer idComputer = qNameIdMap.get(updateElement.getQName());
      Element matchedNode = idComputer.evaluateExpression(updateElement, targetDocument);
      if (matchedNode != null) {
        return new MergeElement(matchedNode);
      }
    }

    return null;
  }
}