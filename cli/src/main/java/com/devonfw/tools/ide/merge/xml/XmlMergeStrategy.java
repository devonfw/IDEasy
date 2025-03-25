package com.devonfw.tools.ide.merge.xml;

import java.util.Locale;
import java.util.function.BiFunction;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.devonfw.tools.ide.merge.xml.matcher.ElementMatcher;

/**
 * Enum of merge strategies for XML elements.
 */
public enum XmlMergeStrategy {

  /**
   * Combines source and target elements. Overrides text nodes and attributes. This process is recursively applied to child elements. If the source element
   * exists in the target document, they are combined, otherwise, the source element is appended.
   */
  COMBINE {
    @Override
    protected void doMerge(Element templateElement, Element resultElement, ElementMatcher matcher) {

      BiFunction<Attr, Attr, String> attributeMerger = null; // here we can allow more configuration flexibility e.g. via merge:attribute-override="id,name"
      XmlMergeSupport.combineAttributes(templateElement, resultElement, attributeMerger);
      combineChildNodes(templateElement, resultElement, matcher);
    }
  },

  /**
   * Replaces the target element with the source element, without considering child elements. If the element exists in the target, it is overridden, otherwise,
   * it is appended.
   */
  OVERRIDE {
    @Override
    protected void doMerge(Element templateElement, Element resultElement, ElementMatcher matcher) {

      Node importedNode = resultElement.getOwnerDocument().importNode(templateElement, true);
      resultElement.getParentNode().replaceChild(importedNode, resultElement);
    }
  },

  /**
   * Keeps the existing target element intact if the source element exists in the target document, otherwise, it is appended.
   */
  KEEP {
    @Override
    protected void doMerge(Element templateElement, Element resultElement, ElementMatcher matcher) {

      // Do nothing, keep the existing element
    }
  };

  /**
   * @param templateElement the {@link Element} of the template XML file to merge.
   * @param resultElement the {@link Element} populated with the workspace XML file to merge into.
   * @param matcher the {@link ElementMatcher}.
   */
  public void merge(Element templateElement, Element resultElement, ElementMatcher matcher) {
    try {
      doMerge(templateElement, resultElement, matcher);
    } catch (XmlMergeException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new XmlMergeException("Merge strategy " + this + " failed on " + XmlMergeSupport.getXPath(templateElement, true), e);
    }
  }

  /**
   * Internal implementation of {@link #merge(Element, Element, ElementMatcher)}
   *
   * @param templateElement the {@link Element} of the template XML file to merge.
   * @param resultElement the {@link Element} populated with the workspace XML file to merge into.
   * @param matcher the {@link ElementMatcher}.
   */
  protected abstract void doMerge(Element templateElement, Element resultElement, ElementMatcher matcher);

  /**
   * Returns the MergeStrategy enum constant with the specified name.
   *
   * @param name the name of the enum constant to return
   * @return the enum constant with the specified name
   */
  public static XmlMergeStrategy of(String name) {

    return Enum.valueOf(XmlMergeStrategy.class, name.toUpperCase(Locale.ROOT));
  }

  /**
   * Combines child nodes (elements, text and CDATA) from the template into the result {@link Element}.
   *
   * @param templateElement the template {@link Element}.
   * @param resultElement the result {@link Element}.
   * @param elementMatcher the {@link ElementMatcher} used for matching elements.
   */
  protected void combineChildNodes(Element templateElement, Element resultElement, ElementMatcher elementMatcher) {

    NodeList templateChildNodes = templateElement.getChildNodes();
    for (int i = 0; i < templateChildNodes.getLength(); i++) {
      Node templateChild = templateChildNodes.item(i);
      if (templateChild.getNodeType() == Node.ELEMENT_NODE) {
        Element templateChildElement = (Element) templateChild;
        Element matchedResultElement = elementMatcher.matchElement(templateChildElement, resultElement);
        if (matchedResultElement != null) {
          XmlMergeStrategy mergeStrategy = XmlMergeSupport.getMergeStrategy(templateChildElement);
          if (mergeStrategy == null) {
            mergeStrategy = this; // fallback "this" will always be COMBINE
          }
          mergeStrategy.merge(templateChildElement, matchedResultElement, elementMatcher);
        } else {
          Node resultChildElement = resultElement.getOwnerDocument().importNode(templateChildElement, true);
          resultElement.appendChild(resultChildElement);
        }
      } else if (XmlMergeSupport.isTextual(templateChild)) {
        if (!templateChild.getTextContent().isBlank()) {
          replaceTextNode(resultElement, templateChild);
        }
      }
    }
  }

  /**
   * Replaces the text node in the target element with the text from the update element, otherwise appends it.
   *
   * @param resultElement the element to be updated
   * @param templateChild the new text node
   */
  protected void replaceTextNode(Element resultElement, Node templateChild) {

    try {
      NodeList targetChildNodes = resultElement.getChildNodes();
      for (int i = 0; i < targetChildNodes.getLength(); i++) {
        Node targetChild = targetChildNodes.item(i);
        if (XmlMergeSupport.isTextual(targetChild)) {
          if (!targetChild.getTextContent().isBlank()) {
            targetChild.setTextContent(templateChild.getTextContent().trim());
            return;
          }
        }
      }
      Node importedNode = resultElement.getOwnerDocument().importNode(templateChild, true);
      resultElement.appendChild(importedNode);
    } catch (DOMException e) {
      throw new IllegalStateException("Failed to replace text node for element " + XmlMergeSupport.getXPath(resultElement), e);
    }
  }

  @Override
  public String toString() {

    return this.name().toLowerCase(Locale.ROOT);
  }
}
