package com.devonfw.tools.ide.commandlet.check;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.property.FlagProperty;

/**
 * {@link Commandlet} to check the current repository for best-practices, starting from the current working directory (CWD).
 */
public class CheckCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(CheckCommandlet.class);

  /**
   * rule to exclude all hidden files and folders.
   */
  public static final String RULE_IGNORE_HIDDEN = ".*";

  /**
   * rule to ensure that the .gitignore file is committed.
   */
  public static final String RULE_UNIGNORE_GITIGNORE = "!" + MissingGitignoreIssue.GITIGNORE;

  /**
   * Flag to fix issues
   */
  public final FlagProperty fix;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CheckCommandlet(IdeContext context) {
    super(context);
    addKeyword(getName());
    this.fix = add(new FlagProperty("--fix"));

  }

  @Override
  public String getName() {

    return "check";
  }

  @Override
  public boolean isIdeRootRequired() {
    return false;
  }

  @Override
  protected void doRun() {

    List<CheckIssue> issues = new ArrayList<>();

    Path repositoryRoot = findRepositoryRoot();
    if (repositoryRoot == null) {
      LOG.warn("This does not seem to be a git repository (no .git folder found in {} or any parent directory).", this.context.getCwd());
      return;
    }

    LOG.info("Found repository root at {}.", repositoryRoot);
    checkGitIgnore(repositoryRoot, issues);
    report(issues);
  }

  private Path findRepositoryRoot() {
    GitContext gitContext = this.context.getGitContext();
    return gitContext.findRepositoryRoot(this.context.getCwd());
  }

  private void checkGitIgnore(Path repositoryRoot, List<CheckIssue> issues) {

    Path gitignore = repositoryRoot.resolve(MissingGitignoreIssue.GITIGNORE);
    if (!context.getFileAccess().isFile(gitignore)) {
      issues.add(new MissingGitignoreIssue(repositoryRoot));
      return;
    }

    List<String> lines;

    lines = context.getFileAccess().readFileLines(gitignore);

    if (!containsRule(lines, RULE_IGNORE_HIDDEN)) {
      issues.add(new MissingGitignoreRuleIssue(gitignore, RULE_IGNORE_HIDDEN));
    }

    if (!containsRule(lines, RULE_UNIGNORE_GITIGNORE)) {
      issues.add(new MissingGitignoreRuleIssue(gitignore, RULE_UNIGNORE_GITIGNORE));
    }
  }

  private boolean containsRule(List<String> lines, String rule) {

    return lines.stream().anyMatch(line -> line.strip().equals(rule));
  }

  private void report(List<CheckIssue> issues) {

    if (issues.isEmpty()) {
      LOG.info("No issues found.");
      return;
    }

    boolean open = false;
    for (CheckIssue issue : issues) {
      if (this.fix.isTrue() && issue.isFixable()) {
        if (issue.fix(this.context)) {
          LOG.info("Fixed: {}", issue);
          continue;
        }
        LOG.warn("Failed to fix {}", issue);
      } else {
        LOG.warn("{}", issue);
        open = true;
      }
    }

    if (open) {
      throw new CliException("ide check found issues - see issues above.", 1);
    }
  }
}
