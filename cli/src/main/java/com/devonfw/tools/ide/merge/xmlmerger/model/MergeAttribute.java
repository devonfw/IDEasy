package com.devonfw.tools.ide.merge.xmlmerger.model;

import com.devonfw.tools.ide.merge.xmlmerger.XmlMerger;
import org.w3c.dom.Attr;

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

    return attr;
  }

  public String getName() {

    return attr.getName();
  }

  public String getValue() {

    return attr.getValue();
  }

  /**
   * Checks if the attribute is a merge namespace attribute.
   *
   * @return {@code true} if the attribute is a merge namespace attribute, otherwise {@code false}
   */
  public boolean isMergeNSAttr() {

    return XmlMerger.MERGE_NS_URI.equals(attr.getNamespaceURI()) || XmlMerger.MERGE_NS_URI.equals(attr.getValue());
  }

  /**
   * Checks if the attribute is a merge namespace id attribute.
   *
   * @return {@code true} if the attribute is a merge namespace id attribute, otherwise {@code false}
   */
  public boolean isMergeNsIdAttr() {

    return isMergeNSAttr() && attr.getLocalName().equals("id");
  }
}
