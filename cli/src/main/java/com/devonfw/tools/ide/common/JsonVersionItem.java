package com.devonfw.tools.ide.common;

/**
 * Interface for a data object that can be read from and written to JSON.
 */
public interface JsonVersionItem {

  /**
   * @return the {@link com.devonfw.tools.ide.version.VersionIdentifier version}.
   */
  String version();

}
