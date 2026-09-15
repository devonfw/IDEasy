package com.devonfw.tools.ide.merge.xml;

import java.util.Locale;
import java.util.function.BiFunction;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
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
    int resultChildIndex = 0;
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
          resultChildIndex = replaceTextNode(resultElement, templateChild, resultChildIndex);
        }
      }
    }
  }

  /**
   * Replaces the next textual node of the result element with the text from the given template node, otherwise appends it. The search starts at the given
   * {@code resultChildIndex} so that an element with multiple textual children (mixed content) is merged node by node instead of collapsing all template
   * texts into the very first textual node of the result.
   *
   * @param resultElement the element to be updated
   * @param templateChild the new textual node ({@link Node#TEXT_NODE text} or {@link Node#CDATA_SECTION_NODE CDATA section})
   * @param resultChildIndex the index of the {@link Element#getChildNodes() child node} of {@code resultElement} to start searching at
   * @return the index of the {@link Element#getChildNodes() child node} of {@code resultElement} to continue searching at for the next textual node
   */
  protected int replaceTextNode(Element resultElement, Node templateChild, int resultChildIndex) {

    try {
      NodeList resultChildNodes = resultElement.getChildNodes();
      for (int i = resultChildIndex; i < resultChildNodes.getLength(); i++) {
        Node resultChild = resultChildNodes.item(i);
        if (XmlMergeSupport.isTextual(resultChild)) {
          if (!resultChild.getTextContent().isBlank()) {
            if (resultChild.getNodeType() == templateChild.getNodeType()) {
              resultChild.setTextContent(getText(templateChild));
            } else {
              // the template determines whether the text is a plain text node or a CDATA section
              resultElement.replaceChild(createTextualNode(resultElement, templateChild), resultChild);
            }
            return i + 1;
          }
        }
      }
      resultElement.appendChild(createTextualNode(resultElement, templateChild));
      return resultElement.getChildNodes().getLength();
    } catch (DOMException e) {
      throw new IllegalStateException("Failed to replace text node for element " + XmlMergeSupport.getXPath(resultElement), e);
    }
  }

  /**
   * @param templateChild the textual {@link Node} from the template.
   * @return the {@link Node#getTextContent() text content} to merge into the result document.
   */
  private static String getText(Node templateChild) {

    return templateChild.getTextContent().trim();
  }

  /**
   * @param resultElement the result {@link Element} owning the {@link Document} to create the {@link Node} for.
   * @param templateChild the textual {@link Node} from the template determining the {@link Node#getNodeType() node type} and text.
   * @return a new textual {@link Node} of the same {@link Node#getNodeType() type} as the given {@code templateChild}.
   */
  private static Node createTextualNode(Element resultElement, Node templateChild) {

    Document document = resultElement.getOwnerDocument();
    String text = getText(templateChild);
    if (templateChild.getNodeType() == Node.CDATA_SECTION_NODE) {
      return document.createCDATASection(text);
    }
    return document.createTextNode(text);
  }

  @Override
  public String toString() {

    return this.name().toLowerCase(Locale.ROOT);
  }
}
