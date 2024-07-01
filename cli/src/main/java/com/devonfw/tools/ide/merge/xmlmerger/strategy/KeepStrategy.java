package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;

/**
 * Merge strategy that keeps existing element without any changes.
 */
public class KeepStrategy extends AbstractStrategy {

  /**
   * @param elementMatcher the element matcher used for matching elements
   */
  public KeepStrategy(ElementMatcher elementMatcher) {

    super(elementMatcher);
  }

  @Override
  public void merge(MergeElement sourceElement, MergeElement targetElement) {

    // Do nothing, keep the existing element
  }
}
