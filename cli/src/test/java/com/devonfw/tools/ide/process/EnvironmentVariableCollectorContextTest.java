package com.devonfw.tools.ide.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.environment.VariableSource;

class EnvironmentVariableCollectorContextTest {

  @Test
  void localEnvVarShouldNotBeExported() {

    Map<String, VariableLine> variables = new HashMap<>();
    EnvironmentVariableCollectorContext context = new EnvironmentVariableCollectorContext(variables,
        new VariableSource(EnvironmentVariablesType.TOOL, null), null);

    context.withEnvVar("TEST_VAR", "test-value", VariableScope.LOCAL);

    assertThat(variables.get("TEST_VAR").isExport()).isFalse();
  }

  @Test
  void exportEnvVarShouldBeExported() {

    Map<String, VariableLine> variables = new HashMap<>();
    EnvironmentVariableCollectorContext context = new EnvironmentVariableCollectorContext(variables,
        new VariableSource(EnvironmentVariablesType.TOOL, null), null);

    context.withEnvVar("TEST_VAR", "test-value", VariableScope.EXPORT);

    assertThat(variables.get("TEST_VAR").isExport()).isTrue();
  }
}
