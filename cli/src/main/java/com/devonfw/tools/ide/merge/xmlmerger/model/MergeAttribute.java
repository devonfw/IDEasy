package com.devonfw.tools.ide.merge.xmlmerger.model;

import org.w3c.dom.Attr;

import com.devonfw.tools.ide.merge.xmlmerger.XmlMerger;

/**
 * Represents an attribute of a {@link MergeElement} during the merging process.
 */
public class MergeAttribute {

  /**
   * The attribute represented by this MergeAttribute.
   */
  private final Attr attr;

  public MergeAttribute(Attr attr) {

    this.attr = attr;
  }

  public Attr getAttr() {

    return this.attr;
  }

  public String getName() {

    return this.attr.getName();
  }

  public String getValue() {

    return this.attr.getValue();
  }

  /**
   * Checks if the attribute is a merge namespace attribute.
   *
   * @return {@code true} if the attribute is a merge namespace attribute, otherwise {@code false}
   */
  public boolean isMergeNsAttr() {

    return XmlMerger.MERGE_NS_URI.equals(this.attr.getNamespaceURI()) || XmlMerger.MERGE_NS_URI.equals(this.attr.getValue());
  }

  /**
   * Checks if the attribute is a merge namespace id attribute.
   *
   * @return {@code true} if the attribute is a merge namespace id attribute, otherwise {@code false}
   */
  public boolean isMergeNsIdAttr() {

    return isMergeNsAttr() && this.attr.getLocalName().equals("id");
  }
}
