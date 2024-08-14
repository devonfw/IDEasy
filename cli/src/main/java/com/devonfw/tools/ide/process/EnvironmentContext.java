package com.devonfw.tools.ide.process;

/**
 * Interface for the environment context relatable in the case a tool needs to be run with an environment variable.
 */
public interface EnvironmentContext {

  EnvironmentContext withEnvVar(String key, String value);

}
