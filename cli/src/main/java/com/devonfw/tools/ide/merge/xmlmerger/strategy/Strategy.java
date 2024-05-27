package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Document;

public interface Strategy {
  void merge(MergeElement updateElement, Document targetDocument);

}
