package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Document;

/**
 * Strategy interface for defining merge strategies.
 */
public interface Strategy {

  /**
   * Merges the given update element into the target document.
   *
   * @param updateElement the element to be merged
   * @param targetDocument the target document where the element will be merged
   */
  void merge(MergeElement updateElement, Document targetDocument);
}
