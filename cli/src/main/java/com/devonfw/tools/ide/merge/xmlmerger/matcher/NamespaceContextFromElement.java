package com.devonfw.tools.ide.merge.xmlmerger.matcher;

import java.util.Collections;
import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;

import org.w3c.dom.Element;

class NamespaceContextFromElement implements NamespaceContext {

  private final Element element;

  NamespaceContextFromElement(Element element) {
    super();
    this.element = element;
  }

  @Override
  public String getNamespaceURI(String prefix) {
    return this.element.lookupNamespaceURI(prefix);
  }

  @Override
  public String getPrefix(String namespaceURI) {

    return this.element.lookupPrefix(namespaceURI);
  }

  @Override
  public Iterator<String> getPrefixes(String namespaceURI) {
    return Collections.singletonList(getPrefix(namespaceURI)).iterator();
  }
}
