package com.devonfw.tools.ide.merge.xmlmerger.strategy;

/**
 * Enum of merge strategies for XML elements.
 */
public enum MergeStrategy {

  /**
   * Combines source and target elements. Overrides text nodes and attributes. This process is recursively applied to child elements. If the source element
   * exists in the target document, they are combined, otherwise, the source element is appended.
   */
  COMBINE,

  /**
   * Replaces the target element with the source element, without considering child elements. If the element exists in the target, it is overridden, otherwise,
   * it is appended.
   */
  OVERRIDE,

  /**
   * Keeps the existing target element intact if the source element exists in the target document, otherwise, it is appended.
   */
  KEEP,
}
