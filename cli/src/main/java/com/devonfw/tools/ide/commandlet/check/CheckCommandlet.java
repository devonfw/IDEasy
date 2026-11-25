package com.devonfw.tools.ide.commandlet.check;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.AbstractCommandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.FlagProperty;

/**
 * {@link AbstractCommandlet} to check the repository for best-practices. By default this starts from the current working directory (CWD) and checks the single
 * repository it belongs to. With the {@code --all} flag all repositories found in all workspaces are checked instead.
 */
public class CheckCommandlet extends AbstractCommandlet {

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
   * Flag to check all repositories found in all workspaces instead of only the repository of the current working directory.
   */
  public final FlagProperty all;

  /**
   * Checks the repository for issues.
   */
  private void checkRepository(Path path, List<CheckIssue> issues) {
    checkGitIgnore(path, issues);
  }

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CheckCommandlet(IdeContext context) {
    super(context);
    addKeyword(getName());
    this.fix = add(new FlagProperty("--fix"));
    this.all = add(new FlagProperty("--all"));
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
    if (this.all.isTrue()) {
      List<Path> roots = findRepositoryRootsAll();
      LOG.info("Found {} repository root(s) to check.", roots.size());
      for (Path root : roots) {
        checkRepository(root, issues);
      }
    } else {
      Path repositoryRoot = findRepositoryRoot();
      if (repositoryRoot == null) {
        LOG.warn("This does not seem to be a git repository (no .git folder found in {} or any parent directory).", this.context.getCwd());
        return;
      }
      LOG.info("Found repository root at {}.", repositoryRoot);
      checkRepository(repositoryRoot, issues);
    }

    report(issues);
  }

  private Path findRepositoryRoot() {
    GitContext gitContext = this.context.getGitContext();
    return gitContext.findRepositoryRoot(this.context.getCwd());
  }

  /**
   * @return all repository roots found in the workspaces and below the current working directory
   */
  private List<Path> findRepositoryRootsAll() {

    Set<Path> baseDirs = new LinkedHashSet<>();
    Path workspacesBasePath = this.context.getWorkspacesBasePath();
    if (workspacesBasePath != null) {
      baseDirs.add(workspacesBasePath);
    } else {
      baseDirs.add(this.context.getCwd());
    }

    Set<Path> seen = new LinkedHashSet<>();
    List<Path> roots = new ArrayList<>();
    for (Path baseDir : baseDirs) {
      for (Path root : walkRoots(baseDir)) {
        if (seen.add(this.context.getFileAccess().toRealPath(root))) {
          roots.add(root);
        }
      }
    }
    return roots;
  }

  /**
   * @param start the directory to scan.
   * @return the repository roots found.
   */
  private List<Path> walkRoots(Path start) {

    if (start == null) {
      return List.of();
    }
    List<Path> roots = new ArrayList<>();
    try {
      Files.walkFileTree(start, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {

          if (dir.getFileName().toString().startsWith(".")) {
            return FileVisitResult.SKIP_SUBTREE;
          }
          if (isRepositoryRoot(dir)) {
            roots.add(dir);
            return FileVisitResult.SKIP_SUBTREE;
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to scan for repositories in " + start, e);
    }
    return roots;
  }

  /**
   * @param dir the directory to check.
   * @return {@code true} if the directory is a repository root.
   */
  private boolean isRepositoryRoot(Path dir) {

    return this.context.getGitContext().isGitRepo(dir);
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
      IdeLogLevel.SUCCESS.log(LOG, "No issues found.");
      return;
    }

    LOG.warn("Found {} issue(s)", issues.size());

    boolean open = false;
    for (CheckIssue issue : issues) {
      if (this.fix.isTrue() && issue.isFixable()) {
        if (issue.fix(this.context)) {
          IdeLogLevel.SUCCESS.log(LOG, "Fixed: {}", issue);
          continue;
        }
        LOG.warn("Failed to fix {}", issue);
        open = true;
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
