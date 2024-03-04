package com.devonfw.tools.ide.tool;

/**
 * User interaction answers when a security risk was found and the user can f.e. choose to stay on the current unsafe
 * version, use the latest safe version, use the latest version or use the next safe version.
 */
public enum SecurityRiskInteractionAnswer {

  /**
   * User answer to stay on the current unsafe version.
   */
  STAY,

  /**
   * User answer to install the latest of all safe versions.
   */
  LATEST_SAFE,

  /**
   * User answer to use the latest safe version.
   */
  SAFE_LATEST,

  /**
   * User answer to use the next safe version.
   */
  NEXT_SAFE,

}
