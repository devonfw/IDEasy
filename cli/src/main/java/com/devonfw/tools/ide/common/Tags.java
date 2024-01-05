package com.devonfw.tools.ide.common;

import java.util.Set;

/**
 * Interface for an object that {@link #getTags() has} {@link Tag}s.
 */
public interface Tags {

  /**
   * @return a {@link Set} with the tags classifying this object. E.g. for mvn (maven) the tags {@link Tag#JAVA java}
   *         and {@link Tag#BUILD build} could be associated.
   */
  Set<Tag> getTags();

}
