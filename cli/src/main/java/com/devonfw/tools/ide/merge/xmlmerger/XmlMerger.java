package com.devonfw.tools.ide.merge.xmlmerger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
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
import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;

public class XmlMerger extends FileMerger {

  private static final DocumentBuilder DOCUMENT_BUILDER;

  private static final TransformerFactory TRANSFORMER_FACTORY;

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

  public XmlMerger(IdeContext context) {

    super(context);
  }

  @Override
  public void merge(Path setup, Path update, EnvironmentVariables resolver, Path workspace) {

    Document document = null;
    Path template = setup;
    Path target = workspace;
    boolean updateFileExists = Files.exists(update);
    if (Files.exists(workspace)) {
      if (!updateFileExists) {
        return; // nothing to do ...
      }
      document = load(workspace);
    } else if (Files.exists(setup)) {
      document = load(setup);
      target = setup;
    }
    if (updateFileExists) {
      template = update;
      if (document == null) {
        document = load(update);
      } else {
        Document updateDocument = load(update);
        merge(updateDocument, document, template, target);
      }
    }
    if (document != null) {
      resolve(document, resolver, false, template);
      save(document, workspace);
    }
  }

  /**
   * Merges the source document with the target document.
   *
   * @param sourceDocument The {@link Document} representing the source xml file.
   * @param targetDocument The {@link Document} representing the target xml file.
   * @param source {@link Path} to the source document.
   * @param target {@link Path} to the target document.
   */
  public void merge(Document sourceDocument, Document targetDocument, Path source, Path target) {

    this.context.debug("Merging {} with {} ...", source.getFileName().toString(), target.getFileName().toString());
    MergeElement sourceRoot = new MergeElement(sourceDocument.getDocumentElement(), source);
    MergeElement targetRoot = new MergeElement(targetDocument.getDocumentElement(), target);

    if (areRootsCompatible(sourceRoot, targetRoot)) {
      MergeStrategy strategy = MergeStrategy.of(sourceRoot.getMergingStrategy());
      strategy.merge(sourceRoot, targetRoot, new ElementMatcher(this.context));
    } else {
      this.context.warning("Root elements do not match. Skipping merge operation.");
    }
  }

  /**
   * Checks the compatibility (tagname and namespaceURI) of the given root elements to be merged.
   *
   * @param sourceRoot the {@link MergeElement} representing the root element of the source document.
   * @param targetRoot the {@link MergeElement} representing the root element of the target document.
   * @return {@code true} when the roots are compatible, otherwise {@code false}.
   */
  private boolean areRootsCompatible(MergeElement sourceRoot, MergeElement targetRoot) {

    Element sourceElement = sourceRoot.getElement();
    Element targetElement = targetRoot.getElement();

    // Check if tag names match
    if (!sourceElement.getTagName().equals(targetElement.getTagName())) {
      this.context.warning("Names of root elements of {} and {} don't match. Found {} and {}",
          sourceRoot.getDocumentPath(), targetRoot.getDocumentPath(),
          sourceElement.getTagName(), targetElement.getTagName());
      return false;
    }

    // Check if namespace URIs match (if they exist)
    String sourceNs = sourceElement.getNamespaceURI();
    String targetNs = targetElement.getNamespaceURI();
    if (sourceNs != null || targetNs != null) {
      if (!Objects.equals(sourceNs, targetNs)) {
        this.context.warning("URI of root elements of {} and {} don't match. Found {} and {}",
            sourceRoot.getDocumentPath(), targetRoot.getDocumentPath(),
            sourceNs, targetNs);
        return false;
      }
    }

    return true;
  }

  @Override
  public void inverseMerge(Path workspace, EnvironmentVariables variables, boolean addNewProperties, Path update) {

    if (!Files.exists(workspace) || !Files.exists(update)) {
      return;
    }
    Document updateDocument = load(update);
    Document workspaceDocument = load(workspace);
    resolve(updateDocument, variables, true, workspace.getFileName());
    MergeStrategy strategy = MergeStrategy.OVERRIDE;
    MergeElement sourceRoot = new MergeElement(workspaceDocument.getDocumentElement(), workspace);
    MergeElement targetRoot = new MergeElement(updateDocument.getDocumentElement(), update);
    strategy.merge(sourceRoot, targetRoot, null);
    save(updateDocument, update);
    this.context.debug("Saved changes in {} to {}", workspace.getFileName(), update);
  }

  public Document load(Path file) {

    try (InputStream in = Files.newInputStream(file)) {
      return DOCUMENT_BUILDER.parse(in);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load XML from: " + file, e);
    }
  }

  public void save(Document document, Path file) {

    ensureParentDirectoryExists(file);
    try {
      Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

      // Workaround:
      // Remove whitespace from the target document before saving, because if target XML Document is already formatted
      // then indent 2 keeps adding empty lines for nothing, and if we don't use indentation then appending/ overriding
      // isn't properly formatted.
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
      if (child.getNodeType() == Node.TEXT_NODE) {
        if (child.getTextContent().trim().isEmpty()) {
          node.removeChild(child);
          i--;
        }
      } else {
        removeWhitespace(child);
      }
    }
  }

  private void resolve(Document document, EnvironmentVariables resolver, boolean inverse, Object src) {

    NodeList nodeList = document.getElementsByTagName("*");
    for (int i = 0; i < nodeList.getLength(); i++) {
      Element element = (Element) nodeList.item(i);
      resolve(element, resolver, inverse, src);
    }
  }

  private void resolve(Element element, EnvironmentVariables variables, boolean inverse, Object src) {

    resolve(element.getAttributes(), variables, inverse, src);
    NodeList nodeList = element.getChildNodes();

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node instanceof Text text) {
        String value = text.getNodeValue();
        String resolvedValue;
        if (inverse) {
          resolvedValue = variables.inverseResolve(value, src);
        } else {
          resolvedValue = variables.resolve(value, src, this.legacySupport);
        }
        text.setNodeValue(resolvedValue);
      }
    }
  }

  private void resolve(NamedNodeMap attributes, EnvironmentVariables variables, boolean inverse, Object src) {

    for (int i = 0; i < attributes.getLength(); i++) {
      Attr attribute = (Attr) attributes.item(i);
      String value = attribute.getValue();
      String resolvedValue;
      if (inverse) {
        resolvedValue = variables.inverseResolve(value, src);
      } else {
        resolvedValue = variables.resolve(value, src, this.legacySupport);
      }
      attribute.setValue(resolvedValue);
    }
  }
}
