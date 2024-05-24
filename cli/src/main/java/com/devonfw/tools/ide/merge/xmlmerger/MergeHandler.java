  package com.devonfw.tools.ide.merge.xmlmerger;

  import com.devonfw.tools.ide.context.IdeContext;
  import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
  import com.devonfw.tools.ide.merge.xmlmerger.model.MergeAttribute;
  import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
  import com.devonfw.tools.ide.merge.xmlmerger.model.MergeStrategy;
  import org.w3c.dom.*;

  import javax.xml.namespace.QName;
  import java.util.HashMap;
  import java.util.Map;

  public class MergeHandler {

    private final ElementMatcher elementMatcher;

    private final IdeContext context;

    private Map<QName, String> qNameIdMap;

    public MergeHandler(ElementMatcher elementMatcher, IdeContext context) {

      this.elementMatcher = elementMatcher;
      this.context = context;
      this.qNameIdMap = new HashMap<>();
    }

    public void merge(Document updateDocument, Document targetDocument) {

      MergeElement updateRootElement = new MergeElement(updateDocument.getDocumentElement());
      mergeElements(updateRootElement, targetDocument);
    }

    private void mergeElements(MergeElement updateElement, Document targetDocument) {

      context.debug("Merging {} ...", updateElement.getXPath());
      Element matchedTargetElement = elementMatcher.matchElement(updateElement, targetDocument, qNameIdMap);

      if (matchedTargetElement != null) {
        this.context.debug("Match found for {}", updateElement.getXPath());
        MergeElement targetElement = new MergeElement(matchedTargetElement);
        MergeStrategy strategy = updateElement.getMergingStrategy();
        context.debug("Merge strategy {}", strategy);

        switch (strategy) {
          case COMBINE:
            combineElements(updateElement, targetElement);
            break;
          case OVERRIDE:
            overrideElement(updateElement, targetElement);
            break;
          case KEEP:
            context.debug("Element already exists, skipping merge");
            break;
          default:
            context.debug("Unknown merging strategy, skipping element {}", updateElement.getXPath());
        }
      } else {
        context.debug("No match found for {}, appending element", updateElement.getXPath());
        appendElement(updateElement, targetDocument);
      }
    }

    private void combineElements(MergeElement updateElement, MergeElement targetElement) {

      context.debug("Combining attributes for {}", updateElement.getXPath());
      combineAttributes(updateElement, targetElement);

      context.debug("Combining child nodes for {}", updateElement.getXPath());
      combineChildNodes(updateElement, targetElement);
    }

    private void combineAttributes(MergeElement updateElement, MergeElement targetElement) {

      for (MergeAttribute updateAttr : updateElement.getElementAttributes()) {
        if (!updateAttr.isMergeNSAttr()) {
         // targetElement.getElement().setAttribute(updateAttr.getName(), updateAttr.getValue());
          String namespaceURI = updateAttr.getAttr().getNamespaceURI();
          String attrName = updateAttr.getAttr().getLocalName();
          targetElement.getElement().setAttributeNS(namespaceURI, attrName, updateAttr.getValue());
        }
      }
    }

    private void combineChildNodes(MergeElement updateElement, MergeElement targetElement) {

      NodeList updateChildNodes = updateElement.getElement().getChildNodes();

      for (int i = 0; i < updateChildNodes.getLength(); i++) {
        Node updateChild = updateChildNodes.item(i);
        if (updateChild.getNodeType() == Node.ELEMENT_NODE) {
          MergeElement mergeUpdateChild = new MergeElement((Element) updateChild);
          mergeElements(mergeUpdateChild, targetElement.getElement().getOwnerDocument());
        } else if (updateChild.getNodeType() == Node.TEXT_NODE || updateChild.getNodeType() == Node.CDATA_SECTION_NODE) {
          targetElement.getElement().setTextContent(updateChild.getTextContent());
        }
      }
    }

    private void overrideElement(MergeElement updateElement, MergeElement targetElement) {

      this.context.debug("Overriding element {}", updateElement.getXPath());
      if (updateElement.isRootElement()) {
        // Handle root element override
        Document targetDocument = targetElement.getElement().getOwnerDocument();
        updateElement.removeMergeNSAttributes();
        Node newRoot = targetDocument.importNode(updateElement.getElement(), true);
        targetDocument.replaceChild(newRoot, targetDocument.getDocumentElement());
      } else {
        // Handle non-root element override
        Node parentNode = targetElement.getElement().getParentNode();
        if (parentNode != null) {
          updateElement.removeMergeNSAttributes();
          Element importedElement = (Element) parentNode.getOwnerDocument().importNode(updateElement.getElement(), true);
          parentNode.replaceChild(importedElement, targetElement.getElement());
        }
      }
    }

    private void appendElement(MergeElement updateElement, Document targetDocument) {

      // add all merge:id to the map, remove all merge ns attributes, then append ... 

      context.debug("Appending element {}", updateElement.getXPath());
      Element parent = (Element) updateElement.getElement().getParentNode();
      Element matchParent = elementMatcher.matchElement(new MergeElement(parent), targetDocument, qNameIdMap);
      if (matchParent != null) {
        Element importedNode = (Element) targetDocument.importNode(updateElement.getElement(), true);
        matchParent.appendChild(importedNode);
      } else {
        // should actually never happen, since appending is for children and parent is at least root
        this.context.debug("Cannot find matching parent element for {} ", updateElement.getXPath());
      }
    }
  }
