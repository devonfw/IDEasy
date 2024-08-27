package com.devonfw.tools.ide.process;

/**
 * Interface for the environment context relatable in the case a tool needs to be run with an environment variable.
 */
public interface EnvironmentContext {

  /**
   * Sets or overrides the specified environment variable only for the planned process execution. Please note that the environment variables are initialized
   * when the {@link EnvironmentContext} is created. This method explicitly set an additional or overrides an existing environment and will have effect for each
   * process execution invoked from this {@link EnvironmentContext} instance.
   *
   * @param key the name of the environment variable (E.g. "PATH").
   * @param value the value of the environment variable.
   * @return this {@link EnvironmentContext} for fluent API calls.
   */
  EnvironmentContext withEnvVar(String key, String value);

}
