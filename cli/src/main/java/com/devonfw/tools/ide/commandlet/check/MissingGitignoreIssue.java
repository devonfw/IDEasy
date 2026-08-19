package com.devonfw.tools.ide.commandlet.check;

import java.nio.file.Path;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;

public class MissingGitignoreIssue extends CheckIssue {

  public static final String GITIGNORE = ".gitignore";

  public MissingGitignoreIssue(Path repositoryRoot) {
    super(repositoryRoot.resolve(GITIGNORE), "No " + GITIGNORE + " found in repository root.");
  }

  @Override
  public boolean isFixable() {
    return true;
  }

  @Override
  public boolean fix(IdeContext context) {

    context.getFileAccess().writeFileLines(List.of(".*", "!" + GITIGNORE), getPath());
    return true;
  }
}
