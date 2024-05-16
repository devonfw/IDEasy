package com.devonfw.tools.ide.merge.xmlMerger;

import org.w3c.dom.Attr;

public class MergeAttribute {

  private final Attr attribute;

  public MergeAttribute(Attr attribute) {
    this.attribute = attribute;
  }

  public Attr getAttribute() {
    return attribute;
  }
}