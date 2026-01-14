package com.devonfw.tools.ide.common;

/**
 * Interface for a builder following the builder-pattern.
 *
 * @param <T> type of the object to build.
 */
public interface Builder<T> {

  /**
   * @return the build object.
   */
  T build();

}
