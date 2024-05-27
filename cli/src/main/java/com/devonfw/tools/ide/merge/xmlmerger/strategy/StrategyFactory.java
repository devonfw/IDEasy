package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;

public class StrategyFactory {

  private final IdeContext context;
  private final ElementMatcher elementMatcher;

  public StrategyFactory(IdeContext context, ElementMatcher elementMatcher) {

    this.context = context;
    this.elementMatcher = elementMatcher;
  }

  public Strategy createStrategy(MergeStrategy strategy) {

    switch (strategy) {
      case COMBINE:
        return new CombineStrategy(context, elementMatcher);
      case OVERRIDE:
        return new OverrideStrategy(context, elementMatcher);
      case KEEP:
        return new KeepStrategy(context, elementMatcher);
      default:
        throw new IllegalArgumentException("Unknown merge strategy: " + strategy);
    }
  }
}
