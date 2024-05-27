package com.devonfw.tools.ide.merge.xmlmerger.model;

import com.devonfw.tools.ide.merge.xmlmerger.annotation.MergeAnnotation;
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

  public boolean isMergeNsIdAttr() {

    return isMergeNSAttr() && attr.getName().equals("id");
  }
}
