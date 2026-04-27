package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import java.util.List;

public class MvnArgProperty extends StringProperty {
  private static final List<String> MAVEN_GOALS = List.of(
      "clean",
      "compile",
      "dependency:list",
      "dependency:tree",
      "deploy",
      "exec:java",
      "generate-resources",
      "generate-sources",
      "help:effective-settings",
      "install",
      "integration-test",
      "package",
      "post-clean",
      "post-integration-test",
      "prepare-package",
      "pre-clean",
      "pre-integration-test",
      "process-resources",
      "process-sources",
      "site",
      "site-deploy",
      "test",
      "test-compile",
      "validate",
      "verify"
    );

  public MvnArgProperty(String name, String alias) {
    super(name, true, alias);
  }

  @Override
  protected void completeValue(String arg, IdeContext context, Commandlet commandlet,
                               CompletionCandidateCollector collector) {
    for (String goal : MAVEN_GOALS) {
      if (goal.startsWith(arg)) {
        collector.add(goal, null, this, commandlet);
      }
    }
  }
}
