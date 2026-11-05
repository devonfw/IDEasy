package com.devonfw.tools.ide.commandlet.check;

import java.nio.file.Path;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * A {@link CheckIssue} reporting that the repository root has no {@code .gitignore} file. When fixed, creates
 * a minimal {@code .gitignore} containing the rules ({@code .*} and {@code !.gitignore}).
 */
public class MissingGitignoreIssue extends CheckIssue {

  /** The file name of the git-ignore file. */
  public static final String GITIGNORE = ".gitignore";

  /**
   * The constructor.
   *
   * @param repositoryRoot the {@link Path} to the (missing) {@code .gitignore} file in the repository root.
   */
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
