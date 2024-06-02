package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeAttribute;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Abstract base class for merge strategies.
 */
public abstract class AbstractStrategy implements Strategy {

  /**
   * The context
   */
  protected final IdeContext context;

  /**
   * The matcher for matching elements in the XML document.
   */
  protected final ElementMatcher elementMatcher;

  /**
   * @param context the IDE context
   * @param elementMatcher the element matcher used for matching elements
   */
  public AbstractStrategy(IdeContext context, ElementMatcher elementMatcher) {

    this.context = context;
    this.elementMatcher = elementMatcher;
  }

  @Override
  public void merge(MergeElement updateElement, Document targetDocument) {

    MergeElement targetElement = elementMatcher.matchElement(updateElement, targetDocument);
    if (targetElement != null) {
      mergeElement(updateElement, targetElement);
    } else {
      appendElement(updateElement, targetDocument);
    }
  }

  /**
   * Merges the update element with the target element.
   *
   * @param sourceElement the source element containing merge annotations
   * @param targetElement the target element to be merged into
   */
  protected abstract void mergeElement(MergeElement sourceElement, MergeElement targetElement);

  /**
   * Appends the update element to the target document.
   *
   * @param updateElement the element to be appended
   * @param targetDocument the target document
   */
  protected void appendElement(MergeElement updateElement, Document targetDocument) {

    try {
      this.context.debug("Appending {}", updateElement.getXPath());
      updateAndRemoveNsAttributes(updateElement);
      Element parent = (Element) updateElement.getElement().getParentNode();
      MergeElement matchParent = elementMatcher.matchElement(new MergeElement(parent), targetDocument);
      if (matchParent != null) {
        Element importedNode = (Element) targetDocument.importNode(updateElement.getElement(), true);
        matchParent.getElement().appendChild(importedNode);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to append element: " + updateElement.getXPath(), e);
    }
  }

  /**
   * Updates the {@link ElementMatcher} and removes namespace attributes from the merge element.
   *
   * @param mergeElement the merge element whose id is to be updated and merge namespace attributes removed.
   */
  protected void updateAndRemoveNsAttributes(MergeElement mergeElement) {

    for (MergeAttribute attribute : mergeElement.getElementAttributes()) {
      if (attribute.isMergeNsIdAttr()) {
        elementMatcher.updateId(mergeElement.getQName(), attribute.getValue());
      }
    }
    mergeElement.removeMergeNsAttributes();

    for (MergeElement childElement : mergeElement.getChildElements()) {
      updateAndRemoveNsAttributes(childElement);
    }
  }
}
