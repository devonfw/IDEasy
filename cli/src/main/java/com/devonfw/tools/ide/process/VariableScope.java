package com.devonfw.tools.ide.process;

/**
 * Scope of an environment variable.
 */
public enum VariableScope {

  /** Variable is local to the environment and is not inherited by child processes. */
  LOCAL,

  /** Variable is exported and inherited by child processes. */
  EXPORT
}
