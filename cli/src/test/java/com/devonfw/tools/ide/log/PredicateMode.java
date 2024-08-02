package com.devonfw.tools.ide.log;

/**
 * {@link Enum} with available modes for asserting a {@link java.util.function.Predicate} on a {@link java.util.Collection}.
 */
public enum PredicateMode {

  /** All {@link java.util.Collection#add(Object) entries} must {@link java.util.function.Predicate#test(Object) evalute} as {@code true}. */
  MATCH_ALL,

  /**
   * At least one {@link java.util.Collection#add(Object) entries} must {@link java.util.function.Predicate#test(Object) evalute} as {@code true}. No further
   * evaluation after first success.
   */
  MATCH_ONE;

}
