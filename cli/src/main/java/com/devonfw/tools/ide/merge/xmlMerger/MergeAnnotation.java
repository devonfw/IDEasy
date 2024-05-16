package com.devonfw.tools.ide.merge.xmlMerger;

import org.w3c.dom.Element;

public class MergeAnnotation {

  private static final String MERGE_NS_URI = "https://github.com/devonfw/IDEasy/merge";

  public static MergeStrategy getMergeStrategy(Element element) {

    String strategy = element.getAttributeNS(MERGE_NS_URI, "strategy");
    if ("combine".equals(strategy)) {
      return MergeStrategy.COMBINE;
    } else if ("override".equals(strategy)) {
      return MergeStrategy.OVERRIDE;
    } else if ("keep".equals(strategy)) {
      return MergeStrategy.KEEP;
    }
    return null;
  }

  public static String getIdExpression(Element element) {

    return element.getAttributeNS(MERGE_NS_URI, "id");
  }

  public static String getChildrenIdExpression(Element element) {

    return element.getAttributeNS(MERGE_NS_URI, "childrenId");
  }
}
