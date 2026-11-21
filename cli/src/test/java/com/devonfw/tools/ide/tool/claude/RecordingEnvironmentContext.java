package com.devonfw.tools.ide.tool.claude;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.VariableScope;

/**
 * Test double for {@link EnvironmentContext} that records every variable set or removed.
 */
public class RecordingEnvironmentContext implements EnvironmentContext {

  /** Variables set via {@link #withEnvVar(String, String)}. */
  public final Map<String, String> set = new HashMap<>();
  /** Scopes used for variables set via withEnvVar. */
  public final Map<String, VariableScope> scopes = new HashMap<>();
  /** Variables removed via {@link #removeEnvVar(String)}. */
  public final Set<String> removed = new HashSet<>();

  /** Path entries added via {@link #withPathEntry(Path)}. */
  public final List<Path> pathEntries = new ArrayList<>();

  @Override
  public EnvironmentContext withEnvVar(String key, String value) {
    this.set.put(key, value);
    return this;
  }

  @Override
  public EnvironmentContext withEnvVar(String key, String value, VariableScope scope) {
    this.set.put(key, value);
    this.scopes.put(key, scope);
    return this;
  }

  @Override
  public EnvironmentContext withPathEntry(Path path) {
    this.pathEntries.add(path);
    return this;
  }

  @Override
  public EnvironmentContext removeEnvVar(String key) {
    this.removed.add(key);
    return this;
  }
}
