package com.devonfw.tools.ide.tool.claude;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.devonfw.tools.ide.process.EnvironmentContext;

/**
 * Test double for {@link EnvironmentContext} that records every variable set or removed.
 */
public class RecordingEnvironmentContext implements EnvironmentContext {

  /** Variables set via {@link #withEnvVar(String, String)}. */
  public final Map<String, String> set = new HashMap<>();

  /** Variables removed via {@link #removeEnvVar(String)}. */
  public final Set<String> removed = new HashSet<>();

  @Override
  public EnvironmentContext withEnvVar(String key, String value) {
    this.set.put(key, value);
    return this;
  }

  @Override
  public EnvironmentContext withPathEntry(Path path) {
    return this;
  }

  @Override
  public EnvironmentContext removeEnvVar(String key) {
    this.removed.add(key);
    return this;
  }
}
