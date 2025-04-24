package com.devonfw.tools.ide.merge.xml;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.merge.FileMerger;
import com.devonfw.tools.ide.merge.xml.matcher.ElementMatcher;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * {@link FileMerger} for XML files.
 */
public class XmlMerger extends FileMerger implements XmlMergeSupport {

  private static final DocumentBuilder DOCUMENT_BUILDER;

  private static final TransformerFactory TRANSFORMER_FACTORY;

  protected final boolean legacyXmlSupport;

  /** The namespace URI for this XML merger. */
  public static final String MERGE_NS_URI = "https://github.com/devonfw/IDEasy/merge";

  static {
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      DOCUMENT_BUILDER = documentBuilderFactory.newDocumentBuilder();
      TRANSFORMER_FACTORY = TransformerFactory.newInstance();
    } catch (Exception e) {
      throw new IllegalStateException("Invalid XML DOM support in JDK.", e);
    }
  }

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public XmlMerger(IdeContext context) {

    super(context);
    this.legacyXmlSupport = Boolean.TRUE.equals(IdeVariables.IDE_XML_MERGE_LEGACY_SUPPORT_ENABLED.get(context));
  }

  @Override
  protected void doMerge(Path setup, Path update, EnvironmentVariables resolver, Path workspace) {

    XmlMergeDocument workspaceDocument = null;
    boolean updateFileExists = Files.exists(update);
    boolean workspaceFileExists = Files.exists(workspace);
    if (workspaceFileExists) {
      if (!updateFileExists) {
        return; // nothing to do ...
      }
      workspaceDocument = load(workspace);
    } else if (Files.exists(setup)) {
      workspaceDocument = loadAndResolve(setup, resolver);
    }
    Document resultDocument = null;
    if (updateFileExists) {
      XmlMergeDocument templateDocument = loadAndResolve(update, resolver);
      if (workspaceDocument == null) {
        resultDocument = templateDocument.getDocument();
      } else {
        resultDocument = merge(templateDocument, workspaceDocument, workspaceFileExists);
        if ((resultDocument == null) && !workspaceFileExists) {
          // if the merge failed due to incompatible roots and we have no workspace file
          // then at least we should take the resolved setup file as result
          resultDocument = workspaceDocument.getDocument();
        }
      }
    } else if (workspaceDocument != null) {
      resultDocument = workspaceDocument.getDocument();
    }
    if (resultDocument != null) {
      XmlMergeDocument result = new XmlMergeDocument(resultDocument, workspace);
      XmlMergeSupport.removeMergeNsAttributes(result);
      save(result);
    }
  }

  /**
   * Merges the source document with the target document.
   *
   * @param templateDocument the {@link XmlMergeDocument} representing the template xml file from the settings.
   * @param workspaceDocument the {@link XmlMergeDocument} of the actual source XML file (typically from the workspace of the real IDE) to merge with the
   *     {@code templateDocument}.
   * @param workspaceFileExists indicates whether the workspace document already exists or if setup templates are loaded
   * @return the merged {@link Document}.
   */
  public Document merge(XmlMergeDocument templateDocument, XmlMergeDocument workspaceDocument, boolean workspaceFileExists) {

    Document resultDocument;
    Path source = templateDocument.getPath();
    Path template = workspaceDocument.getPath();
    this.context.debug("Merging {} into {} ...", template, source);
    Element templateRoot = templateDocument.getRoot();
    QName templateQName = XmlMergeSupport.getQualifiedName(templateRoot);
    Element workspaceRoot = workspaceDocument.getRoot();
    QName workspaceQName = XmlMergeSupport.getQualifiedName(workspaceRoot);
    if (templateQName.equals(workspaceQName)) {
      XmlMergeStrategy strategy = XmlMergeSupport.getMergeStrategy(templateRoot);
      if (strategy == null) {
        strategy = XmlMergeStrategy.COMBINE; // default strategy used as fallback
      }
      if (templateRoot.lookupPrefix(MERGE_NS_URI) == null) {
        if (this.legacyXmlSupport) {
          if (workspaceFileExists) {
            strategy = XmlMergeStrategy.OVERRIDE;
          } else {
            strategy = XmlMergeStrategy.KEEP;
          }
        } else {
          this.context.warning(
              "XML merge namespace not found. If you are working in a legacy devonfw-ide project, please set IDE_XML_MERGE_LEGACY_SUPPORT_ENABLED=true to "
                  + "proceed correctly.");
        }
      }
      ElementMatcher elementMatcher = new ElementMatcher(this.context);
      strategy.merge(templateRoot, workspaceRoot, elementMatcher);
      resultDocument = workspaceDocument.getDocument();
    } else {
      this.context.error("Cannot merge XML template {} with root {} into XML file {} with root {} as roots do not match.", templateDocument.getPath(),
          templateQName, workspaceDocument.getPath(), workspaceQName);
      return null;
    }
    return resultDocument;
  }

  @Override
  public void inverseMerge(Path workspace, EnvironmentVariables variables, boolean addNewProperties, Path update) {

    if (!Files.exists(workspace) || !Files.exists(update)) {
      return;
    }
    throw new UnsupportedOperationException("not implemented!");
  }

  /**
   * {@link #load(Path) Loads} and {@link #resolveDocument(XmlMergeDocument, EnvironmentVariables, boolean) resolves} XML from the given file.
   *
   * @param file the {@link Path} to the XML file.
   * @param variables the {@link EnvironmentVariables}.
   * @return the loaded {@link XmlMergeDocument}.
   */
  public XmlMergeDocument loadAndResolve(Path file, EnvironmentVariables variables) {

    XmlMergeDocument document = load(file);
    resolveDocument(document, variables, false);
    return document;
  }

  /**
   * @param file the {@link Path} to the XML file.
   * @return the loaded {@link XmlMergeDocument}.
   */
  public XmlMergeDocument load(Path file) {

    try (InputStream in = Files.newInputStream(file)) {
      Document document = DOCUMENT_BUILDER.parse(in);
      return new XmlMergeDocument(document, file);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load XML from: " + file, e);
    }
  }

  /**
   * @param document the XML {@link XmlMergeDocument} to save.
   */
  public void save(XmlMergeDocument document) {

    save(document.getDocument(), document.getPath());
  }

  /**
   * @param document the XML {@link Document} to save.
   * @param file the {@link Path} to the file where to save the XML.
   */
  public void save(Document document, Path file) {

    ensureParentDirectoryExists(file);
    try {
      Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

      // Workaround:
      // Remove whitespace from the target document before saving, because if target XML Document is already formatted
      // then indent 2 keeps adding empty lines for nothing, and if we don't use indentation then appending/ overriding
      // isn't properly formatted.
      // https://bugs.openjdk.org/browse/JDK-8262285
      removeWhitespace(document.getDocumentElement());

      DOMSource source = new DOMSource(document);
      StreamResult result = new StreamResult(file.toFile());
      transformer.transform(source, result);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to save XML to file: " + file, e);
    }
  }

  private void removeWhitespace(Node node) {

    NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      short nodeType = child.getNodeType();
      if (nodeType == Node.TEXT_NODE) {
        if (child.getTextContent().trim().isEmpty()) {
          node.removeChild(child);
          i--;
        }
      } else if (nodeType == Node.ELEMENT_NODE) {
        removeWhitespace(child);
      }
    }
  }

  private void resolveDocument(XmlMergeDocument document, EnvironmentVariables variables, boolean inverse) {

    NodeList nodeList = document.getAllElements();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Element element = (Element) nodeList.item(i);
      resolveElement(element, variables, inverse, document.getPath());
    }
  }

  private void resolveElement(Element element, EnvironmentVariables variables, boolean inverse, Object src) {

    resolveAttributes(element.getAttributes(), variables, inverse, src);
    NodeList nodeList = element.getChildNodes();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (XmlMergeSupport.isTextual(node)) {
        resolveValue(node, variables, inverse, src);
      }
    }
  }

  private void resolveAttributes(NamedNodeMap attributes, EnvironmentVariables variables, boolean inverse, Object src) {

    for (int i = 0; i < attributes.getLength(); i++) {
      Attr attribute = (Attr) attributes.item(i);
      resolveValue(attribute, variables, inverse, src);
    }
  }

  private void resolveValue(Node node, EnvironmentVariables variables, boolean inverse, Object src) {
    String value = node.getNodeValue();
    String resolvedValue;
    if (inverse) {
      resolvedValue = variables.inverseResolve(value, src);
    } else {
      resolvedValue = variables.resolve(value, src, this.legacySupport);
    }
    node.setNodeValue(resolvedValue);
  }

  @Override
  protected boolean doUpgrade(Path workspaceFile) throws Exception {

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.parse(workspaceFile.toFile());
    checkForXmlNamespace(document, workspaceFile);
    boolean modified = updateWorkspaceXml(document.getDocumentElement());
    if (modified) {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(document);
      try (BufferedWriter writer = Files.newBufferedWriter(workspaceFile)) {
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
      }
    }
    return modified;
  }

  private boolean updateWorkspaceXml(Element element) {

    boolean modified = false;
    NamedNodeMap attributes = element.getAttributes();
    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        Node node = attributes.item(i);
        if (node instanceof Attr attribute) {
          String value = attribute.getValue();
          String migratedValue = upgradeWorkspaceContent(value);
          if (!migratedValue.equals(value)) {
            modified = true;
            attribute.setValue(migratedValue);
          }
        }
      }
    }

    NodeList childNodes = element.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      boolean childModified = false;
      if (childNode instanceof Element childElement) {
        childModified = updateWorkspaceXml(childElement);
      } else if (childNode instanceof Text childText) {
        String text = childText.getTextContent();
        String migratedText = upgradeWorkspaceContent(text);
        childModified = !migratedText.equals(text);
        if (childModified) {
          childText.setTextContent(migratedText);
        }
      }
      if (childModified) {
        modified = true;
      }
    }
    return modified;
  }

  private void checkForXmlNamespace(Document document, Path workspaceFile) {

    NamedNodeMap attributes = document.getDocumentElement().getAttributes();
    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        Node node = attributes.item(i);
        String uri = node.getNamespaceURI();
        if (MERGE_NS_URI.equals(uri)) {
          return;
        }
      }
    }
    this.context.warning(
        "The XML file {} does not contain the XML merge namespace and seems outdated. For details see:\n"
            + "https://github.com/devonfw/IDEasy/blob/main/documentation/configurator.adoc#xml-merger", workspaceFile);
  }

}
