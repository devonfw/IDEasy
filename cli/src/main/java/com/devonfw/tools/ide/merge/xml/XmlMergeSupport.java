package com.devonfw.tools.ide.merge.xml;

import java.io.StringWriter;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility class with static helper methods for XML merging.
 */
public interface XmlMergeSupport {

  /**
   * The XML namespace used by this {@link XmlMerger} to configure merge specific meta-information.
   *
   * @see XmlMergeSupport#hasMergeNamespace(Attr)
   */
  String MERGE_NS_URI = "https://github.com/devonfw/IDEasy/merge";

  /** {@link Attr#getName() Attribute name} {@value} */
  String ATTR_ID = "id";

  /** XPath for {@link Attr} {@value} */
  String XPATH_ATTR_ID = "@id";

  /** {@link Attr#getName() Attribute name} {@value} */
  String ATTR_NAME = "name";

  /** XPath for {@link Attr} {@value} */
  String XPATH_ATTR_NAME = "@name";

  /** XPath for {@link Element#getTagName() element tag name}: {@value} */
  String XPATH_ELEMENT_NAME = "name()";

  /** {@link Attr#getName() Attribute name} {@value} */
  String ATTR_STRATEGY = "strategy";

  /** XPath for {@link Element#getTextContent() element text}: {@value} */
  String XPATH_ELEMENT_TEXT = "text()";

  /**
   * @param attribute the XML {@link Attr} to check.
   * @return {@code true} if the {@code attribute} is specific for our XML merger and related to {@link XmlMergeSupport#MERGE_NS_URI} (that needs to be omitted
   *     in the XML output), {@code false} otherwise (regular {@code attribute} for the target payload).
   */
  static boolean hasMergeNamespace(Attr attribute) {

    if (MERGE_NS_URI.equals(attribute.getNamespaceURI())) {
      return true;
    } else {
      return "xmlns".equals(attribute.getPrefix()) && MERGE_NS_URI.equals(attribute.getValue());
    }
  }

  /**
   * @param element the {@link Element} for which the XPath is requested.
   * @return the XPath expression.
   */
  static String getXPath(Element element) {

    return getXPath(element, false);
  }

  /**
   * @param element the {@link Element} for which the XPath is requested.
   * @param includeAttributes {@code true} to also include the attributes of the {@link Element}s to the XPath (for debugging), {@code false} otherwise.
   * @return the XPath expression.
   */
  static String getXPath(Element element, boolean includeAttributes) {

    StringBuilder sb = new StringBuilder();
    getXPath(sb, element, includeAttributes);
    return sb.toString();
  }

  private static void getXPath(StringBuilder sb, Element element, boolean includeAttributes) {

    Node parent = element.getParentNode();
    if ((parent != null) && (parent.getNodeType() == Node.ELEMENT_NODE)) {
      getXPath(sb, (Element) parent, includeAttributes);
    }
    sb.append('/');
    // for qualified node this will append «prefix»:«local-name» and then the matching of XPath depends on the chosen prefix so it would not match
    // «prefix2»:«local-name» even if «prefix» and «prefix2» would resolve to the same URL (each in their own XML document).
    appendName(sb, element);
    if (includeAttributes) {
      sb.append('[');
      appendAttributes(sb, element, "@", false);
      sb.append(']');
    }
  }

  /**
   * @param sb the {@link StringBuilder} where to {@link StringBuilder#append(String) append} the XML.
   * @param node the {@link Node} ({@link Element} or {@link Attr}) with the name to append.
   */
  static void appendName(StringBuilder sb, Node node) {

    sb.append(node.getNodeName());
  }

  /**
   * @param sb the {@link StringBuilder} where to {@link StringBuilder#append(String) append} the XML.
   * @param element the {@link Element} {@link Element#getAttributes() containing the attributes} to append.
   */
  static void appendAttributes(StringBuilder sb, Element element) {

    appendAttributes(sb, element, "", true);
  }

  /**
   * @param sb the {@link StringBuilder} where to {@link StringBuilder#append(String) append} the XML.
   * @param element the {@link Element} {@link Element#getAttributes() containing the attributes} to append.
   * @param attributePrefix the prefix for each attribute (typically the empty string but may be "@" for XPath syntax).
   * @param includeMergeNs {@code true} to also include attributes {@link #hasMergeNamespace(Attr) with merge namespace}, {@code false} otherwise.
   */
  static void appendAttributes(StringBuilder sb, Element element, String attributePrefix, boolean includeMergeNs) {
    NamedNodeMap attributes = element.getAttributes();
    int attributeCount = attributes.getLength();
    boolean separator = false;
    for (int i = 0; i < attributeCount; i++) {
      Attr attribute = (Attr) attributes.item(i);
      if (includeMergeNs || !hasMergeNamespace(attribute)) {
        if (separator) {
          sb.append(" ");
        } else {
          separator = true;
        }
        sb.append(attributePrefix);
        appendName(sb, attribute);
        sb.append("='");
        sb.append(escapeSingleQuotes(attribute.getValue()));
        sb.append('\'');
      }
    }
  }

  /**
   * @param value the {@link String} value.
   * @return the given {@link String} with potential single quotes escaped for XML (e.g. to use in attributes).
   */
  static String escapeSingleQuotes(String value) {

    return value.replace("'", "&apos;");
  }

  /**
   * @param element the {@link Element}.
   * @return the {@link QName} (URI + local name).
   */
  static QName getQualifiedName(Element element) {

    String namespaceURI = element.getNamespaceURI();
    String localName = element.getLocalName();
    if (localName == null) {
      localName = element.getTagName();
    }
    return new QName(namespaceURI, localName);
  }

  /**
   * @param node the {@link Node} to check.
   * @return {@code true} if the {@link Node} is a {@link Node#TEXT_NODE text node} or a {@link Node#CDATA_SECTION_NODE CDATA section}.
   */
  static boolean isTextual(Node node) {

    short nodeType = node.getNodeType();
    return (nodeType == Node.TEXT_NODE) || (nodeType == Node.CDATA_SECTION_NODE);
  }

  /**
   * @param document the {@link XmlMergeSupport} from which to remove all {@link #hasMergeNamespace(Attr) merge namespace} {@link Attr attributes}.
   */
  static void removeMergeNsAttributes(XmlMergeDocument document) {

    NodeList nodeList = document.getAllElements();
    for (int i = nodeList.getLength() - 1; i >= 0; i--) {
      Element element = (Element) nodeList.item(i);
      removeMergeNsAttributes(element);
    }
  }

  /**
   * @param element the {@link Element} from which to remove {@link #hasMergeNamespace(Attr) merge namespace} {@link Attr attributes}.
   */
  static void removeMergeNsAttributes(Element element) {

    removeAttributes(element, XmlMergeSupport::hasMergeNamespace);
  }

  /**
   * @param element the {@link Element} from which to remove matching {@link Attr attributes}.
   * @param filter the {@link Predicate} that {@link Predicate#test(Object) decides} if an {@link Attr attribute} shall be removed (if {@code true}) or
   *     not.
   */
  static void removeAttributes(Element element, Predicate<Attr> filter) {

    NamedNodeMap attributes = element.getAttributes();
    // we iterate backwards so we do not cause errors by removing attributes inside the loop
    for (int i = attributes.getLength() - 1; i >= 0; i--) {
      Attr attribute = (Attr) attributes.item(i);
      if (filter.test(attribute)) {
        element.removeAttributeNode(attribute);
      }
    }
  }

  /**
   * @param templateElement the template {@link Element} with the {@link Attr attributes} to combine.
   * @param resultElement the workspace {@link Element} where to add the {@link Attr attributes} to combine.
   * @param attributeMerger the {@link BiFunction} that decides how to merge {@link Attr attributes} with identical name or {@code null} to always override
   *     template attribute value in workspace attribute.
   */
  static void combineAttributes(Element templateElement, Element resultElement, BiFunction<Attr, Attr, String> attributeMerger) {

    NamedNodeMap attributes = templateElement.getAttributes();
    int length = attributes.getLength();
    for (int i = 0; i < length; i++) {
      Attr attribute = (Attr) attributes.item(i);
      if (!XmlMergeSupport.hasMergeNamespace(attribute)) {
        String namespaceUri = attribute.getNamespaceURI();
        String attrName = attribute.getName();
        String attrValue = attribute.getValue();
        if ((namespaceUri != null) && !namespaceUri.isEmpty()) {
          String prefix = attribute.getPrefix();
          if ((prefix != null) && !prefix.isEmpty()) {
            if (resultElement.getAttributeNodeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix) == null) {
              resultElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + prefix, namespaceUri);
            }
          }
          resultElement.setAttributeNS(namespaceUri, attrName, attrValue);
        } else {
          if (attributeMerger != null) {
            Attr workspaceAttr = resultElement.getAttributeNode(attrName);
            if (workspaceAttr != null) {
              if (!Objects.equals(attrValue, workspaceAttr.getValue())) {
                // attribute already exists in workspaceElement but has a different value - let attributeMerger decide how to merge
                attrValue = attributeMerger.apply(attribute, workspaceAttr);
              }
            }
          }
          resultElement.setAttribute(attrName, attrValue);
        }

      }
    }
  }

  /**
   * @param element the {@link Element} to get the "merge:id" for.
   * @return the merge ID ({@link javax.xml.xpath.XPath} expression). Will fall back following convention over configuration if "merge:id" is not explicitly
   *     configured. Will be {@code null} if no fallback could be found.
   */
  static String getMergeId(Element element) {

    String id = element.getAttributeNS(MERGE_NS_URI, ATTR_ID);
    if (!id.isEmpty()) {
      return id;
    }
    if (element.hasAttribute(ATTR_ID)) {
      return XPATH_ATTR_ID;
    } else if (element.hasAttribute(ATTR_NAME)) {
      return XPATH_ATTR_NAME;
    }
    NamedNodeMap attributes = element.getAttributes();
    int attributeCount = attributes.getLength();
    if (attributeCount == 0) {
      return XPATH_ELEMENT_NAME;
    }
    id = XPATH_ELEMENT_NAME;
    for (int i = 0; i < attributeCount; i++) {
      Attr attribute = (Attr) attributes.item(i);
      if (!hasMergeNamespace(attribute)) {
        if (id != XPATH_ELEMENT_NAME) {
          id = "@" + attribute.getName();
        } else {
          id = null;
          break;
        }
      }
    }
    return id;
  }

  /**
   * @param element the {@link Element} to get the "merge:strategy" for.
   * @return the {@link XmlMergeStrategy} of this element or {@code null} if undefined.
   */
  static XmlMergeStrategy getMergeStrategy(Element element) {

    String strategy = element.getAttributeNS(MERGE_NS_URI, ATTR_STRATEGY);
    if (!strategy.isEmpty()) {
      return XmlMergeStrategy.of(strategy);
    }
    return null;
  }

  /**
   * @param node the {@link Node}.
   * @return the XML of the given {@link Node} for debugging (
   */
  static String getXml(Node node) {

    try {
      StringWriter writer = new StringWriter();
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.transform(new DOMSource(node), new StreamResult(writer));
      return writer.toString();
    } catch (Exception e) {
      return e.toString();
    }
  }

}
