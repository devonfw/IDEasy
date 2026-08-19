package com.devonfw.tools.ide.commandlet.check;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;

public class MissingGitignoreRuleIssue extends CheckIssue {

  private static final Logger LOG = LoggerFactory.getLogger(MissingGitignoreRuleIssue.class);
  private final String rule;

  public MissingGitignoreRuleIssue(Path gitignore, String rule) {
    super(gitignore, "Missing rule \"" + rule + "\" in " + gitignore.getFileName()+ ".");
    this.rule = rule;
  }

  @Override
  public boolean isFixable() {

    return true;
  }

  @Override
  public boolean fix(IdeContext context) {

    List<String> lines = new ArrayList<>(context.getFileAccess().readFileLines(getPath()));
    lines.add(this.rule);
    context.getFileAccess().writeFileLines(lines, getPath());
    return true;
  }
}
