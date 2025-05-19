package com.devonfw.tools.ide.merge.xml;

import java.nio.file.Path;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A wrapper for an XML {@link org.w3c.dom.Document}
 */
public class XmlMergeDocument {

  private final Document document;

  private final Path path;

  /**
   * The constructor.
   *
   * @param document the {@link #getDocument() document}.
   * @param path the {@link #getPath() path}.
   */
  public XmlMergeDocument(Document document, Path path) {

    super();
    this.document = document;
    this.path = path;
  }

  /**
   * @return the XML {@link Document}.
   */
  public Document getDocument() {

    return this.document;
  }

  /**
   * @return the {@link Path} to the file from which the {@link #getDocument() document} was loaded.
   */
  public Path getPath() {

    return this.path;
  }

  /**
   * @return the {@link Document#getDocumentElement() root} {@link Element} of the {@link #getDocument() document}.
   */
  public Element getRoot() {

    return this.document.getDocumentElement();
  }

  /**
   * @return a {@link NodeList} with all {@link Element}s of this {@link Document}.
   */
  public NodeList getAllElements() {

    return this.document.getElementsByTagName("*");
  }

  @Override
  public String toString() {

    return this.path.toString();
  }
}
