package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;

/**
 * Merge strategy that keeps existing element without any changes.
 */
public class KeepStrategy extends AbstractStrategy {

  /**
   * @param context the IDE context
   * @param elementMatcher the element matcher used for matching elements
   */
  public KeepStrategy(IdeContext context, ElementMatcher elementMatcher) {

    super(context, elementMatcher);
  }

  @Override
  protected void mergeElement(MergeElement sourceElement, MergeElement targetElement) {
    // Do nothing, keep the existing element
  }
}
