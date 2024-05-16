package com.devonfw.tools.ide.merge.xmlMerger;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.merge.FileMerger;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class NewXmlMerger extends FileMerger {

  private static final DocumentBuilder DOCUMENT_BUILDER;

  private static final TransformerFactory TRANSFORMER_FACTORY;

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
  public NewXmlMerger(IdeContext context) {

    super(context);
  }

  /**
   * @param file the {@link Path} to load.
   * @return the loaded XML {@link Document}.
   */
  public static Document load(Path file) {

    try (InputStream in = Files.newInputStream(file)) {
      return DOCUMENT_BUILDER.parse(in);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load XML from: " + file, e);
    }
  }

  /**
   * @param document the XML {@link Document} to save.
   * @param file the {@link Path} to save to.
   */
  public static void save(Document document, Path file) {

    ensureParentDirectoryExists(file);
    try {
      Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
      DOMSource source = new DOMSource(document);
      StreamResult result = new StreamResult(file.toFile());
      transformer.transform(source, result);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to save XML to file: " + file, e);
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
          resolvedValue = variables.resolve(value, src);
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
        resolvedValue = variables.resolve(value, src);
      }
      attribute.setValue(resolvedValue);
    }
  }

  @Override
  public void merge(Path setup, Path update, EnvironmentVariables resolver, Path workspace) {

    Document document = null;
    boolean updateFileExists = Files.exists(update);
    if (Files.exists(workspace)) {
      if (!updateFileExists) {
        return; // nothing to do ...
      }
      document = load(workspace);
    } else if (Files.exists(setup)) {
      document = load(setup);
    }
    if (updateFileExists) {
      if (document == null) {
        document = load(update);
      } else {
        Document updateDocument = load(update);
        merge(updateDocument, document, true, true);
      }
    }
    resolve(document, resolver, false, workspace.getFileName());
    save(document, workspace);
  }

  /**
   * @param workspace the workspace {@link Path} where to get the changes from.
   * @param variables the {@link EnvironmentVariables} to
   * {@link EnvironmentVariables#inverseResolve(String, Object) inverse resolve variables}.
   * @param addNewProperties - {@code true} to also add new properties to the {@code updateFile}, {@code false}
   * otherwise (to only update existing properties).
   * @param update the update {@link Path}
   */
  @Override
  public void inverseMerge(Path workspace, EnvironmentVariables variables, boolean addNewProperties, Path update) {

  }
}