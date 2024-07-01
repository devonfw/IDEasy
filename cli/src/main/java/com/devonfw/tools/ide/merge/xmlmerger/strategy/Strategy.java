package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Document;

/**
 * Strategy interface for defining merge strategies.
 */
public interface Strategy {

  /**
   * Merges the given source element with the target element.
   *
   * @param sourceElement the element to be merged
   * @param targetElement the element to be merged with
   */
  void merge(MergeElement sourceElement, MergeElement targetElement);
}
