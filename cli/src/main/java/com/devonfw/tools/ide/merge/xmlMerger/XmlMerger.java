package com.devonfw.tools.ide.merge.xmlMerger;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.merge.FileMerger;
import com.devonfw.tools.ide.merge.xmlMerger.matcher.ElementMatcher;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class XmlMerger extends FileMerger {

  private final MergeHandler mergeHandler;
  private static final DocumentBuilder DOCUMENT_BUILDER;
  private static final TransformerFactory TRANSFORMER_FACTORY;

  private final ElementMatcher elementMatcher;

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
    this.elementMatcher = new ElementMatcher();
    this.mergeHandler = new MergeHandler(this.elementMatcher, context);
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
        mergeHandler.merge(updateDocument, document);
      }
    }
    resolve(document, resolver, false, workspace.getFileName());
    save(document, workspace);
  }

  @Override
  public void inverseMerge(Path workspace, EnvironmentVariables variables, boolean addNewProperties, Path update) {


  }

  private void resolve(Document document, EnvironmentVariables resolver, boolean inverse, Object src) {

    NodeResolver.resolve(document, resolver, inverse, src);
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
      DOMSource source = new DOMSource(document);
      StreamResult result = new StreamResult(file.toFile());
      transformer.transform(source, result);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to save XML to file: " + file, e);
    }
  }

}
