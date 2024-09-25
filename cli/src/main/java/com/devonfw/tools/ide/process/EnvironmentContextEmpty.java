package com.devonfw.tools.ide.process;

import java.nio.file.Path;

/**
 * Empty implementation of {@link EnvironmentContext} used to prevent {@link NullPointerException}s.
 */
class EnvironmentContextEmpty implements EnvironmentContext {

  static final EnvironmentContextEmpty INSTANCE = new EnvironmentContextEmpty();

  private EnvironmentContextEmpty() {
    super();
  }

  @Override
  public EnvironmentContext withEnvVar(String key, String value) {

    return this;
  }

  @Override
  public EnvironmentContext withPathEntry(Path path) {

    return this;
  }
}
