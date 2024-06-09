package com.devonfw.tools.ide.merge.xmlmerger.strategy;

import com.devonfw.tools.ide.merge.xmlmerger.matcher.ElementMatcher;

/**
 * Factory class for creating merge strategies.
 */
public class StrategyFactory {

  /**
   * Creates a merge strategy based on the specified merge strategy type.
   *
   * @param strategy the merge strategy type
   * @return the corresponding merge strategy
   * @throws IllegalArgumentException if the merge strategy type is unknown
   */
  public static Strategy createStrategy(MergeStrategy strategy, ElementMatcher matcher) {

    switch (strategy) {
      case COMBINE:
        return new CombineStrategy(matcher);
      case OVERRIDE:
        return new OverrideStrategy(matcher);
      case KEEP:
        return new KeepStrategy(matcher);
      default:
        throw new IllegalArgumentException("Unknown merge strategy: " + strategy);
    }
  }
}
