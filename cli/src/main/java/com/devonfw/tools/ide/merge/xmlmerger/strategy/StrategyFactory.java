package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;

/**
 * Factory class for creating merge strategies.
 */
public class StrategyFactory {

  /**
   * The context.
   */
  private final IdeContext context;

  /**
   * The element matcher.
   */
  private final ElementMatcher elementMatcher;

  /**
   * @param context the IDE context
   * @param elementMatcher the element matcher used for matching elements
   */
  public StrategyFactory(IdeContext context, ElementMatcher elementMatcher) {

    this.context = context;
    this.elementMatcher = elementMatcher;
  }

  /**
   * Creates a merge strategy based on the specified merge strategy type.
   *
   * @param strategy the merge strategy type
   * @return the corresponding merge strategy
   * @throws IllegalArgumentException if the merge strategy type is unknown
   */
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
