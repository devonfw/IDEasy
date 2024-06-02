package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;
import com.devonfw.tools.ide.merge.xmlmerger.model.MergeElement;
import org.w3c.dom.Document;

public class KeepStrategy extends AbstractStrategy {

  public KeepStrategy(IdeContext context, ElementMatcher elementMatcher) {
    super(context, elementMatcher);
  }

  @Override
  protected void mergeElement(MergeElement updateElement, MergeElement targetElement) {
    // do nothing, keep the existing element
  }
}
