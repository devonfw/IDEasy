package com.devonfw.tools.ide.commandlet.check;

import java.nio.file.Path;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * A {@link CheckIssue} reporting that a {@code .gitignore} file exists but is missing expected rules. When fixed, appends the missing rule as a new line at the
 * end of the file.
 */
public class MissingGitignoreRuleIssue extends CheckIssue {

  private final String rule;

  /**
   * The constructor.
   *
   * @param gitignore the {@link Path} to the existing {@code .gitignore} file.
   * @param rule the missing rule (e.g. {@code ".*"} or {@code "!.gitignore"}).
   */
  public MissingGitignoreRuleIssue(Path gitignore, String rule) {
    super(gitignore, "Missing rule \"" + rule + "\" in " + gitignore.getFileName() + ".");
    this.rule = rule;
  }

  @Override
  public boolean isFixable() {

    return true;
  }

  @Override
  public boolean fix(IdeContext context) {

    List<String> lines = context.getFileAccess().readFileLines(getPath());

    if (lines == null) {
      return false;
    }

    lines.add(this.rule);
    context.getFileAccess().writeFileLines(lines, getPath());
    return true;
  }
}
