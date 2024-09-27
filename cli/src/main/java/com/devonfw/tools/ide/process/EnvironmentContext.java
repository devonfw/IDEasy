package com.devonfw.tools.ide.process;

import java.nio.file.Path;

/**
 * Interface for the environment context relatable in the case a tool needs to be run with an environment variable.
 */
public interface EnvironmentContext {

  /**
   * Sets or overrides the specified environment variable only in this context. Please note that the environment variables are initialized when the
   * {@link EnvironmentContext} is created. This method explicitly set an additional or overrides an existing environment and will have effect only within this
   * context and not change the {@link com.devonfw.tools.ide.environment.EnvironmentVariables} or {@link com.devonfw.tools.ide.common.SystemPath}.
   *
   * @param key the name of the environment variable (E.g. "PATH").
   * @param value the value of the environment variable.
   * @return this {@link EnvironmentContext} for fluent API calls.
   */
  EnvironmentContext withEnvVar(String key, String value);

  /**
   * Extends the "PATH" variable with the given {@link Path} entry. The new entry will be added to the beginning of the "PATH" and potentially override other
   * entries if it contains the same binary.
   *
   * @param path the {@link Path} pointing to the folder with the binaries to add to the "PATH" variable.
   * @return this {@link EnvironmentContext} for fluent API calls.
   */
  EnvironmentContext withPathEntry(Path path);

  /**
   * @return an empty instance of {@link EnvironmentContext} to prevent {@link NullPointerException}s.
   */
  static EnvironmentContext getEmpty() {

    return EnvironmentContextEmpty.INSTANCE;
  }

}
