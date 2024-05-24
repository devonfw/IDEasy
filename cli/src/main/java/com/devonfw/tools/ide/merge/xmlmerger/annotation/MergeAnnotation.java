package com.devonfw.tools.ide.merge.xmlmerger.annotation;

import com.devonfw.tools.ide.merge.xmlmerger.model.MergeStrategy;
import org.w3c.dom.Element;

public class MergeAnnotation {

  public static final String MERGE_NS_URI = "https://github.com/devonfw/IDEasy/merge";

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

  public static String getMergeId(Element element) {

    return element.getAttributeNS(MERGE_NS_URI, "id");
  }
}
