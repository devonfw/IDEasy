package com.devonfw.tools.ide.merge.xmlMerger;

import org.w3c.dom.Attr;

public class MergeAttribute {

  private final Attr attr;

  public MergeAttribute(Attr attr) {
    this.attr = attr;
  }

  public Attr getAttr() {
    return attr;
  }

  public String getName() {
    return attr.getName();
  }

  public String getValue() {
    return attr.getValue();
  }

  public boolean isMergeNSAttr() {

    return MergeAnnotation.MERGE_NS_URI.equals(attr.getNamespaceURI()) || MergeAnnotation.MERGE_NS_URI.equals(attr.getValue());
  }
}
