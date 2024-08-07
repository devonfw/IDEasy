package com.devonfw.tools.ide.process;

public interface EnvironmentContext {

  EnvironmentContext withEnvVar(String key, String value);

}
